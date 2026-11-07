import hashlib
import json
import secrets
import uuid
from datetime import date, timedelta
from functools import wraps
from django.contrib.auth import authenticate, get_user_model
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError,ObjectDoesNotExist
from django.db import IntegrityError, connection, transaction
from django.db.models import Q
from django.http import JsonResponse
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from .models import Profile, Service, Booking, Wallet, Withdrawal, Message, Reel, Report, SessionToken, AuthRateLimit, ServiceReaction, Follow, SupportTicket

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
                if authenticated:
                    from . import admin_security as security
                    if request.path.startswith('/api/admin/') and not request.path.startswith('/api/admin/control/') and request.path!='/api/admin/auth':
                        security.require(request,security.route_scope(request),request.method!='GET')
                        if request.method!='GET':
                            target=security.financial_target(request)
                            if security.approval_required(request,target):
                                pending=security.prepare_approval(request,target)
                                if pending:return pending
                                with transaction.atomic():
                                    security.financial_approval(request,target)
                                    return fn(request,*args,**kwargs)
                    if request.path!='/api/my-cases':security.check_operation(request)
                return fn(request, *args, **kwargs)
            except ApiError as exc:
                return JsonResponse({'error': exc.message}, status=exc.status)
            except (ValueError, TypeError, KeyError, ValidationError):
                return JsonResponse({'error': 'invalid_input'}, status=400)
            except ObjectDoesNotExist:
                return JsonResponse({'error':'resource_unavailable'},status=404)
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
    if not isinstance(value, str) or not value.strip() or not minimum <= len(value) <= 128:
        raise ApiError('invalid_password')
    return value

def issue_token(user):
    raw = secrets.token_urlsafe(48)
    SessionToken.objects.create(user=user, digest=hashlib.sha256(raw.encode()).hexdigest(), expires_at=timezone.now() + (timedelta(hours=1) if user.is_staff else timedelta(days=7)))
    return raw

@endpoint(['GET'], authenticated=False)
def health(request):
    with connection.cursor() as cursor:
        cursor.execute('SELECT 1')
    return JsonResponse({'status': 'ok', 'registration_version': 4, 'identity_required': False, 'brand': 'Relaxia', 'mobile_admin_version': 2, 'commerce_version': 1, 'calls_version': 2, 'messaging_version': 3, 'paid_chat_version':1, 'shop_version': 2, 'live_version':1, 'booking_payment_version':1})

@endpoint(['POST'], authenticated=False)
def register(request):
    p = request.payload
    username = ' '.join(text(p, 'username', 2, 50).split()).removeprefix('@')
    import unicodedata
    if len(username) < 2 or not all(unicodedata.category(c)[0] in 'LMN' or c in '._- ' for c in username):
        raise ApiError('invalid_username')
    if username.lower()=='super_admin':raise ApiError('username_unavailable',409)
    email = text(p, 'email', 3, 150)
    from django.core.validators import validate_email
    try:
        validate_email(email)
    except ValidationError:
        raise ApiError('invalid_email')
    password = password_value(p, 8)
    if p.get('accept_non_explicit_policy') is not True:
        raise ApiError('adult_policy_required')
    born = None
    if 'date_of_birth' in p:
        # Compatibility with existing clients; supplied dates remain validated.
        born = date.fromisoformat(text(p, 'date_of_birth', 10, 10))
        today = timezone.localdate()
        age = today.year - born.year - ((today.month, today.day) < (born.month, born.day))
        if born > today or not 18 <= age <= 120:
            raise ApiError('adult_policy_required')
    elif p.get('adult_confirmed') is not True:
        raise ApiError('adult_policy_required')
    user = User(username=username, email=email)
    from django.contrib.auth.password_validation import MinimumLengthValidator, CommonPasswordValidator
    try:
        validate_password(password, user=user, password_validators=[MinimumLengthValidator(8), CommonPasswordValidator()])
    except ValidationError as error:
        codes = {item.code for item in error.error_list}
        raise ApiError('password_too_common' if 'password_too_common' in codes else 'invalid_password')
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
    user = authenticate(username=' '.join(text(p, 'username', 2, 50).split()).removeprefix('@').lower(), password=password_value(p))
    if user is None:
        raise ApiError('invalid_credentials', 401)
    if user.is_staff and user.is_superuser:
        with transaction.atomic():
            Profile.objects.get_or_create(user=user)
            Wallet.objects.get_or_create(user=user)
    elif not Profile.objects.filter(user=user).exists():
        raise ApiError('invalid_credentials', 401)
    return JsonResponse({'token': issue_token(user), 'user_id': user.pk})

@endpoint(['POST'])
def logout(request):
    request.api_token.delete()
    return JsonResponse({'ok': True})

def profile_json(user, viewer):
    profile = Profile.objects.get(user=user)
    from .models import BenefitGrant
    frame=BenefitGrant.objects.filter(user=user,kind='frame',expires_at__gt=timezone.now()).order_by('-id').first()
    return {
        'account_restricted':__import__('core.admin_security',fromlist=['blocked']).blocked(user,'ban') if user==viewer else False,
        'id': user.pk, 'username': user.username,
        'display_name': profile.display_name or user.username,
        'bio': profile.bio, 'email': user.email if user == viewer else '', 'creator': profile.creator,
        'identity_verified': profile.identity_verified,
        'selected_frame': frame.value if frame else profile.selected_frame, 'avatar_url': profile.avatar_url, 'chat_price_cents': profile.chat_price_cents,
        'followers_count': Follow.objects.filter(creator=user, user__is_active=True).count(),
        'following_count': Follow.objects.filter(user=user, creator__is_active=True).count(),
        'service_count': Service.objects.filter(owner=user, approved=True, active=True).count(),
        'reel_count': Reel.objects.filter(owner=user, approved=True).count(),
        'following': Follow.objects.filter(user=viewer, creator=user).exists(),
        'is_staff': user.is_staff if user == viewer else False,
        'is_admin': bool(__import__('core.admin_security',fromlist=['scopes']).scopes(user)) if user==viewer else False,
        'admin_permissions':__import__('core.admin_security',fromlist=['scopes']).scopes(user) if user==viewer else [],
        'admin_scope':('full' if user.is_superuser else 'live') if user==viewer and user.is_staff else '',
    }

@endpoint(['GET', 'PATCH'])
def me(request):
    profile = Profile.objects.get(user=request.actor)
    if request.method == 'PATCH':
        p = request.payload
        if set(p) - {'bio', 'display_name', 'selected_frame', 'chat_price_cents', 'avatar_url', 'email'}:
            raise ApiError('unsupported_profile_field')
        if 'chat_price_cents' in p and (type(p['chat_price_cents']) is not int or not 0 <= p['chat_price_cents'] <= 500000 or not profile.creator):
            raise ApiError('invalid_chat_price')
        if 'selected_frame' in p and (type(p['selected_frame']) is not int or p['selected_frame'] not in range(11)):
            raise ApiError('invalid_frame')
        avatar_url = p.get('avatar_url', profile.avatar_url)
        if not isinstance(avatar_url, str) or len(avatar_url) > 500:
            raise ApiError('invalid_image_url')
        if avatar_url:
            from django.core.validators import URLValidator
            try:
                URLValidator(schemes=['https'])(avatar_url)
            except ValidationError:
                raise ApiError('invalid_image_url')
        email = request.actor.email
        if 'email' in p:
            from django.core.validators import validate_email
            email = text(p, 'email', 3, 150)
            try:
                validate_email(email)
            except ValidationError:
                raise ApiError('invalid_email')
        # Validate the whole patch before persisting any changes.
        bio = text(p, 'bio', 0, 300) if 'bio' in p else profile.bio
        name = text(p, 'display_name', 1, 100) if 'display_name' in p else profile.display_name
        profile.bio, profile.display_name = bio, name
        profile.avatar_url = avatar_url
        profile.selected_frame = p.get('selected_frame', profile.selected_frame)
        profile.chat_price_cents = p.get('chat_price_cents', profile.chat_price_cents)
        with transaction.atomic():
            profile.save()
            if 'email' in p:
                request.actor.email = email
                request.actor.save(update_fields=['email'])
    return JsonResponse(profile_json(request.actor, request.actor))

def service_json(service, viewer):
    owner = Profile.objects.get(user=service.owner)
    return {'duration_minutes':service.duration_minutes,'terms':service.terms,'id': service.pk, 'owner_id': service.owner_id,
            'owner_name': owner.display_name or service.owner.username,
            'owner_frame': owner.selected_frame, 'owner_avatar_url': owner.avatar_url, 'title': service.title,
            'description': service.description, 'price_cents': service.price_cents,
            'image_url': service.image_url, 'category': service.category,
            'liked': ServiceReaction.objects.filter(service=service, user=viewer, kind='like').exists(),
            'saved': ServiceReaction.objects.filter(service=service, user=viewer, kind='save').exists(),
            'like_count': ServiceReaction.objects.filter(service=service, kind='like', user__is_active=True).count()}

@endpoint(['GET', 'POST'])
def services(request):
    if request.method == 'GET':
        rows = Service.objects.filter(approved=True, active=True, owner__is_active=True).exclude(category='كورس').select_related('owner').order_by('-id')[:100]
        return JsonResponse({'items': [service_json(row, request.actor) for row in rows]})
    if not Profile.objects.filter(user=request.actor, creator=True).exists():
        raise ApiError('creator_account_required', 403)
    p = request.payload
    image_url = p.get('image_url', '')
    if image_url:
        from django.core.validators import URLValidator
        URLValidator(schemes=['https'])(image_url)
        if len(image_url) > 500:
            raise ApiError('invalid_image_url')
    category = p.get('category', 'خدمة')
    if category not in ('خدمة', 'منتج', 'جلسة'):
        raise ApiError('invalid_category')
    from .models import UploadedMedia
    media = None
    if p.get('image_id'):
        media = UploadedMedia.objects.filter(pk=uuid.UUID(text(p, 'image_id', 36, 36)), owner=request.actor, purpose='offer').first()
        if not media:
            raise ApiError('invalid_image')
        image_url = request.build_absolute_uri(f'/api/media/{media.pk}')
    duration=p.get('duration_minutes',30)
    if type(duration) is not int or not 1<=duration<=1440:raise ApiError('invalid_duration')
    terms=p.get('terms','')
    if not isinstance(terms,str) or len(terms)>600:raise ApiError('invalid_terms')
    service = Service.objects.create(duration_minutes=duration,terms=terms,image=media, owner=request.actor, title=text(p, 'title', 3, 150), description=text(p, 'description', 10, 600), price_cents=amount(p, 'price_cents'), image_url=image_url, category=category)
    return JsonResponse({'id': service.pk, 'status': 'pending_review'}, status=201)

@endpoint(['POST'])
def reaction(request, service_id):
    service = Service.objects.filter(pk=service_id, active=True, approved=True, owner__is_active=True).exclude(category='كورس').first()
    if service is None:
        raise ApiError('service_unavailable', 404)
    kind = request.payload.get('kind')
    enabled = request.payload.get('enabled')
    if kind not in ('like', 'save') or type(enabled) is not bool:
        raise ApiError('invalid_reaction')
    if enabled:
        ServiceReaction.objects.get_or_create(user=request.actor, service=service, kind=kind)
    else:
        ServiceReaction.objects.filter(user=request.actor, service=service, kind=kind).delete()
    return JsonResponse(service_json(service, request.actor))

@endpoint(['GET'])
def saved(request):
    ids = ServiceReaction.objects.filter(user=request.actor, kind='save').values_list('service_id', flat=True)
    rows = Service.objects.filter(pk__in=ids, active=True, approved=True, owner__is_active=True).exclude(category='كورس').select_related('owner').order_by('-id')[:100]
    return JsonResponse({'items': [service_json(row, request.actor) for row in rows]})

@endpoint(['GET'])
def creators(request):
    users = User.objects.filter(is_active=True, profile__creator=True).order_by('id')[:100]
    return JsonResponse({'items': [profile_json(user, request.actor) for user in users]})

@endpoint(['POST'])
def follow(request, creator_id):
    creator = User.objects.filter(pk=creator_id, is_active=True, profile__creator=True).first()
    if creator is None or creator == request.actor:
        raise ApiError('creator_unavailable', 404)
    enabled = request.payload.get('enabled')
    if type(enabled) is not bool:
        raise ApiError('invalid_follow')
    if enabled:
        Follow.objects.get_or_create(user=request.actor, creator=creator)
    else:
        Follow.objects.filter(user=request.actor, creator=creator).delete()
    return JsonResponse(profile_json(creator, request.actor))

@endpoint(['GET'])
def conversations(request):
    rows = Message.objects.filter(Q(sender=request.actor) | Q(recipient=request.actor)).select_related('sender', 'recipient').order_by('-id')
    peers, result = set(), []
    for message in rows.iterator():
        peer = message.recipient if message.sender_id == request.actor.pk else message.sender
        if peer.pk in peers or not peer.is_active:
            continue
        peers.add(peer.pk)
        profile = Profile.objects.filter(user=peer).first()
        if profile is None:
            continue
        result.append({'id': peer.pk, 'display_name': profile.display_name or peer.username,
                       'selected_frame': frame.value if frame else profile.selected_frame, 'avatar_url': profile.avatar_url, 'last_message': message.body,
                       'created_at': message.created_at})
        if len(result) == 100:
            break
    return JsonResponse({'items': result})

@endpoint(['GET', 'POST'])
def support(request):
    if request.method == 'POST':
        row = SupportTicket.objects.create(user=request.actor, body=text(request.payload, 'body', 3, 2000))
        return JsonResponse({'id': row.pk}, status=201)
    return JsonResponse({'items': list(SupportTicket.objects.filter(user=request.actor).order_by('-id').values('id', 'body', 'reply', 'created_at')[:100])})

@endpoint(['GET', 'POST'])
def bookings(request):
    if request.method == 'GET':
        rows=Booking.objects.filter(Q(buyer=request.actor)|Q(service__owner=request.actor)).select_related('service__owner__profile','buyer__profile','operation').order_by('-created_at')[:100]
        return JsonResponse({'items':[{'id':r.pk,'service_id':r.service_id,'title':r.service.title,'owner_name':r.service.owner.profile.display_name or r.service.owner.username,'buyer_name':r.buyer.profile.display_name or r.buyer.username,'owner_id':r.service.owner_id, 'owner':r.service.owner_id==request.actor.pk,'status':r.status,'price_cents':r.price_cents,'duration_minutes':r.duration_minutes,'terms':r.terms_snapshot,'payment_status':r.operation.status if r.operation_id else 'unpaid','created_at':r.created_at} for r in rows]})
    from . import finance
    from .platform_features import blocked
    service=Service.objects.select_related('owner').filter(pk=amount(request.payload,'service_id'),approved=True,active=True,owner__is_active=True).exclude(category__in=['تعليم','كورس']).first()
    if not service or service.owner_id==request.actor.pk or blocked(request.actor,service.owner):raise ApiError('service_unavailable',404)
    paid=request.payload.get('confirmed') is True
    if paid and (type(request.payload.get('price_cents')) is not int or request.payload['price_cents']!=service.price_cents):raise ApiError('paid_price_changed',409)
    key=uuid.UUID(text(request.payload,'request_key',36,36)) if paid else None
    with transaction.atomic():
        list(User.objects.select_for_update().filter(pk__in=sorted([request.actor.pk,service.owner_id])).order_by('pk'))
        if key:
            previous=Booking.objects.filter(buyer=request.actor,request_key=key).first()
            if previous:
                if previous.service_id!=service.pk:raise ApiError('payment_request_conflict',409)
                return JsonResponse({'id':previous.pk,'status':previous.status,'price_cents':previous.price_cents})
        booking=Booking.objects.create(buyer=request.actor,service=service,price_cents=service.price_cents,request_key=key,terms_snapshot=service.terms or service.description,duration_minutes=service.duration_minutes)
        if paid:
            booking.operation=finance.hold(request.actor,service.owner,'booking',key,str(booking.pk),service.price_cents);booking.status='paid_reserved';booking.save(update_fields=['operation','status'])
    return JsonResponse({'id':booking.pk,'status':booking.status,'price_cents':booking.price_cents},status=201)

@endpoint(['GET', 'POST'])
def messages(request, peer_id):
    from .commerce import message_json
    from .models import UploadedMedia
    peer = User.objects.filter(pk=peer_id, is_active=True).first()
    if not peer or peer == request.actor:
        raise ApiError('recipient_unavailable', 404)
    if request.method == 'GET':
        rows = Message.objects.filter(Q(sender=request.actor, recipient=peer) | Q(sender=peer, recipient=request.actor)).select_related('attachment', 'call', 'sender__profile', 'recipient__profile','reply_to').order_by('-id')[:100]
        peer_profile = Profile.objects.filter(user=peer).first()
        return JsonResponse({'peer': {'id':peer.pk,'username':peer.username,'display_name':(peer_profile.display_name if peer_profile else '') or peer.username,'avatar_url':peer_profile.avatar_url if peer_profile else '', 'creator':bool(peer_profile and peer_profile.creator), 'chat_price_cents':peer_profile.chat_price_cents if peer_profile else 0, 'online':bool(peer_profile and peer_profile.last_active_at and peer_profile.last_active_at > timezone.now()-timedelta(seconds=25)), 'last_active_at':peer_profile.last_active_at if peer_profile else None}, 'items': [message_json(request, row) for row in rows]})
    from .platform_features import blocked
    if blocked(request.actor,peer):raise ApiError('recipient_unavailable',403)
    submitted_body=request.payload.get('body','')
    if not isinstance(submitted_body,str) or len(submitted_body)>2000:raise ApiError('invalid_body')
    client_key = uuid.UUID(text(request.payload,'client_key',36,36)) if request.payload.get('client_key') else None
    with transaction.atomic():
        people={u.pk:u for u in User.objects.select_for_update().filter(pk__in=sorted([request.actor.pk,peer.pk])).order_by('pk')}
        peer=people.get(peer.pk)
        if not peer or not peer.is_active or not people[request.actor.pk].is_active:raise ApiError('recipient_unavailable',404)
        if client_key:
            previous = Message.objects.filter(sender=request.actor, client_key=client_key).first()
            if previous:
                if previous.recipient_id != peer.pk or previous.body != request.payload.get('body','').strip() or previous.reply_to_id != request.payload.get('reply_to') or previous.media_price_cents != request.payload.get('media_price_cents',0) or (str(previous.attachment_id) if previous.attachment_id else None) != request.payload.get('attachment_id'):
                    raise ApiError('message_request_conflict', 409)
                return JsonResponse(message_json(request, previous))
        profile=Profile.objects.select_for_update().filter(user=peer).first()
        if not profile:raise ApiError('recipient_unavailable',404)
        price=profile.chat_price_cents if profile.creator else 0
        if price and (not client_key or type(request.payload.get('price_consent')) is not int or request.payload['price_consent']!=price):
            raise ApiError('paid_price_changed',409)
        p = request.payload
        attachment = None
        if p.get('attachment_id'):
            attachment = UploadedMedia.objects.filter(pk=uuid.UUID(text(p, 'attachment_id', 36, 36)), owner=request.actor, purpose__in=['chat_image', 'chat_audio', 'chat_video']).first()
            if not attachment:
                raise ApiError('invalid_attachment')
        body = p.get('body', '')
        if not isinstance(body, str) or len(body) > 2000:
            raise ApiError('invalid_body')
        if p.get('start_call') is True:
            raise ApiError('update_app_for_internal_calls', 409)
        if not body.strip() and not attachment:
            raise ApiError('invalid_body')
        if attachment:
            existing = Message.objects.filter(attachment=attachment).first()
            if existing:
                if existing.sender_id == request.actor.pk and existing.recipient_id == peer.pk:
                    return JsonResponse(message_json(request, existing))
                raise ApiError('attachment_already_used', 409)
        reply=None
        if p.get('reply_to') is not None:
            reply=Message.objects.filter(pk=amount(p,'reply_to')).filter(Q(sender=request.actor,recipient=peer)|Q(sender=peer,recipient=request.actor)).first()
            if not reply:raise ApiError('invalid_reply')
        media_price=p.get('media_price_cents',0)
        if type(media_price) is not int or not 0<=media_price<=1000000:raise ApiError('invalid_media_price')
        if media_price and (not attachment or attachment.purpose not in ('chat_image','chat_video') or not Profile.objects.filter(user=request.actor,creator=True).exists()):raise ApiError('creator_account_required',403)
        message=Message.objects.create(sender=request.actor,recipient=peer,body=body.strip(),attachment=attachment,client_key=client_key,media_price_cents=media_price,reply_to=reply)
        if price:
            from .commerce import charge_chat
            charge_chat(message,'message',request.actor,peer,price)
    return JsonResponse(message_json(request, message), status=201)

@endpoint(['POST'])
def delivered(request, peer_id):
    ids = request.payload.get('ids')
    if not isinstance(ids,list) or len(ids)>100 or not all(type(value) is int and value>0 for value in ids):
        raise ApiError('invalid_message_ids')
    Message.objects.filter(pk__in=ids,sender_id=peer_id,recipient=request.actor,delivered_at__isnull=True).update(delivered_at=timezone.now())
    return JsonResponse({'ok':True})

@endpoint(['GET', 'POST'])
def reels(request):
    from .models import UploadedMedia
    if request.method == 'POST':
        if not Profile.objects.filter(user=request.actor, creator=True).exists():
            raise ApiError('creator_account_required', 403)
        media = UploadedMedia.objects.filter(pk=uuid.UUID(text(request.payload, 'media_id', 36, 36)), owner=request.actor, purpose='reel').first()
        if not media:
            raise ApiError('invalid_media')
        reel = Reel.objects.create(owner=request.actor, media=media, video_url=request.build_absolute_uri(f'/api/media/{media.pk}'))
        return JsonResponse({'id': reel.pk, 'status': 'pending_review'}, status=201)
    return JsonResponse({'items': list(Reel.objects.filter(approved=True, owner__is_active=True).order_by('-id').values('id', 'owner_id', 'video_url')[:100])})

@endpoint(['GET'])
def wallet(request):
    row = Wallet.objects.get(user=request.actor)
    from .promotions import balance
    return JsonResponse({'promotional_points':balance(request.actor),'promotional_withdrawable':False,'currency': 'USD', 'balance_cents': row.balance_cents, 'reserved_cents': row.reserved_cents, 'available_cents': row.balance_cents - row.reserved_cents})

@endpoint(['GET', 'POST'])
def withdrawals(request):
    if request.method == 'GET':
        return JsonResponse({'items': list(Withdrawal.objects.filter(user=request.actor).order_by('-created_at').values('id', 'amount_cents', 'status', 'created_at', 'payout_reference','processed_at')[:100])})
    if not Profile.objects.filter(user=request.actor, creator=True).exists():
        raise ApiError('creator_account_required', 403)
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

@endpoint(['GET'])
def incoming_messages(request):
    from .commerce import message_json
    rows=Message.objects.filter(recipient=request.actor,delivered_at__isnull=True,sender__is_active=True).select_related('sender__profile','recipient__profile','attachment','call','reply_to').order_by('id')[:100]
    return JsonResponse({'items':[message_json(request,row) for row in rows]})

@endpoint(['POST'])
def acknowledge_messages(request):
    ids=request.payload.get('ids')
    if not isinstance(ids,list) or len(ids)>100 or not all(type(value) is int and value>0 for value in ids):raise ApiError('invalid_message_ids')
    Message.objects.filter(pk__in=ids,recipient=request.actor,delivered_at__isnull=True).update(delivered_at=timezone.now())
    return JsonResponse({'ok':True})


@endpoint(['POST'])
def presence(request):
    Profile.objects.filter(user=request.actor).update(last_active_at=timezone.now())
    return JsonResponse({'ok':True})


@endpoint(['POST'])
def read_messages(request, peer_id):
    ids=request.payload.get('ids')
    if not isinstance(ids,list) or len(ids)>100 or not all(type(i) is int and i>0 for i in ids):
        raise ApiError('invalid_message_ids')
    now=timezone.now()
    with transaction.atomic():
        rows=Message.objects.filter(pk__in=ids,sender_id=peer_id,recipient=request.actor)
        rows.filter(delivered_at__isnull=True).update(delivered_at=now)
        rows.filter(read_at__isnull=True).update(read_at=now)
    return JsonResponse({'ok':True})
