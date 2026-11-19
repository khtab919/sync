"""Super-admin-only account discipline and global room safety.

Never expose raw devices/IPs, never impersonate users, never write wallet
balances as a side effect of sanctions. Existing financial ledgers are immutable.
"""
from datetime import timedelta,datetime
import logging
from django.db import transaction
from django.http import JsonResponse
from django.utils import timezone
from django.db.models import Q
from .views import endpoint,ApiError,text,User
from .models import (
    AccountModerationState,AccountSanction,SessionToken,LiveBroadcast,LiveAdmission,
    LiveSeat,LiveRoomModerator,InternalCall,
)
from .admin_security import event

LOGGER=logging.getLogger('relaxia.moderation')

def active_status(user):
    if user.is_superuser:return None
    row=AccountModerationState.objects.filter(user=user).first()
    if not row:return None
    if row.status=='banned_permanent':return row
    if row.status=='suspended' and (not row.suspended_until or row.suspended_until>timezone.now()):
        return row
    return None


def enforce(user):
    if user.is_superuser:return
    row=active_status(user)
    if not row:return
    if row.status=='suspended':
        raise ApiError('account_suspended',403)
    raise ApiError('account_banned_permanent',403)


def describe(row):
    return {'user_id':row.user_id,'status':row.status,'suspended_until':row.suspended_until,
            'reason':row.reason,'version':row.version,'updated_at':row.updated_at}


def eject_from_live(user):
    from .live import room_name,provider,close,LIVE
    owned=list(LiveBroadcast.objects.filter(owner=user,status__in=LIVE))
    for room in owned:
        try:close(room,'Administrative suspension')
        except Exception:LOGGER.exception('Failed to close suspended host room=%s',room.pk)
    participations=list(LiveAdmission.objects.filter(
        user=user,broadcast__status__in=LIVE).select_related('broadcast'))
    for a in participations:
        if a.broadcast_id in {r.pk for r in owned}:continue
        try:provider('RemoveParticipant',{'room':room_name(a.broadcast),'identity':str(a.pk)},room_name(a.broadcast))
        except Exception:LOGGER.exception('Failed to evict restricted participant room=%s',a.broadcast_id)
    LiveAdmission.objects.filter(user=user,broadcast__status__in=LIVE).update(
        banned=True,connected=False,token_until=None)
    LiveSeat.objects.filter(admission__user=user,admission__broadcast__status__in=LIVE).update(status='rejected')
    LiveRoomModerator.objects.filter(user=user,broadcast__status__in=LIVE).delete()


@endpoint(['GET','POST'])
def discipline(request):
    if not request.actor.is_superuser:raise ApiError('super_admin_required',403)
    if request.method=='POST':
        action=request.payload.get('action')
        if action not in ('suspend','ban_permanent','restore'):raise ApiError('invalid_action')
        user_id=request.payload.get('user_id')
        if type(user_id) is not int or user_id<1:raise ApiError('invalid_user_id')
        reason=text(request.payload,'reason',3,500)
        if action=='suspend':
            duration=request.payload.get('hours')
            if type(duration) is not int or not 1<=duration<=24*365:raise ApiError('invalid_suspension_hours')
            until=timezone.now()+timedelta(hours=duration)
        else:until=None
        with transaction.atomic():
            users={u.pk:u for u in User.objects.select_for_update().filter(
                pk__in=sorted({user_id,request.actor.pk})).order_by('pk')}
            target=users.get(user_id)
            if not target:raise ApiError('account_unavailable',404)
            if target.is_superuser or target.pk==request.actor.pk:raise ApiError('protected_account',403)
            row,_=AccountModerationState.objects.select_for_update().get_or_create(user=target)
            if action=='restore':
                if row.status=='active':raise ApiError('already_active',409)
                # Only restore users banned by this feature; other legacy blocks
                # or active sanctions still independently restrict access.
                previous=row.status
                row.status='active';row.reason=reason;row.suspended_until=None
                if previous=='banned_permanent':
                    target.is_active=True;target.save(update_fields=['is_active'])
            elif action=='suspend':
                row.status='suspended';row.reason=reason;row.suspended_until=until
                if not target.is_active:
                    # A different permanent ban cannot be bypassed by a timed suspension.
                    raise ApiError('account_unavailable',409)
            else:
                row.status='banned_permanent';row.reason=reason;row.suspended_until=None
                target.is_active=False;target.save(update_fields=['is_active'])
            row.version+=1;row.updated_by=request.actor
            row.save()
            if action!='restore':
                # Revoke all access tokens immediately. Existing clients must log in again.
                SessionToken.objects.filter(user=target).delete()
                InternalCall.objects.filter(Q(caller=target)|Q(callee=target),
                    status__in=['ringing','accepted']).update(status='ended',ended_at=timezone.now())
            event(request.actor,'account_'+action,target.pk,reason,details={
                'until':until.isoformat() if until else None,'state_version':row.version})
        if action!='restore':eject_from_live(target)
        return JsonResponse({'ok':True,'account':describe(row)})
    qs=AccountModerationState.objects.select_related('user').filter(
        status__in=('suspended','banned_permanent')).order_by('-updated_at')[:100]
    return JsonResponse({'items':[{
        **describe(row),'username':row.user.username,'public_id':row.user.profile.public_id,
        'effective':bool(active_status(row.user))
    } for row in qs]})


@endpoint(['POST'],authenticated=False)
def check_account_status(request):
    """Privacy: return suspension reason ONLY after the user proves password.

    Account discovery by email/user ID is not allowed.
    """
    from django.contrib.auth import authenticate
    from .identity import normalize_email,email_matches
    email=normalize_email(request.payload.get('email'))
    password=request.payload.get('password')
    if not isinstance(password,str) or not password:raise ApiError('invalid_credentials',401)
    matches=list(email_matches(email)[:2])
    if len(matches)!=1 or not matches[0].check_password(password):
        raise ApiError('invalid_credentials',401)
    row=active_status(matches[0])
    if not row:return JsonResponse({'status':'active'})
    return JsonResponse(describe(row))
