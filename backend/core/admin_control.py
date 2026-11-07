"""Native control centre: scoped metadata, explicit actions and MFA."""
import hashlib,json
from datetime import timedelta
import pyotp
from django.db import transaction
from django.db.models import Q,Sum
from django.http import JsonResponse
from django.utils import timezone
from django.contrib.auth import get_user_model
from .views import endpoint,ApiError,text
from .models import *
from .admin_security import require,scopes,ROLES,SCOPES,cipher,event,digest,blocked
User=get_user_model()

def rows(q,fields):return list(q.values(*fields)[:100])
def reason(request):return text(request.payload,'reason',3,500)

def verify_code(request,access):
    now=timezone.now()
    if access.locked_until and access.locked_until>now:raise ApiError('too_many_attempts',429)
    code=request.payload.get('code','');secret=cipher().decrypt(access.totp_secret.encode()).decode() if access.totp_secret else ''
    counter=int(now.timestamp())//30
    matched=next((n for n in range(counter-1,counter+2) if n>access.last_counter and secret and pyotp.TOTP(secret).verify(code,for_time=n*30)),None)
    if matched is None:
        access.failed_attempts+=1
        if access.failed_attempts>=5:access.locked_until=now+timedelta(minutes=15)
        access.save(update_fields=['failed_attempts','locked_until']);event(request.actor,'mfa_failed',request.actor.pk,'Invalid/reused code','denied');return False
    access.last_counter=matched;access.failed_attempts=0;access.locked_until=None;access.totp_enabled=True;access.save()
    request.api_token.admin_verified_at=now;request.api_token.save(update_fields=['admin_verified_at']);event(request.actor,'mfa_verified',request.actor.pk,'TOTP verified');return True

@endpoint(['GET','POST'])
def authentication(request):
    if not scopes(request.actor):raise ApiError('administrator_required',403)
    a,_=AdminAccess.objects.get_or_create(user=request.actor,defaults={'role':'owner' if request.actor.is_superuser else 'live'})
    if request.method=='GET':return JsonResponse({'enabled':a.totp_enabled,'verified':bool(request.api_token.admin_verified_at and timezone.now()-request.api_token.admin_verified_at<timedelta(minutes=10)),'permissions':scopes(request.actor)})
    action=request.payload.get('action')
    if action=='setup':
        if a.locked_until and a.locked_until>timezone.now():raise ApiError('too_many_attempts',429)
        if a.totp_enabled:raise ApiError('mfa_already_enabled',409)
        if not request.actor.check_password(request.payload.get('password','')):
            with transaction.atomic():
                a=AdminAccess.objects.select_for_update().get(pk=a.pk);a.failed_attempts+=1
                if a.failed_attempts>=5:a.locked_until=timezone.now()+timedelta(minutes=15)
                a.save(update_fields=['failed_attempts','locked_until'])
            event(request.actor,'mfa_setup_denied',request.actor.pk,'Password verification failed','denied');raise ApiError('invalid_credentials',403)
        with transaction.atomic():
            a=AdminAccess.objects.select_for_update().get(pk=a.pk)
            if a.totp_enabled:raise ApiError('mfa_already_enabled',409)
            secret=pyotp.random_base32();a.totp_secret=cipher().encrypt(secret.encode()).decode();a.last_counter=-1;a.save(update_fields=['totp_secret','last_counter']);event(request.actor,'mfa_setup',request.actor.pk,'Enrollment started')
        return JsonResponse({'secret':secret,'uri':pyotp.TOTP(secret).provisioning_uri(request.actor.username,issuer_name='Relaxia')})
    if action=='verify':
        with transaction.atomic():
            a=AdminAccess.objects.select_for_update().get(pk=a.pk);valid=verify_code(request,a)
        if not valid:raise ApiError('invalid_totp',403)
        return JsonResponse({'verified':True})
    raise ApiError('invalid_action')

@endpoint(['GET'])
def dashboard(request):
    require(request,'dashboard.view')
    period=request.GET.get('period','day');days={'day':1,'week':7,'month':30}.get(period)
    if days is None:raise ApiError('invalid_period')
    from .calling import expire_calls
    expire_calls()
    start=timezone.now()-timedelta(days=days);now=timezone.now()
    d={'users':User.objects.count(),'creators':Profile.objects.filter(creator=True).count(),'active_users':Profile.objects.filter(last_active_at__gte=start).count(),'online':Profile.objects.filter(last_active_at__gte=now-timedelta(seconds=25)).count(),'live':LiveBroadcast.objects.filter(status__in=['starting','active','reconnecting']).count(),'calls':InternalCall.objects.filter(status__in=['ringing','accepted']).count(),'bookings':Booking.objects.filter(created_at__gte=start).count(),'pending_reports':Report.objects.filter(resolved=False).count()+LiveReport.objects.filter(resolved=False).count(),'banned':User.objects.filter(is_active=False).count(),'restricted':AccountSanction.objects.filter(revoked_at__isnull=True).filter(Q(expires_at__isnull=True)|Q(expires_at__gt=now)).values('user').distinct().count(),'pending_cases':AdminCase.objects.filter(status='pending').count(),'security_alerts':AdminEvent.objects.filter(result='denied',created_at__gte=start).count(),'failed_live':LiveBroadcast.objects.filter(status='failed',created_at__gte=start).count()}
    if 'finance.view' in scopes(request.actor):
        ops=FinancialOperation.objects.filter(status='settled',settled_at__gte=start);chat=ChatCharge.objects.filter(created_at__gte=start)
        d.update(revenue_cents=(ops.aggregate(v=Sum('amount_cents'))['v'] or 0)+(chat.aggregate(v=Sum('amount_cents'))['v'] or 0),commission_cents=(ops.aggregate(v=Sum('platform_cents'))['v'] or 0)+(chat.aggregate(v=Sum('platform_cents'))['v'] or 0),pending_withdrawals=Withdrawal.objects.filter(status='pending').count())
    return JsonResponse({'stats':d,'period':period,'updated_at':now,'permissions':scopes(request.actor)})

@endpoint(['GET','POST'])
def roles(request):
    require(request,'roles.edit',request.method=='POST')
    if request.method=='POST':
        if not request.actor.is_superuser:raise ApiError('owner_required',403)
        user=User.objects.get(pk=request.payload.get('user_id'))
        if user.is_superuser or user==request.actor:raise ApiError('protected_account',403)
        role=request.payload.get('role');permissions=request.payload.get('permissions',[])
        if role not in ROLES or role=='owner' or not isinstance(permissions,list) or any(p not in SCOPES for p in permissions):raise ApiError('invalid_role')
        with transaction.atomic():
            User.objects.select_for_update().get(pk=user.pk)
            AdminAccess.objects.update_or_create(user=user,defaults={'role':role,'permissions':permissions,'customized':True});user.is_staff=True;user.save(update_fields=['is_staff']);SessionToken.objects.filter(user=user).delete();event(request.actor,'role_changed',user.pk,reason(request),details={'role':role,'permissions':permissions})
    return JsonResponse({'roles':ROLES,'permissions':SCOPES,'items':[{'id':u.pk,'username':u.username,'role':'owner' if u.is_superuser else getattr(getattr(u,'adminaccess',None),'role','live'),'permissions':scopes(u)} for u in User.objects.filter(is_staff=True)[:100]]})

@endpoint(['POST'])
def preview(request,user_id):
    require(request,'users.preview');why=reason(request);u=User.objects.filter(pk=user_id).first()
    if not u:raise ApiError('account_unavailable',404)
    p=Profile.objects.get(user=u);event(request.actor,'account_preview',u.pk,why)
    d={'read_only':True,'banner':'وضع معاينة إدارية','profile':{'username':u.username,'display_name':p.display_name,'avatar_url':p.avatar_url,'bio':p.bio,'creator':p.creator,'identity_verified':p.identity_verified,'frame':p.selected_frame,'broadcast_allowed':p.broadcast_allowed,'created_at':u.date_joined},'services':rows(Service.objects.filter(owner=u),['id','title','active','approved','price_cents','duration_minutes']),'bookings':rows(Booking.objects.filter(Q(buyer=u)|Q(service__owner=u)).order_by('-created_at'),['id','status','price_cents','created_at']),'benefits':rows(BenefitGrant.objects.filter(user=u,expires_at__gt=timezone.now()),['kind','value','expires_at']),'communication':{'sent':Message.objects.filter(sender=u).count(),'received':Message.objects.filter(recipient=u).count(),'delivered':Message.objects.filter(sender=u,delivered_at__isnull=False).count(),'read':Message.objects.filter(sender=u,read_at__isnull=False).count(),'calls':rows(InternalCall.objects.filter(Q(caller=u)|Q(callee=u)).order_by('-created_at'),['id','mode','status','created_at'])}}
    if 'finance.view' in scopes(request.actor):
        w=Wallet.objects.filter(user=u).first();d['wallet']={'balance_cents':w.balance_cents if w else 0,'reserved_cents':w.reserved_cents if w else 0};d['operations']=rows(FinancialOperation.objects.filter(Q(buyer=u)|Q(creator=u)).order_by('-created_at'),['id','kind','amount_cents','status','created_at'])
    return JsonResponse(d)

@endpoint(['GET','POST'])
def sanctions(request):
    require(request,'users.sanction',request.method=='POST')
    if request.method=='POST':
        why=reason(request)
        if request.payload.get('action')=='revoke':
            s=AccountSanction.objects.get(pk=request.payload.get('id'));s.revoked_at=timezone.now();s.revoked_by=request.actor;s.save(update_fields=['revoked_at','revoked_by']);event(request.actor,'sanction_revoked',s.user_id,why)
        else:
            u=User.objects.get(pk=request.payload.get('user_id'));kind=request.payload.get('kind');hours=request.payload.get('hours',0)
            if u.is_superuser or u==request.actor:raise ApiError('protected_account',403)
            if kind not in ['warning','ban','messages','calls','live','store','withdrawals'] or type(hours) is not int or not 0<=hours<=8760:raise ApiError('invalid_sanction')
            s=AccountSanction.objects.create(user=u,actor=request.actor,kind=kind,reason=why,expires_at=timezone.now()+timedelta(hours=hours) if hours else None);event(request.actor,'sanction_'+kind,u.pk,why)
            if kind in ['ban','live']:
                from .live import close
                for room in LiveBroadcast.objects.filter(owner=u,status__in=['starting','active','reconnecting']):close(room,'Administrative restriction',True)
            if kind in ['ban','calls']:
                # Existing call transport observes terminal status and tears down on poll.
                InternalCall.objects.filter(Q(caller=u)|Q(callee=u),status__in=['ringing','accepted']).update(status='ended',ended_at=timezone.now())
    q=AccountSanction.objects.select_related('user','actor').order_by('-id')
    if request.GET.get('q'):q=q.filter(Q(user__username__icontains=request.GET['q'])|Q(reason__icontains=request.GET['q']))
    return JsonResponse({'items':[{'id':s.pk,'username':s.user.username,'user_id':s.user_id,'kind':s.kind,'reason':s.reason,'actor':s.actor.username,'created_at':s.created_at,'expires_at':s.expires_at,'revoked_at':s.revoked_at} for s in q[:100]]})

@endpoint(['GET','POST'])
def cases(request):
    require(request,'support.edit' if request.method=='POST' else 'support.view',request.method=='POST')
    if request.method=='POST':
        if request.payload.get('action')=='resolve_report':
            r=Report.objects.get(pk=request.payload.get('id'));r.resolved=True;r.save(update_fields=['resolved']);event(request.actor,'report_resolved',r.pk,reason(request));return JsonResponse({'ok':True})
        c=AdminCase.objects.get(pk=request.payload.get('id'));c.response=text(request.payload,'response',3,1000);c.status='resolved';c.save(update_fields=['response','status']);event(request.actor,'case_resolved',c.pk,reason(request))
    return JsonResponse({'items':rows(AdminCase.objects.order_by('-id'),['id','user_id','kind','body','status','response','created_at']),'reports':rows(Report.objects.filter(resolved=False).order_by('-id'),['id','reporter_id','target_type','target_id','reason','created_at'])})

@endpoint(['GET','POST'])
def my_cases(request):
    if request.method=='POST':AdminCase.objects.create(user=request.actor,kind='appeal',body=text(request.payload,'body',3,2000))
    return JsonResponse({'items':rows(AdminCase.objects.filter(user=request.actor).order_by('-id'),['id','body','response','status','created_at'])})

@endpoint(['GET'])
def audit_log(request):
    require(request,'audit.view');q=AdminEvent.objects.select_related('actor').order_by('-id');term=request.GET.get('q','')[:100]
    if term:q=q.filter(Q(action__icontains=term)|Q(reason__icontains=term)|Q(actor__username__icontains=term)|Q(target__icontains=term))
    return JsonResponse({'items':[{'id':e.pk,'actor':e.actor.username,'action':e.action,'target':e.target,'reason':e.reason,'result':e.result,'details':e.details,'created_at':e.created_at} for e in q[:100]]})

@endpoint(['GET','POST'])
def approvals(request):
    required='finance.approve' if request.method=='POST' or 'finance.approve' in scopes(request.actor) else 'finance.adjust'
    require(request,required,request.method=='POST')
    if request.method=='POST':
        why=reason(request);action=request.payload.get('action')
        with transaction.atomic():
            a=AdminApproval.objects.select_for_update().get(pk=request.payload.get('id'))
            if a.requester_id==request.actor.pk or a.target_user_id==request.actor.pk:raise ApiError('independent_reviewer_required',403)
            if a.status!='pending' or a.expires_at<=timezone.now():raise ApiError('approval_unavailable',409)
            if action not in ['approve','reject']:raise ApiError('invalid_action')
            a.status='approved' if action=='approve' else 'rejected';a.reviewer=request.actor;a.save(update_fields=['status','reviewer']);event(request.actor,'approval_'+action,a.pk,why)
    return JsonResponse({'items':rows(AdminApproval.objects.order_by('-created_at'),['id','requester_id','reviewer_id','path','payload','target_user_id','reason','status','expires_at','used_at'])})

@endpoint(['GET','POST'])
def settings_control(request):
    require(request,'settings.edit',request.method=='POST')
    keys=['payments','withdrawals','live','calls','messages','store']
    if request.method=='POST':
        key=request.payload.get('key');value=request.payload.get('enabled')
        if key not in keys or type(value) is not bool:raise ApiError('invalid_feature')
        AdminFeature.objects.update_or_create(key=key,defaults={'enabled':value,'reason':reason(request)});event(request.actor,'feature_changed',key,reason(request))
        if key=='live' and not value:
            from .live import close
            for room in LiveBroadcast.objects.filter(status__in=['starting','active','reconnecting']):close(room,'Administrative emergency stop',True)
    values={s.key:s for s in AdminFeature.objects.all()}
    return JsonResponse({'items':[{'key':k,'enabled':values[k].enabled if k in values else True,'reason':values[k].reason if k in values else ''} for k in keys]})

@endpoint(['GET','POST'])
def notices(request):
    require(request,'notices.send',request.method=='POST')
    if request.method=='POST':
        recipient=User.objects.filter(pk=request.payload.get('user_id')).first() if request.payload.get('user_id') else None
        if request.payload.get('user_id') and not recipient:raise ApiError('account_unavailable',404)
        n=AdminNotice.objects.create(actor=request.actor,recipient=recipient,title=text(request.payload,'title',2,120),body=text(request.payload,'body',3,1000));event(request.actor,'notice_sent',n.pk,reason(request))
    return JsonResponse({'items':rows(AdminNotice.objects.order_by('-id'),['id','recipient_id','title','body','created_at'])})

@endpoint(['GET'])
def inbox(request):return JsonResponse({'items':rows(AdminNotice.objects.filter(Q(recipient=request.actor)|Q(recipient__isnull=True)).order_by('-id'),['id','title','body','created_at']),'sanctions':rows(AccountSanction.objects.filter(user=request.actor).order_by('-id'),['id','kind','reason','expires_at','revoked_at'])})

@endpoint(['GET','POST'])
def benefits(request):
    require(request,'benefits.edit' if request.method=='POST' else 'benefits.view',request.method=='POST')
    if request.method=='POST':
        u=User.objects.get(pk=request.payload.get('user_id'));kind=request.payload.get('kind');value=request.payload.get('value');days=request.payload.get('days',30);why=reason(request)
        if kind not in ['frame','promotion'] or type(value) is not int or type(days) is not int or not 1<=days<=365:raise ApiError('invalid_benefit')
        with transaction.atomic():
            User.objects.select_for_update().get(pk=request.actor.pk)
            if kind=='frame':
                if not 0<=value<=10:raise ApiError('invalid_frame')
                BenefitGrant.objects.create(user=u,actor=request.actor,kind='frame',value=value,reason=why,expires_at=timezone.now()+timedelta(days=days))
            else:
                from .promotions import grant
                today=timezone.now()-timedelta(days=1)
                total=PromotionalGrant.objects.filter(actor=request.actor,created_at__gte=today).aggregate(v=Sum('points'))['v'] or 0
                import uuid
                key=str(uuid.UUID(text(request.payload,'request_key',36,36)))
                existing=PromotionalGrant.objects.filter(reference='admin:'+str(request.actor.pk)+':'+key).exists()
                if not 1<=value<=10000 or (not existing and total+value>10000):raise ApiError('promotion_limit',409)
                grant(u,value,'admin:'+str(request.actor.pk)+':'+key,why,request.actor)
            event(request.actor,'benefit_granted',u.pk,why)
    return JsonResponse({'items':rows(BenefitGrant.objects.order_by('-id'),['id','user_id','actor_id','kind','value','reason','expires_at']),'promotions':rows(PromotionalGrant.objects.order_by('-id'),['id','user_id','actor_id','points','reason','created_at'])})

@endpoint(['GET','POST'])
def wheel_limits(request):
    require(request,'wheel.edit' if request.method=='POST' else 'wheel.view',request.method=='POST')
    c,_=PaidWheelConfiguration.objects.get_or_create(pk=1)
    if request.method=='POST':
        maximum=request.payload.get('daily_limit');budget=request.payload.get('daily_budget_cents')
        if type(maximum) is not int or not 0<=maximum<=100 or type(budget) is not int or not 0<=budget<=1000000:raise ApiError('invalid_wheel_limit')
        with transaction.atomic():
            c=PaidWheelConfiguration.objects.select_for_update().get(pk=1);before={'daily_limit':c.daily_limit,'daily_budget_cents':c.daily_budget_cents};c.daily_limit=maximum;c.daily_budget_cents=budget;c.save(update_fields=['daily_limit','daily_budget_cents']);event(request.actor,'wheel_limits',c.pk,reason(request)+' '+json.dumps({'before':before,'after':{'daily_limit':maximum,'daily_budget_cents':budget}}))
    start=timezone.now().replace(hour=0,minute=0,second=0,microsecond=0);q=PaidSpin.objects.filter(created_at__gte=start)
    return JsonResponse({'daily_limit':c.daily_limit,'daily_budget_cents':c.daily_budget_cents,'spins':q.count(),'cost_cents':q.aggregate(v=Sum('cost_cents'))['v'] or 0,'prizes_cents':q.aggregate(v=Sum('prize_cents'))['v'] or 0,'items':rows(PaidSpin.objects.order_by('-id'),['id','user_id','cost_cents','prize_cents','prizes_snapshot','created_at'])})

@endpoint(['GET'])
def communications(request):
    require(request,'support.view')
    from .calling import expire_calls
    expire_calls()
    return JsonResponse({'items':rows(InternalCall.objects.order_by('-created_at'),['id','caller_id','callee_id','mode','status','created_at','accepted_at','ended_at','caller_seen_at','callee_seen_at']),'message_states':{'pending_delivery':Message.objects.filter(delivered_at__isnull=True,deleted_at__isnull=True).count(),'delivered':Message.objects.filter(delivered_at__isnull=False).count(),'read':Message.objects.filter(read_at__isnull=False).count()},'content_access':False})
