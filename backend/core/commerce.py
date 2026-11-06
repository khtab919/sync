import base64
import binascii
import io
import re
import secrets
import uuid
from datetime import timedelta
from django.core import signing
from django.core.files.base import ContentFile
from django.db import transaction
from django.db.models import Sum, Case, When, Value, IntegerField
from django.http import FileResponse, JsonResponse, HttpResponse
from django.utils import timezone
from .views import endpoint, ApiError, text, amount, User
from .mobile_admin import require_admin, audit
from .models import UploadedMedia, Service, Reel, Message, Profile, Wallet, SellerApplication, TopUp

ADDRESS = '0xc35797768180752b67743d791f79f812bb0972d1'


def grant_url(request, media):
    token = signing.dumps({'media': str(media.pk), 'viewer': request.actor.pk}, salt='media-access')
    return request.build_absolute_uri(f'/api/media/{media.pk}?grant={token}')


def may_read(media, user):
    if user is None or not user.is_active:
        return False
    if user.is_staff and user.is_superuser:
        return True
    if not media.owner.is_active:
        return False
    if user.pk == media.owner_id:
        return True
    if media.purpose.startswith('chat_'):
        return Message.objects.filter(attachment=media, recipient=user).exists()
    return False


def media_file(request, media_id):
    media = UploadedMedia.objects.select_related('owner').filter(pk=media_id).first()
    if media is None:
        return HttpResponse(status=404)
    public = media.owner.is_active and (Service.objects.filter(image=media, active=True, approved=True).exists() or Reel.objects.filter(media=media, approved=True).exists())
    if not public:
        try:
            data = signing.loads(request.GET.get('grant', ''), salt='media-access', max_age=600)
            viewer = User.objects.filter(pk=data['viewer'], is_active=True).first()
            if data['media'] != str(media.pk) or not may_read(media, viewer):
                return HttpResponse(status=403)
        except (signing.BadSignature, KeyError, ValueError):
            return HttpResponse(status=403)
    response = FileResponse(media.file.open('rb'), content_type=media.mime)
    response['Cache-Control'] = 'private, no-store'
    response['X-Content-Type-Options'] = 'nosniff'
    response['Content-Security-Policy'] = "default-src 'none'; media-src 'self'"
    return response


@endpoint(['POST'])
def upload(request):
    purpose = request.payload.get('purpose')
    if purpose not in ('offer', 'receipt', 'chat_image', 'chat_audio', 'reel'):
        raise ApiError('invalid_purpose')
    if purpose in ('offer', 'reel') and not Profile.objects.filter(user=request.actor, creator=True).exists():
        raise ApiError('creator_account_required', 403)
    encoded = text(request.payload, 'data', 4, 28 * 1024 * 1024)
    try:
        raw = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError):
        raise ApiError('invalid_file')
    maximum = 20 * 1024 * 1024 if purpose == 'reel' else 8 * 1024 * 1024
    if not 16 <= len(raw) <= maximum:
        raise ApiError('file_too_large')
    if purpose in ('offer', 'receipt', 'chat_image'):
        from PIL import Image, UnidentifiedImageError
        try:
            with Image.open(io.BytesIO(raw)) as image:
                if image.width * image.height > 25000000 or image.format not in ('JPEG', 'PNG', 'WEBP'):
                    raise ApiError('invalid_image')
                image.load()
                image.thumbnail((2048, 2048))
                output = io.BytesIO()
                image.convert('RGB').save(output, 'JPEG', quality=88)
                raw = output.getvalue()
        except (UnidentifiedImageError, OSError, Image.DecompressionBombError):
            raise ApiError('invalid_image')
        mime, suffix = 'image/jpeg', '.jpg'
    else:
        # Accept ISO base media (Android M4A recordings / MP4 reels), never HTML/SVG.
        if raw[4:8] != b'ftyp' or b'mdat' not in raw or b'moov' not in raw:
            raise ApiError('invalid_media')
        mime, suffix = ('audio/mp4', '.m4a') if purpose == 'chat_audio' else ('video/mp4', '.mp4')
    with transaction.atomic():
        User.objects.select_for_update().get(pk=request.actor.pk)
        used = UploadedMedia.objects.filter(owner=request.actor).aggregate(n=Sum('size'))['n'] or 0
        if used + len(raw) > 500 * 1024 * 1024:
            raise ApiError('storage_limit', 429)
        row = UploadedMedia(owner=request.actor, purpose=purpose, size=len(raw), mime=mime)
        row.file.save(uuid.uuid4().hex + suffix, ContentFile(raw), save=False)
        row.save()
    return JsonResponse({'id': row.pk, 'url': grant_url(request, row), 'mime': mime}, status=201)


@endpoint(['GET'])
def payment_info(request):
    return JsonResponse({'network': 'BNB Smart Chain (BEP20)', 'currency': 'USDT', 'address': ADDRESS, 'qr_url': request.build_absolute_uri('/api/payment-qr'), 'packages_cents': [1000, 2500, 5000, 10000], 'offers': [{'amount_cents': 2500, 'bonus_percent': 4}, {'amount_cents': 5000, 'bonus_percent': 6}, {'amount_cents': 10000, 'bonus_percent': 8}], 'credit_rate': '1 USDT = 1 USD internal balance', 'manual_review': True})


def payment_qr(request):
    import qrcode
    output = io.BytesIO()
    qrcode.make(ADDRESS).save(output, format='PNG')
    return HttpResponse(output.getvalue(), content_type='image/png')


@endpoint(['GET', 'POST'])
def topups(request):
    if request.method == 'GET':
        return JsonResponse({'items': [{'id': r.pk, 'amount_cents': r.amount_cents, 'bonus_cents': r.bonus_cents, 'status': r.status, 'transaction_hash': r.transaction_hash, 'review_note': r.review_note} for r in TopUp.objects.filter(user=request.actor).order_by('-created_at')[:100]]})
    cents = amount(request.payload, 'amount_cents')
    if cents < 100:
        raise ApiError('minimum_topup_one_usdt')
    tx = text(request.payload, 'transaction_hash', 64, 66).lower().removeprefix('0x')
    if not re.fullmatch('[0-9a-f]{64}', tx):
        raise ApiError('invalid_transaction_hash')
    with transaction.atomic():
        User.objects.select_for_update().get(pk=request.actor.pk)
        previous = TopUp.objects.filter(transaction_hash='0x' + tx).first()
        if previous:
            if previous.user_id == request.actor.pk and previous.amount_cents == cents and str(previous.receipt_id) == request.payload.get('receipt_id'):
                return JsonResponse({'id': previous.pk, 'status': previous.status})
            raise ApiError('transaction_already_submitted', 409)
        receipt = UploadedMedia.objects.filter(pk=uuid.UUID(text(request.payload, 'receipt_id', 36, 36)), owner=request.actor, purpose='receipt').first()
        if not receipt:
            raise ApiError('receipt_required')
        bonus = cents * (8 if cents >= 10000 else 6 if cents >= 5000 else 4 if cents >= 2500 else 0) // 100
        row = TopUp.objects.create(user=request.actor, amount_cents=cents, transaction_hash='0x' + tx, receipt=receipt, bonus_cents=bonus)
    return JsonResponse({'id': row.pk, 'status': row.status, 'bonus_cents': bonus}, status=201)


@endpoint(['GET', 'POST'])
def seller_requests(request):
    if request.method == 'POST':
        description = text(request.payload, 'description', 10, 1000)
        with transaction.atomic():
            User.objects.select_for_update().get(pk=request.actor.pk)
            if Profile.objects.filter(user=request.actor, creator=True).exists():
                raise ApiError('already_seller', 409)
            if SellerApplication.objects.filter(user=request.actor, status='pending').exists():
                raise ApiError('seller_request_pending', 409)
            SellerApplication.objects.create(user=request.actor, description=description)
    return JsonResponse({'items': list(SellerApplication.objects.filter(user=request.actor).order_by('-id').values('id', 'description', 'status', 'review_note')[:20])})


@endpoint(['GET', 'POST'])
def review_requests(request):
    require_admin(request)
    if request.method == 'POST':
        kind = request.payload.get('kind')
        decision = request.payload.get('decision')
        if kind not in ('topup', 'seller') or decision not in ('approve', 'reject'):
            raise ApiError('invalid_action')
        note = text(request.payload, 'review_note', 3, 300)
        model = TopUp if kind == 'topup' else SellerApplication
        pk = uuid.UUID(text(request.payload, 'id', 36, 36)) if kind == 'topup' else amount(request.payload, 'id')
        with transaction.atomic():
            row = model.objects.select_for_update().filter(pk=pk).first()
            if not row:
                raise ApiError('request_unavailable', 404)
            wanted = 'approved' if decision == 'approve' else 'rejected'
            if row.status != 'pending':
                if row.status != wanted:
                    raise ApiError('request_already_reviewed', 409)
            else:
                if wanted == 'approved':
                    if kind == 'topup':
                        if request.payload.get('payment_verified') is not True:
                            raise ApiError('verify_transaction_on_chain')
                        Wallet.objects.get_or_create(user=row.user)
                        wallet = Wallet.objects.select_for_update().get(user=row.user)
                        wallet.balance_cents += row.amount_cents + row.bonus_cents
                        wallet.save(update_fields=['balance_cents'])
                    else:
                        profile, _ = Profile.objects.get_or_create(user=row.user)
                        profile.creator = True
                        profile.save(update_fields=['creator'])
                row.status, row.reviewer, row.review_note = wanted, request.actor, note
                row.save(update_fields=['status', 'reviewer', 'review_note'])
                audit(request.actor, row, f'{wanted}: {note}')
    payments = [{'id': str(r.pk), 'kind': 'topup', 'user_id': r.user_id, 'username': r.user.username, 'amount_cents': r.amount_cents, 'bonus_cents': r.bonus_cents, 'transaction_hash': r.transaction_hash, 'receipt_url': grant_url(request, r.receipt), 'status': r.status, 'review_note': r.review_note} for r in TopUp.objects.select_related('user', 'receipt').order_by(Case(When(status='pending', then=Value(0)), default=Value(1), output_field=IntegerField()), '-created_at')[:100]]
    sellers = [{'id': str(r.pk), 'kind': 'seller', 'user_id': r.user_id, 'username': r.user.username, 'description': r.description, 'status': r.status, 'review_note': r.review_note} for r in SellerApplication.objects.select_related('user').order_by(Case(When(status='pending', then=Value(0)), default=Value(1), output_field=IntegerField()), '-id')[:100]]
    return JsonResponse({'items': payments + sellers})


def message_json(request, row):
    return {'id': row.pk, 'sender_id': row.sender_id, 'recipient_id': row.recipient_id, 'body': row.body, 'created_at': row.created_at, 'attachment_url': grant_url(request, row.attachment) if row.attachment_id else '', 'attachment_mime': row.attachment.mime if row.attachment_id else '', 'call_url': row.call_url}


def wheel_settings(config):
    import hashlib, json
    prizes = config.prizes
    valid = isinstance(prizes, list) and 1 <= len(prizes) <= 20 and all(isinstance(p, dict) and type(p.get('amount_cents')) is int and 0 <= p['amount_cents'] <= 1000000 and type(p.get('weight')) is int and 1 <= p['weight'] <= 1000000 for p in prizes)
    valid = valid and 1 <= config.cost_cents <= 10000
    total = sum(p['weight'] for p in prizes) if valid else 0
    version = hashlib.sha256(json.dumps([config.cost_cents, prizes], sort_keys=True).encode()).hexdigest()
    return valid, total, version


@endpoint(['GET', 'POST'])
def paid_wheel(request):
    from .models import PaidWheelConfiguration, PaidSpin
    config, _ = PaidWheelConfiguration.objects.get_or_create(pk=1)
    valid, total, version = wheel_settings(config)
    if request.method == 'GET':
        return JsonResponse({'enabled': bool(config.enabled and valid), 'cost_cents': config.cost_cents, 'config_version': version, 'prizes': [dict(amount_cents=p['amount_cents'], chance_percent=format(p['weight'] * 100 / total, '.8f')) for p in config.prizes] if valid else []})
    key = uuid.UUID(text(request.payload, 'request_key', 36, 36))
    with transaction.atomic():
        config = PaidWheelConfiguration.objects.select_for_update().get(pk=1)
        valid, total, version = wheel_settings(config)
        previous = PaidSpin.objects.filter(user=request.actor, request_key=key).first()
        if previous:
            wallet = Wallet.objects.get(user=request.actor)
            return JsonResponse({'prize_cents': previous.prize_cents, 'balance_cents': wallet.balance_cents, 'replayed': True})
        if not config.enabled or not valid:
            raise ApiError('wheel_unavailable', 409)
        if request.payload.get('config_version') != version:
            raise ApiError('wheel_configuration_changed', 409)
        wallet = Wallet.objects.select_for_update().get(user=request.actor)
        if wallet.balance_cents - wallet.reserved_cents < config.cost_cents:
            raise ApiError('insufficient_funds', 409)
        if config.bankroll_cents + config.cost_cents < max(p['amount_cents'] for p in config.prizes):
            raise ApiError('wheel_unavailable', 409)
        # Uniform cryptographic draw over documented integer weights; no per-user odds.
        pick = secrets.randbelow(total)
        prize = 0
        for p in config.prizes:
            if pick < p['weight']:
                prize = p['amount_cents']; break
            pick -= p['weight']
        wallet.balance_cents += prize - config.cost_cents
        wallet.save(update_fields=['balance_cents'])
        config.bankroll_cents += config.cost_cents - prize
        config.save(update_fields=['bankroll_cents'])
        PaidSpin.objects.create(user=request.actor, request_key=key, cost_cents=config.cost_cents, prize_cents=prize, prizes_snapshot=config.prizes)
    return JsonResponse({'prize_cents': prize, 'balance_cents': wallet.balance_cents}, status=201)


@endpoint(['GET', 'POST'])
def wheel_configuration(request):
    from .models import PaidWheelConfiguration
    require_admin(request)
    with transaction.atomic():
        config, _ = PaidWheelConfiguration.objects.get_or_create(pk=1)
        config = PaidWheelConfiguration.objects.select_for_update().get(pk=1)
        if request.method == 'POST':
            previous_settings = {'enabled': config.enabled, 'cost_cents': config.cost_cents, 'prizes': config.prizes, 'bankroll_cents': config.bankroll_cents}
            p = request.payload
            if type(p.get('enabled')) is not bool or type(p.get('cost_cents')) is not int or type(p.get('bankroll_cents')) is not int or not 0 <= p['bankroll_cents'] <= 100000000:
                raise ApiError('invalid_wheel_configuration')
            if p.get('expected_bankroll_cents') != config.bankroll_cents:
                raise ApiError('wheel_configuration_changed', 409)
            config.enabled, config.cost_cents, config.prizes = p['enabled'], p['cost_cents'], p.get('prizes')
            config.bankroll_cents = p['bankroll_cents']
            valid, _, _ = wheel_settings(config)
            if not valid or (config.enabled and config.bankroll_cents < max(prize['amount_cents'] for prize in config.prizes)):
                raise ApiError('invalid_wheel_configuration')
            config.save()
            import json
            audit(request.actor, config, json.dumps({'before': previous_settings, 'after': {'enabled': config.enabled, 'cost_cents': config.cost_cents, 'prizes': config.prizes, 'bankroll_cents': config.bankroll_cents}}))
    return JsonResponse({'enabled': config.enabled, 'cost_cents': config.cost_cents, 'prizes': config.prizes, 'bankroll_cents': config.bankroll_cents})
