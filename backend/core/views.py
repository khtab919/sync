import hashlib
import json
import secrets
import uuid
from datetime import date, timedelta
from functools import wraps
from django.contrib.auth import authenticate, get_user_model
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError
from django.db import IntegrityError, connection, transaction
from django.db.models import Q
from django.http import JsonResponse
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from .models import Profile, Service, Booking, Wallet, Withdrawal, Message, Reel, Report, SessionToken, AuthRateLimit

User = get_user_model()

class ApiError(Exception):
    def __init__(self, message, status=400):
        self.message, self.status = message, status

def endpoint(methods, authenticated=True):
    def decorate(fn):
        @csrf_exempt
        @wraps(fn)
        def wrapped(request, *args, **kwargs):
            if request.method not in methods:
                response = JsonResponse({'error': 'method_not_allowed'}, status=405)
                response['Allow'] = ', '.join(methods)
                return response
            try:
                if authenticated:
                    header = request.headers.get('Authorization', '')
                    if not header.startswith('Bearer ') or len(header) > 256:
                        raise ApiError('authentication_required', 401)
                    digest = hashlib.sha256(header[7:].encode()).hexdigest()
                    token = SessionToken.objects.select_related('user').filter(digest=digest, expires_at__gt=timezone.now(), user__is_active=True).first()
                    if not token:
                        raise ApiError('invalid_session', 401)
                    request.actor = token.user
                    request.api_token = token
                if not authenticated and request.method == 'POST':
                    limited = False
                    ip = request.headers.get('X-Real-IP', request.META.get('REMOTE_ADDR', 'unknown'))
                    key = hashlib.sha256((ip + request.path).encode()).hexdigest()
                    now = timezone.now()
                    with transaction.atomic():
                        AuthRateLimit.objects.get_or_create(key=key, defaults={'window_start': now})
                        limiter = AuthRateLimit.objects.select_for_update().get(key=key)
                        if now - limiter.window_start >= timedelta(minutes=15):
                            limiter.window_start, limiter.attempts = now, 0
                        limiter.attempts += 1
                        limiter.save()
                        limited = limiter.attempts > 20
                    if limited:
                        raise ApiError('too_many_attempts', 429)
                request.payload = {}
                if request.method in ('POST', 'PATCH'):
                    if request.content_type != 'application/json':
                        raise ApiError('json_required', 415)
                    request.payload = json.loads(request.body)
                    if not isinstance(request.payload, dict):
                        raise ApiError('json_object_required')
                return fn(request, *args, **kwargs)
            except ApiError as exc:
                return JsonResponse({'error': exc.message}, status=exc.status)
            except (ValueError, TypeError, KeyError, ValidationError):
                return JsonResponse({'error': 'invalid_input'}, status=400)
            except IntegrityError:
                return JsonResponse({'error': 'conflict'}, status=409)
        return wrapped
    return decorate

def text(data, key, minimum=1, maximum=300):
    value = data.get(key)
    if not isinstance(value, str) or not minimum <= len(value.strip()) <= maximum:
        raise ApiError('invalid_' + key)
    return value.strip()

def amount(data, key):
    value = data.get(key)
    if type(value) is not int or not 1 <= value <= 1000000:
        raise ApiError('invalid_' + key)
    return value

def password_value(data, minimum=1):
    value = data.get('password')
    if not isinstance(value, str) or not minimum <= len(value) <= 128:
        raise ApiError('invalid_password')
    return value

def issue_token(user):
    raw = secrets.token_urlsafe(48)
    SessionToken.objects.create(user=user, digest=hashlib.sha256(raw.encode()).hexdigest(), expires_at=timezone.now() + timedelta(days=7))
    return raw

@endpoint(['GET'], authenticated=False)
def health(request):
    with connection.cursor() as cursor:
        cursor.execute('SELECT 1')
    return JsonResponse({'status': 'ok'})

@endpoint(['POST'], authenticated=False)
def register(request):
    p = request.payload
    username = text(p, 'username', 3, 50)
    if not all(c.isalnum() or c in '._-' for c in username):
        raise ApiError('invalid_username')
    email = text(p, 'email', 3, 150)
    from django.core.validators import validate_email
    validate_email(email)
    password = password_value(p, 12)
    born = date.fromisoformat(text(p, 'date_of_birth', 10, 10))
    today = timezone.localdate()
    age = today.year - born.year - ((today.month, today.day) < (born.month, born.day))
    if born > today or not 18 <= age <= 120 or p.get('accept_non_explicit_policy') is not True:
        raise ApiError('adult_policy_required')
    user = User(username=username, email=email)
    validate_password(password, user=user)
    with transaction.atomic():
        # Case-insensitive handles must be unique even during concurrent requests.
        if User.objects.filter(username__iexact=username).exists():
            raise ApiError('username_unavailable', 409)
        user.username = username.lower()
        user.set_password(password)
        user.save()
        Profile.objects.create(user=user, date_of_birth=born)
        Wallet.objects.create(user=user)
        token = issue_token(user)
    return JsonResponse({'token': token, 'user_id': user.pk}, status=201)

@endpoint(['POST'], authenticated=False)
def login(request):
    p = request.payload
    user = authenticate(username=text(p, 'username', 3, 50).lower(), password=password_value(p))
    if user is None or not Profile.objects.filter(user=user).exists():
        raise ApiError('invalid_credentials', 401)
    return JsonResponse({'token': issue_token(user), 'user_id': user.pk})

@endpoint(['POST'])
def logout(request):
    request.api_token.delete()
    return JsonResponse({'ok': True})

@endpoint(['GET', 'PATCH'])
def me(request):
    profile = Profile.objects.get(user=request.actor)
    if request.method == 'PATCH':
        if set(request.payload) - {'bio'}:
            raise ApiError('unsupported_profile_field')
        profile.bio = text(request.payload, 'bio', 0, 300)
        profile.save(update_fields=['bio'])
    return JsonResponse({'id': request.actor.pk, 'username': request.actor.username, 'bio': profile.bio, 'creator': profile.creator, 'identity_verified': profile.identity_verified})

@endpoint(['GET', 'POST'])
def services(request):
    if request.method == 'GET':
        rows = Service.objects.filter(approved=True, active=True, owner__is_active=True).order_by('-id').values('id', 'owner_id', 'title', 'description', 'price_cents')[:100]
        return JsonResponse({'items': list(rows)})
    if not Profile.objects.filter(user=request.actor, creator=True, identity_verified=True).exists():
        raise ApiError('verified_creator_required', 403)
    p = request.payload
    service = Service.objects.create(owner=request.actor, title=text(p, 'title', 3, 150), description=text(p, 'description', 10, 600), price_cents=amount(p, 'price_cents'))
    return JsonResponse({'id': service.pk, 'status': 'pending_review'}, status=201)

@endpoint(['GET', 'POST'])
def bookings(request):
    if request.method == 'GET':
        rows = Booking.objects.filter(buyer=request.actor).order_by('-created_at').values('id', 'service_id', 'price_cents', 'status', 'created_at')[:100]
        return JsonResponse({'items': list(rows)})
    service = Service.objects.filter(pk=amount(request.payload, 'service_id'), approved=True, active=True, owner__is_active=True).first()
    if not service or service.owner_id == request.actor.pk:
        raise ApiError('service_unavailable', 404)
    booking = Booking.objects.create(buyer=request.actor, service=service, price_cents=service.price_cents)
    return JsonResponse({'id': booking.pk, 'status': booking.status, 'price_cents': booking.price_cents}, status=201)

@endpoint(['GET', 'POST'])
def messages(request, peer_id):
    peer = User.objects.filter(pk=peer_id, is_active=True).first()
    if not peer or peer == request.actor:
        raise ApiError('recipient_unavailable', 404)
    if request.method == 'GET':
        rows = Message.objects.filter(Q(sender=request.actor, recipient=peer) | Q(sender=peer, recipient=request.actor)).order_by('-id').values('id', 'sender_id', 'recipient_id', 'body', 'created_at')[:100]
        return JsonResponse({'items': list(rows)})
    profile = Profile.objects.filter(user=peer).first()
    if not profile or profile.chat_price_cents:
        raise ApiError('paid_chat_requires_verified_payment', 409)
    message = Message.objects.create(sender=request.actor, recipient=peer, body=text(request.payload, 'body', 1, 2000))
    return JsonResponse({'id': message.pk}, status=201)

@endpoint(['GET'])
def reels(request):
    if not Profile.objects.filter(user=request.actor, identity_verified=True).exists():
        raise ApiError('identity_verification_required', 403)
    return JsonResponse({'items': list(Reel.objects.filter(approved=True, owner__is_active=True).order_by('-id').values('id', 'owner_id', 'video_url')[:100])})

@endpoint(['GET'])
def wallet(request):
    row = Wallet.objects.get(user=request.actor)
    return JsonResponse({'currency': 'USD', 'balance_cents': row.balance_cents, 'reserved_cents': row.reserved_cents, 'available_cents': row.balance_cents - row.reserved_cents})

@endpoint(['GET', 'POST'])
def withdrawals(request):
    if request.method == 'GET':
        return JsonResponse({'items': list(Withdrawal.objects.filter(user=request.actor).order_by('-created_at').values('id', 'amount_cents', 'status', 'created_at')[:100])})
    if not Profile.objects.filter(user=request.actor, creator=True, identity_verified=True).exists():
        raise ApiError('verified_creator_required', 403)
    p = request.payload
    key = uuid.UUID(text(p, 'request_key', 36, 36))
    cents = amount(p, 'amount_cents')
    destination = text(p, 'destination', 5, 300)
    with transaction.atomic():
        row = Wallet.objects.select_for_update().get(user=request.actor)
        existing = Withdrawal.objects.filter(user=request.actor, request_key=key).first()
        if existing:
            if existing.amount_cents != cents or existing.destination != destination:
                raise ApiError('idempotency_conflict', 409)
            return JsonResponse({'id': existing.pk, 'status': existing.status})
        if cents > row.balance_cents - row.reserved_cents:
            raise ApiError('insufficient_funds', 409)
        row.reserved_cents += cents
        row.save(update_fields=['reserved_cents'])
        withdrawal = Withdrawal.objects.create(user=request.actor, amount_cents=cents, destination=destination, request_key=key)
    return JsonResponse({'id': withdrawal.pk, 'status': 'pending'}, status=201)

@endpoint(['POST'])
def reports(request):
    p = request.payload
    kind = text(p, 'target_type', 1, 32)
    target_id = amount(p, 'target_id')
    model = {'service': Service, 'reel': Reel, 'user': User, 'message': Message}.get(kind)
    if model is None:
        raise ApiError('invalid_target')
    queryset = model.objects.filter(pk=target_id)
    if model is Message:
        queryset = queryset.filter(Q(sender=request.actor) | Q(recipient=request.actor))
    if not queryset.exists():
        raise ApiError('target_unavailable', 404)
    report = Report.objects.create(reporter=request.actor, target_type=kind, target_id=target_id, reason=text(p, 'reason', 3, 500))
    return JsonResponse({'id': report.pk}, status=201)
