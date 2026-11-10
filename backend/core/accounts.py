from django.contrib.auth import get_user_model
from django.contrib.auth.tokens import default_token_generator
from django.contrib.auth.password_validation import validate_password
from django.core.mail import send_mail
from django.conf import settings
from django.db import transaction
from django.db.models import Q
from django.http import JsonResponse
from django.utils.encoding import force_bytes,force_str
from django.utils.http import urlsafe_base64_encode,urlsafe_base64_decode
from .views import endpoint,ApiError,password_value,text
from .models import SessionToken,Wallet,PushDevice,Service,Profile,Booking,InternalCall,LiveBroadcast

@endpoint(['POST'],authenticated=False)
def reset_request(request):
    from .identity import normalize_email,email_matches
    email=normalize_email(request.payload.get('email'))
    if not settings.EMAIL_HOST:raise ApiError('email_not_configured',503)
    users=list(email_matches(email).filter(is_active=True)[:2])
    if len(users)==1:
        user=users[0];code=urlsafe_base64_encode(force_bytes(user.pk))+'.'+default_token_generator.make_token(user)
        # User enters the one-time code in the app; no untrusted redirect URL.
        try:send_mail('استعادة حساب Relaxia','أدخل هذا الرمز في التطبيق لتعيين كلمة مرور جديدة. صالح لمدة 30 دقيقة:\n\n'+code+'\n\nإذا لم تطلب التغيير فتجاهل الرسالة.',settings.DEFAULT_FROM_EMAIL,[user.email],fail_silently=False)
        except Exception:pass # Do not reveal account existence through SMTP failures.
    return JsonResponse({'ok':True,'message':'إذا كان البريد مسجلاً فستصلك رسالة الاستعادة.'})

@endpoint(['POST'],authenticated=False)
def reset_confirm(request):
    try:
        uid,token=text(request.payload,'code',10,300).split('.',1)
        user=get_user_model().objects.get(pk=force_str(urlsafe_base64_decode(uid)),is_active=True)
    except Exception:raise ApiError('invalid_reset_code')
    with transaction.atomic():
        user=get_user_model().objects.select_for_update().get(pk=user.pk)
        if not default_token_generator.check_token(user,token):raise ApiError('invalid_reset_code')
        password=password_value(request.payload,8);validate_password(password,user)
        user.set_password(password);user.save(update_fields=['password'])
        SessionToken.objects.filter(user=user).delete();PushDevice.objects.filter(user=user).update(enabled=False)
    return JsonResponse({'ok':True})

@endpoint(['POST'])
def security(request):
    action=request.payload.get('action')
    with transaction.atomic():
        user=get_user_model().objects.select_for_update().get(pk=request.actor.pk)
        if not user.check_password(text(request.payload,'current_password',1,128)):raise ApiError('invalid_credentials',403)
        if action=='password':
            password=password_value(request.payload,8);validate_password(password,user)
            user.set_password(password);user.save(update_fields=['password'])
        elif action=='delete':
            if user.is_staff:raise ApiError('admin_account_protected',409)
            wallet=Wallet.objects.select_for_update().get(user=user)
            if wallet.balance_cents or wallet.reserved_cents:raise ApiError('account_has_balance',409)
            if Booking.objects.filter(Q(buyer=user)|Q(service__owner=user),status__in=['paid_reserved','accepted']).exists():raise ApiError('account_has_bookings',409)
            if LiveBroadcast.objects.filter(owner=user,status__in=['starting','active','reconnecting']).exists():raise ApiError('end_live_first',409)
            user.is_active=False;user.email='';user.set_unusable_password();user.save(update_fields=['is_active','email','password'])
            Profile.objects.filter(user=user).update(display_name='حساب محذوف',bio='',avatar_url='',last_active_at=None)
            Service.objects.filter(owner=user).update(active=False)
            # Retain financial/moderation records without exposing the public profile.
        else:raise ApiError('invalid_action')
        SessionToken.objects.filter(user=user).delete();PushDevice.objects.filter(user=user).update(enabled=False)
        from django.utils import timezone
        InternalCall.objects.filter(Q(caller=user)|Q(callee=user),status__in=['ringing','accepted']).update(status='ended',ended_at=timezone.now())
    return JsonResponse({'ok':True})
