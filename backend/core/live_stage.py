"""Server-owned stage permissions, settled gift events and non-wagering PK scores."""
import uuid
from datetime import timedelta
from django.db import transaction
from django.db.models import Sum
from django.utils import timezone
from .models import (LiveAdmission,LiveBroadcast,LiveSeat,LiveRoomModerator,LiveGiftDefinition,
                     LiveGiftEvent,LivePK,LiveComment,BenefitGrant,FinancialOperation)
from .views import ApiError,User,text


def publishing(admission):
    from .live import eligible
    owner=admission.user_id==admission.broadcast.owner_id
    seat=LiveSeat.objects.filter(admission=admission,status='approved').first()
    if not owner and not seat:return False,[]
    eligible(admission.user,True)
    mode=admission.broadcast.mode if owner else seat.mode
    return True,['microphone']+(['camera'] if mode=='video' else [])


def room_moderator(row,user):
    return row.owner_id==user.pk or LiveRoomModerator.objects.filter(broadcast=row,user=user).exists()


def require_room_manager(request,row):
    if not room_moderator(row,request.actor):
        from .live import require_moderator
        require_moderator(request)


def person(user):
    total=FinancialOperation.objects.filter(creator=user,kind='live_gift',status='settled').aggregate(n=Sum('amount_cents'))['n'] or 0
    return {'user_id':user.pk,'name':user.profile.display_name or user.username,'avatar_url':user.profile.avatar_url,
            'level':total//1000,'vip':BenefitGrant.objects.filter(user=user,kind='vip',expires_at__gt=timezone.now()).exists()}


def pk_json(row):
    pk=LivePK.objects.filter(broadcast=row).order_by('-pk').first()
    if not pk:return None
    sums={v['team']:v['n'] for v in LiveGiftEvent.objects.filter(pk_round=pk,operation__status='settled').values('team').annotate(n=Sum('operation__amount_cents'))}
    return {'id':pk.pk,'teams':pk.teams,'scores':[sums.get(0,0),sums.get(1,0)],'ends_at':pk.ends_at,
            'active':not pk.cancelled_at and pk.ends_at>timezone.now(),'cancelled':bool(pk.cancelled_at)}


def snapshot(row,user):
    seats=LiveSeat.objects.filter(admission__broadcast=row).select_related('admission__user__profile')
    own=next((s for s in seats if s.admission.user_id==user.pk),None)
    manager=room_moderator(row,user)
    a=LiveAdmission.objects.select_related('broadcast','user').filter(broadcast=row,user=user).first()
    try:can_publish,sources=publishing(a) if a else (False,[])
    except ApiError:can_publish,sources=False,[]
    speakers=[dict(person(row.owner),identity='host-'+str(row.owner_id),mode=row.mode,owner=True)]
    for s in seats:
        if s.status=='approved' and not s.admission.banned:
            speakers.append(dict(person(s.admission.user),identity=str(s.admission_id),mode=s.mode,owner=False))
    events=LiveGiftEvent.objects.filter(broadcast=row).select_related('operation__buyer__profile','operation__creator__profile').order_by('-pk')[:20]
    top=LiveGiftEvent.objects.filter(broadcast=row,operation__status='settled').values('operation__buyer_id','operation__buyer__profile__display_name','operation__buyer__username').annotate(total=Sum('operation__amount_cents')).order_by('-total')[:5]
    received=LiveGiftEvent.objects.filter(broadcast=row,operation__status='settled').aggregate(total=Sum('operation__amount_cents'),earnings=Sum('operation__creator_cents'))
    return {'mode':row.mode,'max_speakers':row.max_speakers,'speakers':speakers,'can_publish':can_publish,'publish_sources':sources,
            'seat_status':own.status if own else '', 'manage_room':manager,
            'seat_requests':[dict(person(s.admission.user),mode=s.mode) for s in seats if s.status=='pending'] if manager else [],
            'moderator_ids':list(LiveRoomModerator.objects.filter(broadcast=row).values_list('user_id',flat=True)) if row.owner_id==user.pk else [],
            'gift_catalog':list(LiveGiftDefinition.objects.filter(enabled=True).order_by('price_cents').values('code','name','symbol','price_cents','effect')),
            'gift_events':[{'id':g.pk,'sender':g.operation.buyer.profile.display_name or g.operation.buyer.username,'recipient':g.operation.creator.profile.display_name or g.operation.creator.username,'name':g.gift_name,'symbol':g.symbol,'effect':g.effect} for g in reversed(events)],
            'top_supporters':[{'name':t['operation__buyer__profile__display_name'] or t['operation__buyer__username'],'total':t['total']} for t in top],
            'gift_total_cents':received['total'] or 0,'gift_goal_cents':row.gift_goal_cents,
            'earned_cents':received['earnings'] or 0 if row.owner_id==user.pk else None,'pk':pk_json(row),
            'pinned_comment':{'id':row.pinned_comment_id,'name':row.pinned_comment.user.profile.display_name or row.pinned_comment.user.username,'text':row.pinned_comment.text} if row.pinned_comment_id else None}


def action(request,row,a):
    from .live import eligible,provider,room_name,authorized,audit
    p=request.payload;kind=p.get('action')
    if kind=='gift':
        code=p.get('gift_code')
        if not code:code={100:'rose',300:'heart',1000:'star'}.get(p.get('price_cents'))
        gift=LiveGiftDefinition.objects.filter(pk=code,enabled=True).first()
        if not row.gifts_enabled or not gift:raise ApiError('gift_unavailable',409)
        if p.get('confirmed') is not True or type(p.get('price_cents')) is not int or p['price_cents']!=gift.price_cents:raise ApiError('price_confirmation_required',409)
        recipient_id=p.get('recipient_id',row.owner_id)
        if type(recipient_id) is not int or recipient_id==request.actor.pk:raise ApiError('invalid_gift_recipient')
        key=uuid.UUID(text(p,'request_key',36,36))
        with transaction.atomic():
            users={u.pk:u for u in User.objects.select_for_update().filter(pk__in=sorted({request.actor.pk,recipient_id,row.owner_id})).order_by('pk')}
            current=LiveBroadcast.objects.select_for_update().get(pk=row.pk);authorized(current,request.actor)
            if current.status!='active':raise ApiError('broadcast_ended',409)
            if recipient_id!=row.owner_id and not LiveSeat.objects.filter(admission__broadcast=row,admission__user_id=recipient_id,admission__banned=False,status='approved').exists():raise ApiError('invalid_gift_recipient')
            target=users.get(recipient_id)
            if not target:raise ApiError('invalid_gift_recipient')
            eligible(target,True)
            from . import finance
            op=finance.hold(request.actor,target,'live_gift',key,str(row.pk),gift.price_cents)
            existing=LiveGiftEvent.objects.filter(operation=op).first()
            if existing:
                if existing.gift_code!=gift.code:raise ApiError('payment_request_conflict',409)
                return
            finance.settle(op)
            pk=LivePK.objects.filter(broadcast=row,cancelled_at__isnull=True,ends_at__gt=timezone.now()).order_by('-id').first()
            team=next((i for i,members in enumerate(pk.teams) if recipient_id in members),None) if pk else None
            LiveGiftEvent.objects.create(broadcast=row,operation=op,gift_code=gift.code,gift_name=gift.name,symbol=gift.symbol,effect=gift.effect,pk_round=pk if team is not None else None,team=team)
        return
    if kind=='pk_start':
        from .live import synchronize
        synchronize(row,True)
    with transaction.atomic():
        current=LiveBroadcast.objects.select_for_update().get(pk=row.pk);authorized(current,request.actor)
        if kind=='seat_request':
            eligible(request.actor,True)
            if current.max_speakers<=1 or request.actor.pk==row.owner_id:raise ApiError('stage_unavailable',409)
            mode=p.get('mode')
            if mode not in ('audio','video') or (row.mode=='audio' and mode!='audio'):raise ApiError('invalid_stage_mode')
            seat,_=LiveSeat.objects.get_or_create(admission=a,defaults={'mode':mode})
            if seat.status=='approved':return
            if timezone.now()-seat.updated_at<timedelta(seconds=15) and seat.status=='rejected':raise ApiError('slow_down',429)
            seat.mode=mode;seat.status='pending';seat.save();return
        if kind=='seat_leave':
            seat=LiveSeat.objects.filter(admission=a).first()
            if seat:
                # Remove tracks before revoking DB permission. On provider failure the DB stays unchanged.
                provider('RemoveParticipant',{'room':room_name(row),'identity':str(a.pk)},room_name(row))
                seat.status='left';seat.save()
            return
        require_room_manager(request,current)
        if kind=='seat_decision':
            seat=LiveSeat.objects.select_related('admission__user','admission__broadcast').filter(admission__broadcast=row,admission__user_id=p.get('user_id')).first()
            if not seat:raise ApiError('participant_unavailable',404)
            if p.get('approve') is True:
                eligible(seat.admission.user,True)
                if seat.admission.banned:raise ApiError('broadcast_forbidden',403)
                if seat.status!='approved' and LiveSeat.objects.filter(admission__broadcast=row,status='approved').count()>=current.max_speakers-1:raise ApiError('stage_full',409)
                seat.status='approved'
            else:
                provider('RemoveParticipant',{'room':room_name(row),'identity':str(seat.admission_id)},room_name(row));seat.status='rejected'
            seat.save();audit(request.actor,row,f'Stage permission {seat.admission.user_id}: {seat.status}');return
        if kind in ('pin_comment','delete_comment'):
            comment=LiveComment.objects.filter(broadcast=row,pk=p.get('comment_id')).first()
            if kind=='pin_comment':current.pinned_comment=comment;current.save(update_fields=['pinned_comment']);row.pinned_comment=comment
            elif comment:comment.delete()
            return
        if kind=='set_moderator':
            if request.actor.pk!=row.owner_id:raise ApiError('owner_required',403)
            target=LiveAdmission.objects.filter(broadcast=row,user_id=p.get('user_id'),banned=False).exclude(user=row.owner).first()
            if not target:raise ApiError('participant_unavailable',404)
            if p.get('enabled') is True:LiveRoomModerator.objects.get_or_create(broadcast=row,user=target.user)
            else:LiveRoomModerator.objects.filter(broadcast=row,user=target.user).delete()
            audit(request.actor,row,f'Room moderator changed {target.user_id}');return
        if kind=='pk_start':
            if request.actor.pk!=row.owner_id:raise ApiError('owner_required',403)
            teams=p.get('teams');duration=p.get('duration')
            if not isinstance(teams,list) or len(teams)!=2 or any(not isinstance(t,list) or not t for t in teams):raise ApiError('invalid_pk_teams')
            flat=[u for t in teams for u in t]
            if any(type(u) is not int for u in flat) or len(flat)!=len(set(flat)) or len(flat)>12 or row.owner_id not in flat or type(duration) is not int or duration not in (60,180,300):raise ApiError('invalid_pk_teams')
            permitted={row.owner_id}|set(LiveSeat.objects.filter(admission__broadcast=row,status='approved',admission__connected=True).values_list('admission__user_id',flat=True))
            if not set(flat)<=permitted:raise ApiError('invalid_pk_teams')
            if LivePK.objects.filter(broadcast=row,cancelled_at__isnull=True,ends_at__gt=timezone.now()).exists():raise ApiError('pk_already_active',409)
            LivePK.objects.create(broadcast=row,teams=teams,ends_at=timezone.now()+timedelta(seconds=duration));audit(request.actor,row,'PK started; no wagers or automatic payouts');return
        if kind=='pk_cancel':
            if request.actor.pk!=row.owner_id:raise ApiError('owner_required',403)
            LivePK.objects.filter(broadcast=row,cancelled_at__isnull=True,ends_at__gt=timezone.now()).update(cancelled_at=timezone.now());return
        raise ApiError('invalid_action')
