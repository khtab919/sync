"""Ephemeral SFU broadcasting. No egress/recording/storage APIs are used."""
import hashlib,json,os,secrets,uuid,time,urllib.request,urllib.error
from datetime import timedelta
from urllib.parse import urlsplit,parse_qs
import jwt
from django.core import signing
from django.db import transaction
from django.db.models import Q
from django.http import JsonResponse,HttpResponse
from django.shortcuts import render
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from .views import endpoint,ApiError,text,amount,User
from .models import Profile,SessionToken,LiveBroadcast,LiveAdmission,LiveComment,LiveLike,LiveReport,LiveRoomTicket,FinancialOperation,UserBlock
from . import finance
from .mobile_admin import require_admin,audit
LIVE=['starting','active','reconnecting']

def moderator(user):return user.is_staff and (user.is_superuser or user.has_perm('core.moderate_livebroadcast'))
def require_moderator(request):
    if not moderator(request.actor):raise ApiError('administrator_required',403)

def enabled():return os.environ.get('LAYALI_LIVE_ENABLED')=='1' and bool(os.environ.get('LIVEKIT_API_KEY') and os.environ.get('LIVEKIT_API_SECRET'))
def room_name(row):return 'live-'+str(row.pk)
def jwt_token(grants,identity=None,metadata=None,ttl=30):
    now=int(time.time());p={'iss':os.environ['LIVEKIT_API_KEY'],'iat':now,'nbf':now-5,'exp':now+ttl,'video':grants}
    if identity:p['sub']=identity
    if metadata:p['metadata']=json.dumps(metadata,separators=(',',':'))
    return jwt.encode(p,os.environ['LIVEKIT_API_SECRET'],algorithm='HS256')
def provider(method,payload,room=None):
    grants={'roomList':True} if method=='ListRooms' else {'roomCreate':True} if method in ('CreateRoom','DeleteRoom') else {'roomAdmin':True,'room':room}
    token=jwt_token(grants,ttl=30)
    url=os.environ.get('LIVEKIT_INTERNAL_URL','http://livekit:7880').rstrip('/')+'/twirp/livekit.RoomService/'+method
    try:
        req=urllib.request.Request(url,data=json.dumps(payload).encode(),headers={'Authorization':'Bearer '+token,'Content-Type':'application/json'})
        with urllib.request.urlopen(req,timeout=6) as response:return json.loads(response.read(2*1024*1024))
    except urllib.error.HTTPError as exc:
        try:code=json.loads(exc.read(4096)).get('code')
        except Exception:code=None
        if code=='not_found' and method in ('DeleteRoom','RemoveParticipant'):return {}
        if code=='not_found' and method=='ListParticipants':return {'participants':[]}
        raise ApiError('live_server_unavailable',503) from exc
    except Exception as exc:raise ApiError('live_server_unavailable',503) from exc

def eligible(user,host=False):
    p=Profile.objects.filter(user=user).first()
    if not p or not p.policy_accepted_at:raise ApiError('adult_confirmation_required',403)
    if p.date_of_birth:
        today=timezone.now().date();age=today.year-p.date_of_birth.year-((today.month,today.day)<(p.date_of_birth.month,p.date_of_birth.day))
        if age<18:raise ApiError('adult_confirmation_required',403)
    if not user.is_active or (host and (not p.creator or not p.broadcast_allowed)):raise ApiError('broadcast_permission_required',403)
    return p

def find(user,pk,owner=False):
    row=LiveBroadcast.objects.select_related('owner__profile').filter(pk=pk).first()
    if not row or (owner and row.owner_id!=user.pk):raise ApiError('broadcast_unavailable',404)
    return row

def authorized(row,user):
    eligible(user,host=user.pk==row.owner_id)
    if row.status not in LIVE or not row.owner.is_active:raise ApiError('broadcast_ended',409)
    if user.pk!=row.owner_id and not moderator(user) and UserBlock.objects.filter(Q(user=row.owner,blocked=user)|Q(user=user,blocked=row.owner)).exists():raise ApiError('broadcast_forbidden',403)
    if row.invite_only and not moderator(user) and user.pk!=row.owner_id and not row.invited.filter(pk=user.pk).exists():raise ApiError('invitation_required',403)
    if LiveAdmission.objects.filter(broadcast=row,user=user,banned=True).exists():raise ApiError('broadcast_forbidden',403)

def describe(row,user):
    admission=LiveAdmission.objects.select_related('operation').filter(broadcast=row,user=user).first()
    return {'id':row.pk,'owner_id':row.owner_id,'owner_name':row.owner.profile.display_name or row.owner.username,'avatar_url':row.owner.profile.avatar_url,'title':row.title,'private':row.private,'invite_only':row.invite_only,'price_cents':row.price_cents,'max_viewers':row.max_viewers,'viewers':row.viewers,'minimum_seconds':row.minimum_seconds,'status':row.status,'gifts_enabled':row.gifts_enabled,'likes':LiveLike.objects.filter(broadcast=row).count(),'following':__import__('core.models',fromlist=['Follow']).Follow.objects.filter(user=user,creator=row.owner).exists(),'owner':row.owner_id==user.pk,'admitted':bool(admission and not admission.banned),'payment_status':admission.operation.status if admission and admission.operation_id else 'free','refund_policy':f'يُحجز السعر عند التأكيد. تستحق الخدمة بعد {row.minimum_seconds} ثانية من إتاحة الوسائط أثناء اتصالك. يُعاد الحجز إذا انتهى البث قبل ذلك. لا يتكرر الدفع لإعادة الدخول في البث نفسه.'}

def financial_locks(row):
    ids=list(LiveAdmission.objects.filter(broadcast=row).values_list('user_id',flat=True))+[row.owner_id]
    list(User.objects.select_for_update().filter(pk__in=sorted(set(ids))).order_by('pk'))

def close(row,reason='ended',failed=False):
    # Provider deletion succeeds before financial/state finalization; a retry is safe.
    with transaction.atomic():
        financial_locks(row)
        locked=LiveBroadcast.objects.select_for_update().get(pk=row.pk)
        if locked.status not in LIVE:return
        locked.status='failed' if failed else 'ended';locked.ended_at=timezone.now();locked.viewers=0;locked.end_reason=reason[:200];locked.cleanup_pending=True;locked.save()
        for a in LiveAdmission.objects.filter(broadcast=locked).select_related('operation__buyer','operation__creator'):
            if a.operation_id and a.operation.status=='held':finance.refund(a.operation,'انتهى البث قبل اكتمال مدة الخدمة')
        LiveAdmission.objects.filter(broadcast=locked).update(connected=False,token_until=None)
        LiveComment.objects.filter(broadcast=locked).delete()
        LiveRoomTicket.objects.filter(admission__broadcast=locked).delete()
    try:
        provider('DeleteRoom',{'room':room_name(row)})
        LiveBroadcast.objects.filter(pk=row.pk).update(cleanup_pending=False)
    except ApiError:pass

def synchronize(row,force=False):
    now=timezone.now()
    if row.status not in LIVE or (not force and row.last_checked_at and now-row.last_checked_at<timedelta(seconds=4)):return
    participants=provider('ListParticipants',{'room':room_name(row)},room_name(row)).get('participants',[])
    host=next((p for p in participants if p.get('identity')=='host-'+str(row.owner_id)),None)
    transmitting=bool(host and any(str(t.get('type','AUDIO')).upper() in ('VIDEO','1','AUDIO','0') and not t.get('muted',False) for t in host.get('tracks',[])))
    ids={str(p.get('identity')) for p in participants}
    invalid=[]
    with transaction.atomic():
        financial_locks(row)
        locked=LiveBroadcast.objects.select_for_update().get(pk=row.pk)
        if locked.status not in LIVE:return
        previous=locked.last_checked_at
        locked.last_checked_at=now
        if transmitting:
            locked.status='active';locked.started_at=locked.started_at or now;locked.host_missing_at=None
        else:
            locked.host_missing_at=locked.host_missing_at or now
            if locked.started_at:locked.status='reconnecting'
        viewers=0
        for a in LiveAdmission.objects.filter(broadcast=locked).select_related('user','operation__buyer','operation__creator'):
            if a.user_id==locked.owner_id:continue
            present=str(a.pk) in ids
            if present and (a.banned or not a.user.is_active or not a.session_id or not SessionToken.objects.filter(pk=a.session_id,expires_at__gt=now).exists() or (not moderator(a.user) and UserBlock.objects.filter(Q(user=locked.owner,blocked=a.user)|Q(user=a.user,blocked=locked.owner)).exists())):invalid.append(str(a.pk));present=False
            if present:viewers+=1
            if present and a.connected and transmitting and previous and locked.status=='active':
                a.watched_seconds+=min(10,max(0,int((now-previous).total_seconds())))
            a.connected=present
            if present:a.last_seen_at=now
            a.save(update_fields=['connected','last_seen_at','watched_seconds'])
            if a.operation_id and a.operation.status=='held':
                if a.watched_seconds>=locked.minimum_seconds:finance.settle(a.operation)
                elif not present and not a.last_seen_at and now-a.created_at>timedelta(seconds=90):finance.refund(a.operation,'لم يبدأ الاتصال خلال المهلة')
        locked.viewers=viewers;locked.save();row.__dict__.update(locked.__dict__)
    for identity in invalid:provider('RemoveParticipant',{'room':room_name(row),'identity':identity},room_name(row))
    if (not row.owner.is_active or not row.owner.profile.creator or not row.owner.profile.broadcast_allowed) or (row.host_missing_at and now-row.host_missing_at>timedelta(seconds=45)) or now-row.created_at>timedelta(hours=2):close(row,'انتهى الاتصال بصاحب البث',True);row.refresh_from_db()

@endpoint(['GET','POST'])
def broadcasts(request):
    profile=eligible(request.actor)
    if request.method=='POST':
        eligible(request.actor,True)
        if not enabled():raise ApiError('live_not_configured',503)
        p=request.payload;private=p.get('private') is True;invite_only=p.get('invite_only') is True
        price=amount(p,'price_cents') if private else 0
        maximum=p.get('max_viewers',25);minimum=p.get('minimum_seconds',30)
        if type(maximum) is not int or not 1<=maximum<=min(100,int(os.environ.get("LAYALI_MAX_LIVE_VIEWERS","25"))) or type(minimum) is not int or not 30<=minimum<=3600:raise ApiError('invalid_live_limits')
        names=p.get('invite_names',[])
        if not isinstance(names,list) or len(names)>100 or not all(isinstance(n,str) and 1<=len(n)<=100 for n in names):raise ApiError('invalid_invitations')
        with transaction.atomic():
            User.objects.select_for_update().get(pk=request.actor.pk)
            from django.db import connection
            if connection.vendor=='postgresql':
                with connection.cursor() as cursor:cursor.execute('SELECT pg_advisory_xact_lock(81477231)')
            if LiveBroadcast.objects.filter(owner=request.actor,status__in=LIVE).exists():raise ApiError('already_broadcasting',409)
            if LiveBroadcast.objects.filter(status__in=LIVE).count()>=int(os.environ.get('LAYALI_MAX_LIVE_ROOMS','5')):raise ApiError('live_capacity',409)
            row=LiveBroadcast.objects.create(owner=request.actor,title=text(p,'title',2,120),private=private,invite_only=invite_only,price_cents=price,max_viewers=maximum,minimum_seconds=minimum,gifts_enabled=p.get('gifts_enabled') is True)
            invited=list(User.objects.filter(username__in=names,is_active=True))
            if invite_only and len(invited)!=len(set(names)):raise ApiError('unknown_invited_user')
            row.invited.set(invited)
        try:provider('CreateRoom',{'name':room_name(row),'empty_timeout':60,'departure_timeout':10,'max_participants':maximum+2})
        except ApiError:
            row.status='failed';row.ended_at=timezone.now();row.save();raise
        return JsonResponse(describe(row,request.actor),status=201)
    rows=LiveBroadcast.objects.filter(status__in=LIVE,owner__is_active=True).select_related('owner__profile').order_by('-created_at')[:50]
    items=[];healthy=enabled()
    if healthy:
        try:provider('ListRooms',{})
        except ApiError:healthy=False
    for row in rows:
        try:synchronize(row)
        except ApiError:healthy=False;continue
        if row.status!='active':continue
        if row.invite_only and row.owner_id!=request.actor.pk and not row.invited.filter(pk=request.actor.pk).exists():continue
        try:authorized(row,request.actor)
        except ApiError:continue
        if request.GET.get('q','').strip().lower() not in (row.title+' '+row.owner.profile.display_name+' '+row.owner.username).lower():continue
        items.append(describe(row,request.actor))
    return JsonResponse({'items':items,'available':healthy,'can_broadcast':profile.creator and profile.broadcast_allowed,'recording':False,'max_viewers':min(100,int(os.environ.get('LAYALI_MAX_LIVE_VIEWERS','25')))})

@endpoint(['GET','POST'])
def detail(request,broadcast_id):
    row=find(request.actor,broadcast_id)
    if request.method=='POST':
        if request.payload.get('action')=='end':
            if request.actor.pk!=row.owner_id:require_moderator(request)
            close(row,'إنهاء من صاحب البث' if request.actor.pk==row.owner_id else 'إنهاء إداري');audit(request.actor,row,'Live ended without recording');return JsonResponse({'status':'ended'})
        raise ApiError('invalid_action')
    if row.status in LIVE:authorized(row,request.actor)
    elif request.actor.pk!=row.owner_id and not request.actor.is_superuser:raise ApiError('broadcast_ended',409)
    synchronize(row);return JsonResponse(describe(row,request.actor))

@endpoint(['POST'])
def admission(request,broadcast_id):
    row=find(request.actor,broadcast_id);authorized(row,request.actor);synchronize(row,True)
    if row.status!='active' and request.actor.pk!=row.owner_id:raise ApiError('host_not_transmitting',409)
    with transaction.atomic():
        list(User.objects.select_for_update().filter(pk__in=sorted({request.actor.pk,row.owner_id})).order_by('pk'))
        row=LiveBroadcast.objects.select_for_update().get(pk=row.pk)
        authorized(row,request.actor)
        a=LiveAdmission.objects.select_for_update().filter(broadcast=row,user=request.actor).first()
        if a and a.operation_id and a.operation.status=='refunded':raise ApiError('admission_refunded',409)
        if request.actor.pk!=row.owner_id and not moderator(request.actor) and not(a and a.connected):
            reserved=LiveAdmission.objects.filter(broadcast=row,user__is_active=True,banned=False).exclude(user_id__in=[row.owner_id,request.actor.pk]).filter(Q(connected=True)|Q(token_until__gt=timezone.now())).count()
            if reserved>=row.max_viewers:raise ApiError('live_capacity',409)
        if a is None:a=LiveAdmission.objects.create(broadcast=row,user=request.actor)
        if row.private and request.actor.pk!=row.owner_id and not moderator(request.actor) and a.operation_id is None:
            if request.payload.get('confirmed') is not True or type(request.payload.get('price_cents')) is not int or request.payload['price_cents']!=row.price_cents:raise ApiError('price_confirmation_required',409)
            a.operation=finance.hold(request.actor,row.owner,'live_entry',uuid.UUID(str(row.pk)),str(row.pk),row.price_cents);a.save(update_fields=['operation'])
        a.token_until=timezone.now()+timedelta(seconds=30);a.session=request.api_token;a.save(update_fields=['token_until','session'])
        raw=secrets.token_urlsafe(48)
        LiveRoomTicket.objects.filter(expires_at__lt=timezone.now()).delete()
        LiveRoomTicket.objects.create(digest=hashlib.sha256(raw.encode()).hexdigest(),admission=a,session=request.api_token,expires_at=timezone.now()+timedelta(seconds=30))
    return JsonResponse({'url':request.build_absolute_uri(f'/api/live/{row.pk}/room'),'ticket':raw,'owner':request.actor.pk==row.owner_id})

def join_token(a,session):
    row=a.broadcast;authorized(row,a.user)
    if a.operation_id and a.operation.status=='refunded':raise ApiError('admission_refunded',403)
    owner=a.user_id==row.owner_id
    if row.private and not owner and not moderator(a.user) and not a.operation_id:raise ApiError('payment_required',403)
    identity='host-'+str(a.user_id) if owner else str(a.pk)
    return jwt_token({'roomJoin':True,'room':room_name(row),'canPublish':owner,'canSubscribe':not owner,'canPublishData':False,'canUpdateOwnMetadata':False},identity,{'session':session.pk,'admission':a.pk})

@csrf_exempt
def room(request,broadcast_id):
    if request.method!='POST':return HttpResponse(status=405)
    try:
        with transaction.atomic():
            ticket=LiveRoomTicket.objects.select_for_update().select_related('admission__broadcast__owner','admission__user','admission__operation','session__user').filter(digest=hashlib.sha256(request.POST.get('ticket','').encode()).hexdigest(),admission__broadcast_id=broadcast_id,used_at__isnull=True,expires_at__gt=timezone.now(),session__expires_at__gt=timezone.now(),session__user__is_active=True).first()
            if not ticket:return HttpResponse(status=403)
            a=ticket.admission;join=join_token(a,ticket.session);ticket.used_at=timezone.now();ticket.save(update_fields=['used_at'])
        nonce=secrets.token_urlsafe(24)
        control=signing.dumps({'session':ticket.session_id,'admission':a.pk},salt='live-control')
        config={'id':str(broadcast_id),'token':join,'control':control,'url':os.environ.get('LIVEKIT_PUBLIC_URL',request.build_absolute_uri('/livekit').replace('https://','wss://')),'owner':a.user_id==a.broadcast.owner_id,'ownerId':a.broadcast.owner_id,'ownerAvatar':a.broadcast.owner.profile.avatar_url,'ownerName':a.broadcast.owner.profile.display_name or a.broadcast.owner.username,'title':a.broadcast.title,'gifts':a.broadcast.gifts_enabled}
        public=urlsplit(config['url'])
        if public.scheme not in ('wss','ws') or public.username or public.password or (public.scheme=='ws' and os.environ.get('LAYALI_TESTING')!='1'):raise ApiError('invalid_live_origin',503)
        ws_origin=public.scheme+'://'+public.netloc
        response=render(request,'core/live_room.html',{'config':config,'nonce':nonce})
        response['Cache-Control']='no-store';response['Referrer-Policy']='no-referrer'
        response['Content-Security-Policy']=f"default-src 'none'; script-src 'self' 'nonce-{nonce}'; style-src 'unsafe-inline'; connect-src 'self' {ws_origin}; media-src 'self' blob:; img-src 'self' data: https:; frame-ancestors 'none'; base-uri 'none'"
        return response
    except ApiError as e:return JsonResponse({'error':e.message},status=e.status)

@csrf_exempt
def authorize(request):
    try:
        uri=request.headers.get('X-Forwarded-Uri','');token=parse_qs(urlsplit(uri).query).get('access_token',[None])[0]
        if not token:token=request.headers.get('Authorization','').removeprefix('Bearer ')
        data=jwt.decode(token,os.environ['LIVEKIT_API_SECRET'],algorithms=['HS256'],issuer=os.environ['LIVEKIT_API_KEY'],options={'require':['exp','iss','sub']})
        metadata=json.loads(data.get('metadata','{}'))
        session=SessionToken.objects.select_related('user').get(pk=metadata['session'],expires_at__gt=timezone.now(),user__is_active=True)
        a=LiveAdmission.objects.select_related('broadcast__owner','user','operation').get(pk=metadata['admission'],user=session.user)
        authorized(a.broadcast,a.user)
        if data.get('video',{}).get('room')!=room_name(a.broadcast) or data['sub']!=('host-'+str(a.user_id) if a.user_id==a.broadcast.owner_id else str(a.pk)):raise ValueError()
        if a.operation_id and a.operation.status=='refunded':raise ValueError()
        if a.broadcast.private and a.user_id!=a.broadcast.owner_id and not moderator(a.user) and not a.operation_id:raise ValueError()
        return HttpResponse(status=204)
    except Exception:return HttpResponse(status=403)

@endpoint(['GET','POST'])
def events(request,broadcast_id):
    row=find(request.actor,broadcast_id);authorized(row,request.actor)
    a=LiveAdmission.objects.filter(broadcast=row,user=request.actor,banned=False).first()
    if not a or (row.private and request.actor.pk!=row.owner_id and not moderator(request.actor) and not a.operation_id):raise ApiError('admission_required',403)
    if request.method=='POST':
        action=request.payload.get('action')
        if action=='comment':
            if LiveComment.objects.filter(user=request.actor,created_at__gt=timezone.now()-timedelta(seconds=10)).count()>=5:raise ApiError('slow_down',429)
            LiveComment.objects.create(broadcast=row,user=request.actor,text=text(request.payload,'text',1,300))
        elif action=='like':LiveLike.objects.get_or_create(broadcast=row,user=request.actor)
        elif action=='follow':
            from .models import Follow
            if request.actor.pk==row.owner_id:raise ApiError('invalid_action')
            Follow.objects.get_or_create(user=request.actor,creator=row.owner)
        elif action=='report':LiveReport.objects.create(broadcast=row,user=request.actor,reason=text(request.payload,'reason',3,500))
        elif action=='gift':
            if not row.gifts_enabled or request.actor.pk==row.owner_id:raise ApiError('gift_unavailable',409)
            price=request.payload.get('price_cents')
            if type(price) is not int or price not in (100,300,1000) or request.payload.get('confirmed') is not True:raise ApiError('price_confirmation_required',409)
            key=uuid.UUID(text(request.payload,'request_key',36,36))
            with transaction.atomic():
                list(User.objects.select_for_update().filter(pk__in=sorted([request.actor.pk,row.owner_id])).order_by('pk'))
                current=LiveBroadcast.objects.select_for_update().get(pk=row.pk)
                if current.status!='active':raise ApiError('broadcast_ended',409)
                op=finance.hold(request.actor,row.owner,'live_gift',key,str(row.pk),price);finance.settle(op)
        elif action=='ban':
            if row.owner_id!=request.actor.pk:require_moderator(request)
            user_id=amount(request.payload,'user_id')
            target=LiveAdmission.objects.filter(broadcast=row,user_id=user_id).exclude(user=row.owner).first()
            if not target:raise ApiError('participant_unavailable',404)
            if moderator(target.user):raise ApiError('protected_account',403)
            target.banned=True;target.connected=False;target.token_until=None;target.save(update_fields=['banned','connected','token_until'])
            provider('RemoveParticipant',{'room':room_name(row),'identity':str(target.pk)},room_name(row));audit(request.actor,row,f'Banned live participant {user_id}')
        else:raise ApiError('invalid_action')
    synchronize(row)
    comments=LiveComment.objects.filter(broadcast=row).select_related('user__profile').order_by('-id')[:50]
    result=describe(row,request.actor);result['comments']=[{'id':c.pk,'user_id':c.user_id,'name':c.user.profile.display_name or c.user.username,'text':c.text} for c in reversed(comments)]
    if row.owner_id==request.actor.pk:result['participants']=[{'user_id':x.user_id,'name':x.user.profile.display_name or x.user.username} for x in LiveAdmission.objects.filter(broadcast=row,connected=True).exclude(user=row.owner).select_related('user__profile')]
    return JsonResponse(result)

@csrf_exempt
def control(request,broadcast_id):
    try:
        data=signing.loads(request.headers.get('X-Live-Control',''),salt='live-control',max_age=7200)
        session=SessionToken.objects.select_related('user').get(pk=data['session'],expires_at__gt=timezone.now(),user__is_active=True)
        a=LiveAdmission.objects.select_related('broadcast__owner','user','operation').get(pk=data['admission'],user=session.user,broadcast_id=broadcast_id)
        request.actor=session.user;request.api_token=session;request.payload={}
        if request.method=='POST':
            if request.content_type!='application/json' or len(request.body)>4096:raise ApiError('invalid_input')
            request.payload=json.loads(request.body)
            if not isinstance(request.payload,dict):raise ApiError('invalid_input')
            if request.payload.get('action')=='token':return JsonResponse({'token':join_token(a,session)})
            if request.payload.get('action')=='end':return detail.__wrapped__(request,broadcast_id)
        elif request.method!='GET':return HttpResponse(status=405)
        return events.__wrapped__(request,broadcast_id)
    except ApiError as e:return JsonResponse({'error':e.message},status=e.status)
    except Exception:return JsonResponse({'error':'live_authorization_required'},status=403)

@endpoint(['GET','POST'])
def administration(request):
    require_moderator(request)
    if request.method=='POST':
        row=find(request.actor,text(request.payload,'id',36,36))
        if request.payload.get('action')=='end':close(row,'إيقاف إداري');audit(request.actor,row,'Live stopped; no media retained')
        elif request.payload.get('action')=='price':
            require_admin(request)
            with transaction.atomic():
                row=LiveBroadcast.objects.select_for_update().get(pk=row.pk)
                if not row.private or LiveAdmission.objects.filter(broadcast=row,operation__isnull=False).exists():raise ApiError('price_locked',409)
                row.price_cents=amount(request.payload,'price_cents');row.save(update_fields=['price_cents']);audit(request.actor,row,'Live entry price changed before sales')
        elif request.payload.get('action')=='resolve':LiveReport.objects.filter(broadcast=row).update(resolved=True);audit(request.actor,row,'Live reports reviewed without recording')
        else:raise ApiError('invalid_action')
    rows=LiveBroadcast.objects.filter(status__in=LIVE).select_related('owner__profile')
    items=[]
    for row in rows:
        try:synchronize(row)
        except ApiError:pass
        item=describe(row,request.actor);item['reports']=[{'id':r.pk,'reason':r.reason} for r in LiveReport.objects.filter(broadcast=row,resolved=False)];items.append(item)
    return JsonResponse({'items':items,'recording':False})
