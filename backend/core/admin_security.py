"""Per-request native administration policy; never impersonates a customer."""
import base64,hashlib,json
from datetime import timedelta
from cryptography.fernet import Fernet
from django.conf import settings
from django.db import transaction
from django.db.models import Q
from django.utils import timezone
from .models import AdminAccess,AdminEvent,AccountSanction,AdminFeature,AdminApproval

SCOPES=['dashboard.view','users.view','users.edit','users.sanction','users.preview','finance.view','finance.adjust','finance.approve','live.view','live.moderate','store.view','store.edit','wheel.view','wheel.edit','benefits.view','benefits.edit','support.view','support.edit','roles.edit','audit.view','settings.edit','notices.send']
ROLES={
 'owner':SCOPES,
 'general':[s for s in SCOPES if s not in ['roles.edit','finance.adjust','finance.approve']],
 'finance':['dashboard.view','users.view','finance.view','finance.adjust','finance.approve','audit.view'],
 'users':['dashboard.view','users.view','users.edit','users.sanction','users.preview'],
 'live':['dashboard.view','live.view','live.moderate'],
 'store':['dashboard.view','store.view','store.edit'],
 'support':['dashboard.view','users.view','users.preview','support.view','support.edit']}

def scopes(user):
    if not user.is_staff:return []
    if user.is_superuser:return SCOPES
    a=AdminAccess.objects.filter(user=user).first()
    if a:return list(dict.fromkeys(([p for p in a.permissions if p in SCOPES] if a.customized else ROLES.get(a.role,[]))))
    return ROLES['live'] if user.has_perm('core.moderate_livebroadcast') else []

def event(actor,action,target='',reason='',result='success'):
    return AdminEvent.objects.create(actor=actor,action=action,target=str(target)[:120],reason=reason[:500],result=result)

def cipher():return Fernet(base64.urlsafe_b64encode(hashlib.sha256((settings.SECRET_KEY+':admin-totp').encode()).digest()))

def require(request,scope,fresh=False):
    from .views import ApiError
    if scope not in scopes(request.actor):
        event(request.actor,'access_denied',request.path,scope,'denied');raise ApiError('administrator_required',403)
    request.actor._admin_reason=str(request.payload.get('reason') or request.payload.get('review_note') or '')[:500]
    verified=request.api_token.admin_verified_at
    if not verified or timezone.now()-verified>timedelta(minutes=10 if fresh else 60):raise ApiError('admin_mfa_required',403)
    if request.method!='GET' and request.path.startswith('/api/admin/control'):
        from .views import text
        text(request.payload,'reason',3,500)

def route_scope(request):
    path=request.path;write=request.method!='GET'
    if '/accounts' in path:
        action=request.payload.get('action','')
        if action=='credit':return 'finance.adjust'
        if action.startswith('moderator_'):return 'roles.edit'
        return 'users.edit' if write else 'users.view'
    if path.endswith('/live'):return 'live.moderate' if write else 'live.view'
    if path.endswith('/wheel'):return 'wheel.edit' if write else 'wheel.view'
    if path.endswith('/moderation') or path.endswith('/bookings'):return 'store.edit' if write else 'store.view'
    if path.endswith('/requests') and request.payload.get('kind')=='seller':return 'users.edit' if write else 'users.view'
    return 'finance.approve' if write else 'finance.view'

def blocked(user,kind):return AccountSanction.objects.filter(user=user,kind__in=['ban',kind],revoked_at__isnull=True).filter(Q(expires_at__isnull=True)|Q(expires_at__gt=timezone.now())).exists()

def check_operation(request):
    from .views import ApiError
    p=request.path;kind=None;feature=None
    if p.startswith('/api/admin/') or p in ['/api/login','/api/register','/api/logout']:return
    if blocked(request.actor,'ban'):raise ApiError('account_restricted',403)
    if request.method in ['POST','PATCH']:
        if p.startswith('/api/messages/'):kind='messages';feature='messages'
        elif p.startswith('/api/calls'):kind='calls';feature='calls'
        elif p=='/api/live':kind='live';feature='live'
        elif p.startswith('/api/bookings') or p=='/api/services':kind='store';feature='store'
        elif p=='/api/withdrawals':kind='withdrawals';feature='withdrawals'
        elif p.startswith(('/api/topup','/api/nowpayments/invoices','/api/promotion/wheel')):feature='payments'
        if kind and blocked(request.actor,kind):raise ApiError('feature_restricted',403)
        if feature and AdminFeature.objects.filter(key=feature,enabled=False).exists():raise ApiError('feature_maintenance',503)

def digest(payload):return hashlib.sha256(json.dumps({k:v for k,v in payload.items() if k!='approval_id'},sort_keys=True,separators=(',',':')).encode()).hexdigest()

def financial_approval(request,target_user=None):
    """Single-use approval bound to exact endpoint/body and independent reviewer."""
    from .views import ApiError
    if not request.payload.get('approval_id'):raise ApiError('independent_approval_required',409)
    a=AdminApproval.objects.select_for_update().filter(pk=request.payload.get('approval_id'),requester=request.actor,path=request.path,payload_digest=digest(request.payload),status='approved',expires_at__gt=timezone.now()).first()
    if not a or a.reviewer_id in (request.actor.pk,target_user) or a.target_user_id!=target_user:raise ApiError('independent_approval_required',409)
    if a.used_at:return
    a.used_at=timezone.now();a.save(update_fields=['used_at'])
    event(request.actor,'financial_approval_used',a.pk,a.reason)

def financial_target(request):
    from .models import Withdrawal,Booking,TopUp
    p=request.path;action=request.payload.get('action')
    if '/accounts/' in p and action=='credit':
        from .views import amount,text
        import uuid
        amount(request.payload,'amount_cents');text(request.payload,'reason',3,300);uuid.UUID(str(request.payload.get('request_key')))
        return int(p.rsplit('/',1)[-1])
    if p.endswith('/payouts') and action=='paid' and request.payload.get('paid_verified') is True:
        from .views import ApiError
        if AdminFeature.objects.filter(key='withdrawals',enabled=False).exists():raise ApiError('feature_maintenance',503)
        row=Withdrawal.objects.filter(pk=request.payload.get('id')).first()
        if row and blocked(row.user,'withdrawals'):raise ApiError('feature_restricted',403)
        return row.user_id if row and row.status=='pending' else None
    if p.endswith('/bookings') and action=='refund':
        row=Booking.objects.filter(pk=request.payload.get('id')).first();return row.buyer_id if row and row.operation_id and row.operation.status=='held' else None
    if p.endswith('/requests') and request.payload.get('kind')=='topup' and request.payload.get('decision')=='approve' and request.payload.get('payment_verified') is True:
        row=TopUp.objects.filter(pk=request.payload.get('id')).first();return row.user_id if row and row.status=='pending' else None
    return None

def approval_required(request,target):
    from .models import BalanceAdjustment
    if request.payload.get('action')=='credit' and BalanceAdjustment.objects.filter(administrator=request.actor,request_key=request.payload.get('request_key')).exists():return False
    return target is not None

def prepare_approval(request,target):
    from django.http import JsonResponse
    if request.payload.get('approval_id'):return None
    key=digest(request.payload)
    # Persist the proposal before returning 409; no monetary mutation has occurred.
    a=AdminApproval.objects.filter(requester=request.actor,path=request.path,payload_digest=key,status__in=['pending','approved'],used_at__isnull=True,expires_at__gt=timezone.now()).first()
    if not a:a=AdminApproval.objects.create(requester=request.actor,path=request.path,payload_digest=key,payload=request.payload,target_user_id=target,reason=str(request.payload.get('reason') or request.payload.get('review_note') or 'Financial action requires independent review')[:500],expires_at=timezone.now()+timedelta(hours=1))
    event(request.actor,'approval_requested',a.pk,a.reason)
    return JsonResponse({'error':'independent_approval_required','approval_id':str(a.pk)},status=409)
