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
from django.http import FileResponse, JsonResponse, HttpResponse, StreamingHttpResponse
from django.utils import timezone
from .views import endpoint, ApiError, text, amount, User
from .mobile_admin import require_admin, audit
from .models import UploadedMedia, Service, Reel, Message, Profile, Wallet, SellerApplication, TopUp

ADDRESS = '0xc35797768180752b67743d791f79f812bb0972d1'


def grant_url(request, media):
    payload={'media':str(media.pk),'viewer':request.actor.pk}
    if media.owner_id!=request.actor.pk and request.actor.is_staff and hasattr(request,'api_token'):
        payload['admin_session']=request.api_token.pk
    token=signing.dumps(payload,salt='media-access')
    return request.build_absolute_uri(f'/api/media/{media.pk}?grant={token}')


def may_read(media, user):
    if user is None or not user.is_active:
        return False
    from .admin_security import blocked
    if blocked(user,'ban'):return False
    from .admin_security import scopes
    allowed=scopes(user)
    if (media.purpose=='receipt' and 'finance.view' in allowed) or (media.purpose in ['offer','reel'] and 'store.view' in allowed):return True
    if not media.owner.is_active:
        return False
    if user.pk == media.owner_id:
        return True
    if media.purpose.startswith('chat_'):
        row=Message.objects.filter(attachment=media,recipient=user).first()
        if not row or row.deleted_at:return False
        from .models import ChatCharge
        return not row.media_price_cents or ChatCharge.objects.filter(message=row,buyer=user,kind='unlock').exists()
    from .admin_security import scopes
    allowed=scopes(user)
    return (media.purpose=='receipt' and 'finance.view' in allowed) or (media.purpose in ['offer','reel'] and 'store.view' in allowed)


def media_file(request, media_id):
    media = UploadedMedia.objects.select_related('owner').filter(pk=media_id).first()
    if media is None:
        return HttpResponse(status=404)
    public = media.owner.is_active and ((media.purpose=='avatar' and Profile.objects.filter(user=media.owner,avatar_url__endswith=f'/api/media/{media.pk}').exists()) or Service.objects.filter(image=media, active=True, approved=True).exists() or Reel.objects.filter(media=media, approved=True).exists())
    if not public:
        try:
            data = signing.loads(request.GET.get('grant', ''), salt='media-access', max_age=600)
            viewer = User.objects.filter(pk=data['viewer'], is_active=True).first()
            if viewer and viewer.pk!=media.owner_id and viewer.is_staff and not media.purpose.startswith('chat_'):
                from .models import SessionToken
                session=SessionToken.objects.filter(pk=data.get('admin_session'),user=viewer,expires_at__gt=timezone.now(),admin_verified_at__gt=timezone.now()-timedelta(hours=1)).first()
                if not session:return HttpResponse(status=403)
            if data['media'] != str(media.pk) or not may_read(media, viewer):
                return HttpResponse(status=403)
        except (signing.BadSignature, KeyError, ValueError):
            return HttpResponse(status=403)
    # Android video players request byte ranges to read MP4 metadata and seek.
    # Keep media authorization above this block; partial responses never bypass it.
    size = media.file.size
    requested_range = request.headers.get('Range', '').strip()
    start, end = 0, size - 1
    if requested_range:
        match = re.fullmatch(r'bytes=(\d*)-(\d*)', requested_range)
        if not match or (not match.group(1) and not match.group(2)):
            response = HttpResponse(status=416)
            response['Content-Range'] = f'bytes */{size}'
        else:
            first, last = match.groups()
            if first:
                start = int(first)
                end = min(int(last), size - 1) if last else size - 1
            else:
                length = int(last)
                start = max(0, size - length) if length else size
                end = size - 1
            if start >= size or start > end:
                response = HttpResponse(status=416)
                response['Content-Range'] = f'bytes */{size}'
            else:
                count = end - start + 1
                def chunks():
                    with media.file.open('rb') as source:
                        source.seek(start)
                        remaining = count
                        while remaining:
                            chunk = source.read(min(64 * 1024, remaining))
                            if not chunk:
                                break
                            remaining -= len(chunk)
                            yield chunk
                response = StreamingHttpResponse(
                    chunks(), status=206, content_type=media.mime
                ) if request.method != 'HEAD' else HttpResponse(
                    status=206, content_type=media.mime
                )
                response['Content-Range'] = f'bytes {start}-{end}/{size}'
                response['Content-Length'] = str(count)
    else:
        if request.method == 'HEAD':
            response = HttpResponse(content_type=media.mime)
            response['Content-Length'] = str(size)
        else:
            response = FileResponse(media.file.open('rb'), content_type=media.mime)
    response['Accept-Ranges'] = 'bytes'
    response['Cache-Control'] = 'private, no-store'
    response['X-Content-Type-Options'] = 'nosniff'
    response['Content-Security-Policy'] = "default-src 'none'; media-src 'self'"
    return response


@endpoint(['POST'])
def upload(request):
    purpose = request.payload.get('purpose')
    if purpose not in ('offer', 'receipt', 'chat_image', 'chat_audio', 'chat_video', 'reel', 'avatar'):
        raise ApiError('invalid_purpose')
    if purpose in ('offer', 'reel') and not Profile.objects.filter(user=request.actor, publishing_allowed=True).exists():
        raise ApiError('creator_account_required', 403)
    encoded = text(request.payload, 'data', 4, 28 * 1024 * 1024)
    try:
        raw = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError):
        raise ApiError('invalid_file')
    maximum = 20 * 1024 * 1024 if purpose == 'reel' else 8 * 1024 * 1024
    if not 16 <= len(raw) <= maximum:
        raise ApiError('file_too_large')
    if purpose in ('offer', 'receipt', 'chat_image', 'avatar'):
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
        from .media_probe import validate
        validate(raw,purpose)
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


def funding_settings():
    from .models import PaymentSettings
    return PaymentSettings.objects.get_or_create(pk=1)[0]


def bonus_for(cents):
    return cents * (8 if cents >= 10000 else 6 if cents >= 5000 else 4 if cents >= 2500 else 0) // 100


@endpoint(['GET'])
def payment_info(request):
    from .nowpayments import configured
    settings = funding_settings()
    return JsonResponse({'network':'BNB Smart Chain (BEP20)', 'currency':'USDT', 'address':ADDRESS, 'qr_url':request.build_absolute_uri('/api/payment-qr'), 'packages_cents':[100,1000,2500,5000,10000], 'coin_rate':100, 'credit_rate':'100 coins = 1 USD internal balance', 'manual_review':True, 'nowpayments_enabled':configured(), 'shamcash_enabled':bool(settings.shamcash_enabled and settings.shamcash_account and settings.shamcash_rate>0), 'shamcash_account':settings.shamcash_account if settings.shamcash_enabled else '', 'shamcash_currency':settings.shamcash_currency, 'shamcash_instructions':settings.shamcash_instructions})


def payment_qr(request):
    import qrcode
    destination=ADDRESS
    if request.GET.get('quote'):
        from .models import TopUpQuote
        try:
            data=signing.loads(request.GET.get('grant',''),salt='payment-qr',max_age=86400)
            if data['quote']!=request.GET['quote']:return HttpResponse(status=403)
            destination=TopUpQuote.objects.get(pk=data['quote'],user_id=data['user']).destination
        except (signing.BadSignature,ValueError,KeyError,TopUpQuote.DoesNotExist):return HttpResponse(status=403)
    elif request.GET.get('method')=='shamcash':
        settings=funding_settings()
        if not settings.shamcash_enabled or not settings.shamcash_account:return HttpResponse(status=404)
        destination=settings.shamcash_account
    output=io.BytesIO();qrcode.make(destination).save(output,format='PNG')
    return HttpResponse(output.getvalue(),content_type='image/png')


def quote_json(row,request):
    return {'qr_url': request.build_absolute_uri('/api/payment-qr?quote='+str(row.pk)+'&grant='+signing.dumps({'quote':str(row.pk),'user':row.user_id},salt='payment-qr')), 'id':str(row.pk),'method':row.method,'amount_cents':row.amount_cents,'bonus_cents':row.bonus_cents,'coins':row.amount_cents,'payment_amount':str(row.payment_amount),'payment_currency':row.payment_currency,'destination':row.destination,'expires_at':row.expires_at,'duration_seconds':1800,'server_now_ms':int(timezone.now().timestamp()*1000)}


@endpoint(['POST'])
def quotes(request):
    from .models import TopUpQuote
    from decimal import Decimal, ROUND_HALF_UP
    cents=amount(request.payload,'amount_cents')
    if cents<100: raise ApiError('minimum_topup_one_usdt')
    method=request.payload.get('method','usdt')
    if method not in ('usdt','shamcash','nowpayments'): raise ApiError('invalid_payment_method')
    with transaction.atomic():
        User.objects.select_for_update().get(pk=request.actor.pk)
        existing=TopUpQuote.objects.filter(user=request.actor,method=method,amount_cents=cents,expires_at__gt=timezone.now(),topup__isnull=True,gatewayinvoice__credited_at__isnull=True).exclude(gatewayinvoice__status__in=['failed','expired','refunded']).first()
        if existing:return JsonResponse(quote_json(existing,request))
        settings=funding_settings()
        if method=='nowpayments':
            from .nowpayments import configured
            if not configured():raise ApiError('nowpayments_not_configured',409)
        if method=='shamcash' and not(settings.shamcash_enabled and settings.shamcash_account and settings.shamcash_rate>0): raise ApiError('shamcash_not_configured',409)
        rate=settings.shamcash_rate if method=='shamcash' else Decimal(1)
        row=TopUpQuote.objects.create(user=request.actor,method=method,amount_cents=cents,bonus_cents=bonus_for(cents),payment_amount=(Decimal(cents)*rate/100).quantize(Decimal('.01'),rounding=ROUND_HALF_UP),payment_currency=settings.shamcash_currency if method=='shamcash' else 'USD' if method=='nowpayments' else 'USDT',destination=settings.shamcash_account if method=='shamcash' else 'NOWPayments hosted checkout' if method=='nowpayments' else ADDRESS,expires_at=timezone.now()+timedelta(minutes=30))
    return JsonResponse(quote_json(row,request),status=201)


@endpoint(['GET', 'POST'])
def topups(request):
    from .models import TopUpQuote
    if request.method=='GET':
        from .models import GatewayInvoice
        manual=[{'id':r.pk,'method':r.method,'amount_cents':r.amount_cents,'bonus_cents':r.bonus_cents,'status':r.status,'transaction_hash':r.transaction_hash,'review_note':r.review_note,'late_submission':r.late_submission} for r in TopUp.objects.filter(user=request.actor).order_by('-created_at')[:100]]
        automatic=[{'id':str(r.quote_id),'method':'nowpayments','amount_cents':r.quote.amount_cents,'bonus_cents':r.quote.bonus_cents,'status':'approved' if r.credited_at else 'rejected' if r.status in ('failed','expired','refunded') else 'pending','review_note':r.status} for r in GatewayInvoice.objects.filter(quote__user=request.actor).select_related('quote').order_by('-created_at')[:100]]
        return JsonResponse({'items':automatic+manual})
    with transaction.atomic():
        User.objects.select_for_update().get(pk=request.actor.pk)
        quote=None
        if request.payload.get('quote_id'):
            quote=TopUpQuote.objects.filter(pk=uuid.UUID(text(request.payload,'quote_id',36,36)),user=request.actor).first()
            if not quote:raise ApiError('invalid_quote')
        cents=quote.amount_cents if quote else amount(request.payload,'amount_cents')
        if cents<100:raise ApiError('minimum_topup_one_usdt')
        method=quote.method if quote else 'usdt'
        if method=='nowpayments':raise ApiError('gateway_invoice_required',409)
        reference=text(request.payload,'transaction_hash',4,100).strip()
        if method=='usdt':
            reference=reference.lower().removeprefix('0x')
            if not re.fullmatch('[0-9a-f]{64}',reference):raise ApiError('invalid_transaction_hash')
            reference='0x'+reference
        else:
            if not re.fullmatch('[A-Za-z0-9_-]{4,80}',reference):raise ApiError('invalid_transaction_reference')
            reference='shamcash:'+reference
        previous=TopUp.objects.filter(transaction_hash=reference).first()
        if previous:
            if previous.user_id==request.actor.pk and previous.amount_cents==cents and str(previous.receipt_id)==request.payload.get('receipt_id'):return JsonResponse({'id':previous.pk,'status':previous.status})
            raise ApiError('transaction_already_submitted',409)
        receipt=UploadedMedia.objects.filter(pk=uuid.UUID(text(request.payload,'receipt_id',36,36)),owner=request.actor,purpose='receipt').first()
        if not receipt:raise ApiError('receipt_required')
        row=TopUp.objects.create(user=request.actor,method=method,quote=quote,late_submission=bool(quote and quote.expires_at<timezone.now()),amount_cents=cents,transaction_hash=reference,receipt=receipt,bonus_cents=quote.bonus_cents if quote else bonus_for(cents))
    return JsonResponse({'id':row.pk,'status':row.status,'bonus_cents':row.bonus_cents,'late_submission':row.late_submission},status=201)


@endpoint(['GET','POST'])
def payment_settings(request):
    require_admin(request)
    from decimal import Decimal, InvalidOperation
    from .models import PaymentSettings
    with transaction.atomic():
        settings=funding_settings();settings=PaymentSettings.objects.select_for_update().get(pk=settings.pk)
        if request.method=='POST':
            p=request.payload
            if type(p.get('enabled')) is not bool or p.get('currency') not in ('USD','SYP'):raise ApiError('invalid_payment_settings')
            account=p.get('account','')
            if not isinstance(account,str) or len(account)>160:raise ApiError('invalid_payment_settings')
            account=account.strip()
            try:rate=Decimal(str(p.get('rate','1')))
            except InvalidOperation:raise ApiError('invalid_payment_settings')
            if not rate.is_finite() or not 0<rate<=1000000 or (p['currency']=='USD' and rate!=1) or (p['enabled'] and not account):raise ApiError('invalid_payment_settings')
            settings.shamcash_enabled=p['enabled'];settings.shamcash_account=account;settings.shamcash_currency=p['currency'];settings.shamcash_rate=rate;settings.shamcash_instructions=str(p.get('instructions',''))[:500];settings.save()
            audit(request.actor,settings,'Sham Cash destination, currency and conversion settings updated')
    from .nowpayments import configured
    return JsonResponse({'nowpayments_configured':configured(),'enabled':settings.shamcash_enabled,'account':settings.shamcash_account,'currency':settings.shamcash_currency,'rate':str(settings.shamcash_rate),'instructions':settings.shamcash_instructions})


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
                            raise ApiError('payment_verification_required')
                        Wallet.objects.get_or_create(user=row.user)
                        wallet = Wallet.objects.select_for_update().get(user=row.user)
                        wallet.balance_cents += row.amount_cents
                        from .promotions import grant
                        grant(row.user,row.bonus_cents,'topup:'+str(row.pk),'Verified funding promotional points')
                        wallet.save(update_fields=['balance_cents'])
                    else:
                        profile, _ = Profile.objects.get_or_create(user=row.user)
                        profile.creator = True
                        profile.save(update_fields=['creator'])
                row.status, row.reviewer, row.review_note = wanted, request.actor, note
                row.save(update_fields=['status', 'reviewer', 'review_note'])
                audit(request.actor, row, f'{wanted}: {note}')
    payments = [{'id': str(r.pk), 'kind': 'topup', 'user_id':r.user_id,'public_id':r.user.profile.public_id, 'username': r.user.username, 'amount_cents': r.amount_cents, 'bonus_cents': r.bonus_cents, 'transaction_hash': r.transaction_hash, 'method': r.method, 'payment_amount': str(r.quote.payment_amount) if r.quote_id else str(r.amount_cents / 100), 'payment_currency': r.quote.payment_currency if r.quote_id else 'USDT', 'late_submission': r.late_submission, 'receipt_url': grant_url(request, r.receipt), 'status': r.status, 'review_note': r.review_note} for r in TopUp.objects.select_related('user', 'receipt').order_by(Case(When(status='pending', then=Value(0)), default=Value(1), output_field=IntegerField()), '-created_at')[:100]]
    sellers = [{'id': str(r.pk), 'kind': 'seller', 'user_id':r.user_id,'public_id':r.user.profile.public_id, 'username': r.user.username, 'description': r.description, 'status': r.status, 'review_note': r.review_note} for r in SellerApplication.objects.select_related('user').order_by(Case(When(status='pending', then=Value(0)), default=Value(1), output_field=IntegerField()), '-id')[:100]]
    from .models import GatewayInvoice
    gateway=[{'id':str(r.quote_id),'kind':'gateway','username':r.quote.user.username,'user_id':r.quote.user_id,'amount_cents':r.quote.amount_cents,'bonus_cents':r.quote.bonus_cents,'status':'approved' if r.credited_at else r.status,'review_note':'تحتاج مراجعة التاجر' if r.review_required else '','provider_id':r.payment_id or r.invoice_id or ''} for r in GatewayInvoice.objects.select_related('quote__user').order_by('-created_at')[:100]]
    return JsonResponse({'items': payments + sellers + gateway})


def message_json(request, row):
    from .models import ChatCharge
    unlocked=not row.deleted_at and (not row.media_price_cents or request.actor.pk==row.sender_id or ChatCharge.objects.filter(message=row,buyer=request.actor,kind='unlock').exists())
    return {'id': row.pk, 'sender_id': row.sender_id, 'recipient_id': row.recipient_id, 'body': 'حُذفت هذه الرسالة' if row.deleted_at else row.body, 'deleted':bool(row.deleted_at), 'reply_to':row.reply_to_id, 'reply_preview':(row.reply_to.body[:100] if row.reply_to and not row.reply_to.deleted_at else '') if row.reply_to_id else '', 'created_at': row.created_at, 'attachment_url': grant_url(request, row.attachment) if row.attachment_id and unlocked else '', 'media_price_cents':row.media_price_cents, 'media_locked':bool(not row.deleted_at and not unlocked), 'attachment_mime': row.attachment.mime if row.attachment_id and not row.deleted_at else '', 'client_key': str(row.client_key) if row.client_key else '', 'delivered_at': row.delivered_at, 'read_at': row.read_at, 'recipient_name': getattr(getattr(row.recipient,'profile',None),'display_name','') or row.recipient.username, 'sender_name': getattr(getattr(row.sender,'profile',None),'display_name','') or row.sender.username, 'call_url': '', 'call_id': str(row.call_id) if row.call_id else '', 'call_mode': row.call.mode if row.call_id else '', 'call_status': row.call.status if row.call_id else ''}


def wheel_settings(config):
    import hashlib,json
    prizes=config.prizes
    valid=isinstance(prizes,list) and 1<=len(prizes)<=20 and all(isinstance(p,dict) and type(p.get('amount_cents')) is int and 0<=p['amount_cents']<=1000000 and type(p.get('weight')) is int and 1<=p['weight']<=1000000 for p in prizes)
    valid=valid and 1<=config.cost_cents<=config.maximum_cost_cents<=10000 and 0<=config.price_step_cents<=10000 and 0<=config.first_reward_cents<=1000000
    total=sum(p['weight'] for p in prizes) if valid else 0
    version=hashlib.sha256(json.dumps([config.cost_cents,prizes,config.daily_limit,config.daily_budget_cents,config.price_step_cents,config.maximum_cost_cents,config.first_reward_cents,config.grand_unlock_spend_cents],sort_keys=True).encode()).hexdigest()
    return valid,total,version


def wheel_offer(config,user):
    from .models import PaidSpin
    from django.db.models import Count
    import hashlib,json
    history=PaidSpin.objects.filter(user=user).aggregate(count=Count('id'),spent=Sum('cost_cents'))
    count,spent=history['count'],history['spent'] or 0
    valid,_,version=wheel_settings(config)
    cost=min(config.maximum_cost_cents,config.cost_cents+count*config.price_step_cents)
    first=count==0 and config.first_reward_cents>0
    grand=spent>=config.grand_unlock_spend_cents
    prizes=([{'amount_cents':config.first_reward_cents,'weight':1}] if first else [p for p in config.prizes if p['amount_cents']<10000 or grand]) if valid else []
    total=sum(p['weight'] for p in prizes)
    quote_version=hashlib.sha256(json.dumps([version,count,spent,cost,prizes],sort_keys=True).encode()).hexdigest()
    start=timezone.now().replace(hour=0,minute=0,second=0,microsecond=0)
    today=PaidSpin.objects.filter(created_at__gte=start)
    remaining=max(0,config.daily_limit-today.filter(user=user).count())
    affordable=bool(total and (today.aggregate(v=Sum('prize_cents'))['v'] or 0)+max(p['amount_cents'] for p in prizes)<=config.daily_budget_cents)
    return {'daily_limit':config.daily_limit,'remaining_daily':remaining,'enabled':bool(config.enabled and valid and total and remaining and affordable), 'cost_cents':cost,'config_version':quote_version,'first_spin_guaranteed':first,'first_reward_cents':config.first_reward_cents,'spin_count':count,'spent_cents':spent,'grand_unlocked':grand,'grand_unlock_spend_cents':config.grand_unlock_spend_cents,'price_step_cents':config.price_step_cents,'maximum_cost_cents':config.maximum_cost_cents,'prizes':[dict(p,chance_percent=format(p['weight']*100/total,'.8f')) for p in prizes] if total else []}


@endpoint(['GET','POST'])
def paid_wheel(request):
    from .models import PaidWheelConfiguration,PaidSpin
    config,_=PaidWheelConfiguration.objects.get_or_create(pk=1)
    if request.method=='GET':return JsonResponse(wheel_offer(config,request.actor))
    if request.payload.get('rules_version')!=2:raise ApiError('update_app_for_wheel_rules',409)
    key=uuid.UUID(text(request.payload,'request_key',36,36))
    with transaction.atomic():
        config=PaidWheelConfiguration.objects.select_for_update().get(pk=1)
        previous=PaidSpin.objects.filter(user=request.actor,request_key=key).first()
        if previous:
            wallet=Wallet.objects.get(user=request.actor)
            return JsonResponse({'prize_cents':previous.prize_cents,'balance_cents':wallet.balance_cents,'replayed':True})
        offer=wheel_offer(config,request.actor)
        if not offer['enabled']:raise ApiError('wheel_unavailable',409)
        start=timezone.now().replace(hour=0,minute=0,second=0,microsecond=0)
        today=PaidSpin.objects.filter(created_at__gte=start)
        if today.filter(user=request.actor).count()>=config.daily_limit:raise ApiError('wheel_daily_limit',409)
        paid=today.aggregate(v=Sum('prize_cents'))['v'] or 0
        if paid+max(p['amount_cents'] for p in offer['prizes'])>config.daily_budget_cents:raise ApiError('wheel_budget_limit',409)
        if request.payload.get('config_version')!=offer['config_version']:raise ApiError('wheel_configuration_changed',409)
        wallet=Wallet.objects.select_for_update().get(user=request.actor)
        cost=offer['cost_cents'];prizes=offer['prizes']
        if wallet.balance_cents-wallet.reserved_cents<cost:raise ApiError('insufficient_funds',409)
        if config.bankroll_cents+cost<max(p['amount_cents'] for p in prizes):raise ApiError('wheel_unavailable',409)
        pick=secrets.randbelow(sum(p['weight'] for p in prizes));prize=0
        for p in prizes:
            if pick<p['weight']:prize=p['amount_cents'];break
            pick-=p['weight']
        wallet.balance_cents+=prize-cost;wallet.save(update_fields=['balance_cents'])
        config.bankroll_cents+=cost-prize;config.save(update_fields=['bankroll_cents'])
        PaidSpin.objects.create(user=request.actor,request_key=key,cost_cents=cost,prize_cents=prize,prizes_snapshot={'prizes':prizes,'config_version':offer['config_version'],'daily_limit':config.daily_limit,'daily_budget_cents':config.daily_budget_cents,'first_guarantee':offer['first_spin_guaranteed'],'spent_before':offer['spent_cents']})
    return JsonResponse({'prize_cents':prize,'balance_cents':wallet.balance_cents,'cost_cents':cost},status=201)


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
            for field in ('price_step_cents','maximum_cost_cents','first_reward_cents','grand_unlock_spend_cents'):
                if field in p:
                    if type(p[field]) is not int or not 0<=p[field]<=1000000:raise ApiError('invalid_wheel_configuration')
                    setattr(config,field,p[field])
            valid, _, _ = wheel_settings(config)
            if not valid or (config.enabled and (config.first_reward_cents <= config.cost_cents or config.bankroll_cents < max(config.first_reward_cents,max(prize['amount_cents'] for prize in config.prizes)))):
                raise ApiError('invalid_wheel_configuration')
            config.save()
            import json
            audit(request.actor, config, json.dumps({'before': previous_settings, 'after': {'enabled': config.enabled, 'cost_cents': config.cost_cents, 'prizes': config.prizes, 'bankroll_cents': config.bankroll_cents}}))
    return JsonResponse({'enabled': config.enabled, 'cost_cents': config.cost_cents, 'prizes': config.prizes, 'bankroll_cents': config.bankroll_cents,'price_step_cents':config.price_step_cents,'maximum_cost_cents':config.maximum_cost_cents,'first_reward_cents':config.first_reward_cents,'grand_unlock_spend_cents':config.grand_unlock_spend_cents})


# Caller holds both account locks and the message lock inside transaction.atomic().
def charge_chat(message,kind,buyer,creator,cents):
    from .models import ChatCharge
    if ChatCharge.objects.filter(message=message,kind=kind,buyer=buyer).exists():return
    from .admin_security import ensure_payments
    ensure_payments()
    for person in sorted([buyer,creator],key=lambda user:user.pk):Wallet.objects.get_or_create(user=person)
    wallets={wallet.user_id:wallet for wallet in Wallet.objects.select_for_update().filter(user_id__in=[buyer.pk,creator.pk]).order_by('user_id')}
    source,target=wallets[buyer.pk],wallets[creator.pk]
    if source.balance_cents-source.reserved_cents<cents:raise ApiError('insufficient_balance',409)
    share=cents*80//100
    source.balance_cents-=cents;target.balance_cents+=share
    source.save(update_fields=['balance_cents']);target.save(update_fields=['balance_cents'])
    ChatCharge.objects.create(message=message,kind=kind,buyer=buyer,creator=creator,amount_cents=cents,creator_cents=share,platform_cents=cents-share)


@endpoint(['POST'])
def unlock_message(request,message_id):
    row=Message.objects.filter(pk=message_id,recipient=request.actor,sender__is_active=True).first()
    if row is None:raise ApiError('message_unavailable',404)
    with transaction.atomic():
        # All transfers lock users in the same order as sending to avoid deadlocks.
        list(User.objects.select_for_update().filter(pk__in=sorted([row.sender_id,row.recipient_id])).order_by('pk'))
        row=Message.objects.select_for_update(of=('self',)).select_related('sender__profile','recipient__profile','attachment','call').get(pk=row.pk)
        if row.deleted_at or not row.sender.is_active or not row.recipient.is_active:raise ApiError('message_unavailable',404)
        if not row.attachment_id or row.media_price_cents<=0:raise ApiError('not_paid_media')
        if type(request.payload.get('price_cents')) is not int or request.payload['price_cents']!=row.media_price_cents:raise ApiError('paid_price_changed',409)
        charge_chat(row,'unlock',request.actor,row.sender,row.media_price_cents)
    return JsonResponse(message_json(request,row))
