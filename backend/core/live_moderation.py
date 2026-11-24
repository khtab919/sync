"""Server-authoritative room moderation; WebView/Firestore snapshots are display only.

LiveKit RoomService evicts kicked users immediately. Account and room permissions
remain enforced on all authenticated endpoints, token refreshes and SFU grants.
Remote microphones are never unmuted without the device owner's consent.
"""
import re
from datetime import timedelta
from django.db import transaction
from django.db.models import F
from django.utils import timezone
from django.contrib.auth.hashers import make_password,check_password
from .models import (
    LiveBroadcast,LiveAdmission,LiveSeat,LiveRoomModerator,
    LiveChatRestriction,LiveHostBan,LiveReport,LiveComment,SessionToken,
)
from .views import ApiError,text,User
from .admin_security import scopes,event


def is_global(user):
    return bool(user.is_superuser or 'live.moderate' in scopes(user))


def is_manager(row,user):
    return is_global(user) or user.pk==row.owner_id or LiveRoomModerator.objects.filter(
        broadcast=row,user=user).exists()


def role(row,user):
    if user.is_superuser:return 'super_admin'
    if is_global(user):return 'app_moderator'
    if row.owner_id==user.pk:return 'host'
    if LiveRoomModerator.objects.filter(broadcast=row,user=user).exists():return 'room_moderator'
    return 'viewer'


def check_target(row,actor,target):
    if target.pk==actor.pk:raise ApiError('self_moderation_forbidden',403)
    if target.is_superuser or target.is_staff:raise ApiError('protected_account',403)
    if target.pk==row.owner_id:raise ApiError('protected_host',403)
    if LiveRoomModerator.objects.filter(broadcast=row,user=target).exists() and not (
        actor.pk==row.owner_id or is_global(actor)
    ):raise ApiError('moderator_hierarchy',403)


def free_seat(row,ignore_user_id=None):
    cap=max(1,row.max_speakers)
    occupied=set(LiveSeat.objects.filter(
        admission__broadcast=row,status='approved'
    ).exclude(admission__user_id=ignore_user_id).exclude(seat_number__isnull=True)
                 .values_list('seat_number',flat=True))
    locked=set(row.locked_seat_numbers or [])
    return next((n for n in range(1,cap) if n not in occupied and n not in locked),None)


def check_room_password(row,user,admission,plaintext=None):
    if not row.password_hash or user.pk==row.owner_id or is_global(user):
        return
    if admission and admission.password_verified_at:
        return
    if not isinstance(plaintext,str) or not check_password(plaintext,row.password_hash):
        raise ApiError('room_password_required',403)


def ensure_chat_allowed(row,user,message):
    if is_manager(row,user):return
    if not row.chat_enabled:raise ApiError('room_chat_disabled',403)
    restraint=LiveChatRestriction.objects.filter(broadcast=row,user=user).first()
    if restraint and (restraint.entire_stream or (restraint.muted_until and restraint.muted_until>timezone.now())):
        raise ApiError('room_chat_muted',403)
    if row.block_external_links and re.search(
        r'(?i)(?:https?://|www\.|(?:[a-z0-9-]+\.)+(?:com|net|org|io|me|ly|app|ru|xyz|info)(?:/|\b))',
        message,
    ):raise ApiError('external_links_blocked',403)


def manager_action(request,row):
    from .live import provider,room_name
    actor=request.actor
    p=request.payload
    kind=p.get('action')
    room_actions={
        'kick_user','mute_chat','unmute_chat','force_mute_mic','release_mic_mute',
        'invite_to_seat','remove_from_seat','set_room_moderator',
        'ban_from_host_rooms','remove_host_ban','room_settings'
    }
    if kind not in room_actions:raise ApiError('invalid_action')
    if not is_manager(row,actor):raise ApiError('room_manager_required',403)
    # Settings and permanent host-wide bans belong to host or Super Admin.
    host_only={'room_settings','ban_from_host_rooms','remove_host_ban','set_room_moderator'}
    if kind in host_only and actor.pk!=row.owner_id and not actor.is_superuser:
        raise ApiError('host_required',403)

    with transaction.atomic():
        room=LiveBroadcast.objects.select_for_update().get(pk=row.pk)
        if room.status not in ('starting','active','reconnecting'):
            raise ApiError('broadcast_ended',409)
        now=timezone.now()
        if kind=='room_settings':
            data=p.get('settings')
            if not isinstance(data,dict) or not data:raise ApiError('invalid_room_settings')
            accepted={'chat_enabled','chat_slow_seconds','block_external_links','seat_join_mode',
                      'lock_seat','password','clear_password','capacity','room_theme'}
            if any(k not in accepted for k in data):raise ApiError('invalid_room_settings')
            if 'capacity' in data:
                cap=data['capacity']
                if room.mode!='audio' or type(cap) is not int or cap not in (5,9,13):
                    raise ApiError('invalid_room_settings')
                occupied=LiveSeat.objects.filter(admission__broadcast=room,status='approved')
                if occupied.filter(seat_number__gte=cap).exists() or occupied.count()>cap-1:
                    raise ApiError('occupied_seat_outside_layout',409)
                room.max_speakers=cap
                room.locked_seat_numbers=[n for n in (room.locked_seat_numbers or []) if n<cap]
            if 'room_theme' in data:
                if room.mode!='audio' or data['room_theme'] not in ('royal','diamond','emerald','amethyst'):
                    raise ApiError('invalid_room_settings')
                room.room_theme=data['room_theme']
            if 'chat_enabled' in data:
                if type(data['chat_enabled']) is not bool:raise ApiError('invalid_room_settings')
                room.chat_enabled=data['chat_enabled']
            if 'chat_slow_seconds' in data:
                if type(data['chat_slow_seconds']) is not int or data['chat_slow_seconds'] not in (0,5,10,30):
                    raise ApiError('invalid_room_settings')
                room.chat_slow_seconds=data['chat_slow_seconds']
            if 'block_external_links' in data:
                if type(data['block_external_links']) is not bool:raise ApiError('invalid_room_settings')
                room.block_external_links=data['block_external_links']
            if 'seat_join_mode' in data:
                if data['seat_join_mode'] not in ('free','request'):raise ApiError('invalid_room_settings')
                room.seat_join_mode=data['seat_join_mode']
            if 'lock_seat' in data:
                x=data['lock_seat']
                if not isinstance(x,dict) or type(x.get('number')) is not int or type(x.get('locked')) is not bool:
                    raise ApiError('invalid_room_settings')
                limit=room.max_speakers
                if not 1<=x['number']<limit:raise ApiError('invalid_room_settings')
                locks=set(room.locked_seat_numbers or [])
                if x['locked']:locks.add(x['number'])
                else:locks.discard(x['number'])
                room.locked_seat_numbers=sorted(locks)
            if 'password' in data:
                password=data['password']
                if not isinstance(password,str) or not 4<=len(password)<=40:raise ApiError('invalid_room_password')
                room.password_hash=make_password(password)
                # A changed password invalidates prior proofs from every guest.
                LiveAdmission.objects.filter(broadcast=room).exclude(user_id=room.owner_id).update(password_verified_at=None)
                # Reconnecting clients must submit the new password first.
            if 'clear_password' in data:
                if data['clear_password'] is not True:raise ApiError('invalid_room_settings')
                room.password_hash=''
            room.control_revision=F('control_revision')+1
            room.save()
            event(actor,'room_settings',room.pk,'Updated room moderation options',details={
                'keys':list(data.keys()) # never log plaintext password
            })
            return {'ok':True,'revision':'updated'}

        target_id=p.get('user_id')
        if type(target_id) is not int or target_id<1:raise ApiError('invalid_user_id')
        target=User.objects.filter(pk=target_id).first()
        if not target:raise ApiError('participant_unavailable',404)
        check_target(room,actor,target)
        admission=LiveAdmission.objects.select_for_update().filter(broadcast=room,user=target).first()
        if not admission and kind not in ('ban_from_host_rooms','remove_host_ban'):
            raise ApiError('participant_unavailable',404)
        reason=str(p.get('reason','Room moderation'))[:300]
        if kind=='mute_chat':
            duration=p.get('duration')
            if duration not in ('10m','1h','stream'):raise ApiError('invalid_duration')
            expiry=now+(timedelta(minutes=10) if duration=='10m' else timedelta(hours=1)) if duration!='stream' else None
            LiveChatRestriction.objects.update_or_create(broadcast=room,user=target,
                defaults={'muted_until':expiry,'entire_stream':duration=='stream','reason':reason})
        elif kind=='unmute_chat':
            LiveChatRestriction.objects.filter(broadcast=room,user=target).delete()
        elif kind=='set_room_moderator':
            if type(p.get('enabled')) is not bool:raise ApiError('invalid_input')
            if p['enabled']:
                if admission.banned:raise ApiError('participant_unavailable',404)
                LiveRoomModerator.objects.get_or_create(broadcast=room,user=target)
            else:
                LiveRoomModerator.objects.filter(broadcast=room,user=target).delete()
        elif kind in ('ban_from_host_rooms','remove_host_ban'):
            if kind=='ban_from_host_rooms':
                LiveHostBan.objects.update_or_create(host=room.owner,user=target,
                    defaults={'reason':reason})
                # Ban the current admission too, and remove immediately.
                if admission:
                    provider('RemoveParticipant',{'room':room_name(room),'identity':str(admission.pk)},room_name(room))
                    admission.banned=True;admission.connected=False;admission.token_until=None
                    admission.save(update_fields=['banned','connected','token_until'])
                    LiveRoomModerator.objects.filter(broadcast=room,user=target).delete()
                    LiveSeat.objects.filter(admission=admission).update(status='rejected')
            else:LiveHostBan.objects.filter(host=room.owner,user=target).delete()
        elif kind=='kick_user':
            provider('RemoveParticipant',{'room':room_name(room),'identity':str(admission.pk)},room_name(room))
            admission.banned=True;admission.connected=False;admission.token_until=None
            admission.save(update_fields=['banned','connected','token_until'])
            LiveRoomModerator.objects.filter(broadcast=room,user=target).delete()
            LiveSeat.objects.filter(admission=admission).update(status='rejected')
        elif kind in ('invite_to_seat','remove_from_seat'):
            seat=LiveSeat.objects.filter(admission=admission).first()
            if kind=='invite_to_seat':
                if admission.banned:raise ApiError('broadcast_forbidden',403)
                number=free_seat(room,ignore_user_id=target.pk)
                if number is None:raise ApiError('stage_full',409)
                # The invite grants permission only; the guest must grant OS mic access.
                if not seat:seat=LiveSeat(admission=admission,mode='audio' if room.mode=='audio' else 'video')
                seat.status='approved';seat.seat_number=number;seat.save()
            else:
                if seat:
                    provider('RemoveParticipant',{'room':room_name(room),'identity':str(admission.pk)},room_name(room))
                    seat.status='rejected';seat.seat_number=None;seat.save()
        elif kind in ('force_mute_mic','release_mic_mute'):
            admission.forced_mic_mute=(kind=='force_mute_mic')
            admission.save(update_fields=['forced_mic_mute'])
            if kind=='force_mute_mic':
                # Evict to revoke a token's previous publish privileges immediately.
                provider('RemoveParticipant',{'room':room_name(room),'identity':str(admission.pk)},room_name(room))
            # Releasing does not forcibly turn on the user's hardware mic.
        room.control_revision=F('control_revision')+1
        room.save(update_fields=['control_revision'])
        if kind in ('kick_user','ban_from_host_rooms'):
            from .firestore_realtime import on_commit_room_kick
            on_commit_room_kick(room.pk,target.pk,kind)
        event(actor,kind,target.pk,reason,details={'broadcast':str(room.pk)})
        return {'ok':True,'action':kind,'user_id':target.pk}
