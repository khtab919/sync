"""Two-party WebRTC calls. Media is never posted to Django or recorded."""
import base64
import hashlib
import hmac
import json
import os
import secrets
import time
from datetime import timedelta
from django.core import signing
from django.db import transaction
from django.db.models import Q
from django.http import JsonResponse, HttpResponse
from django.shortcuts import render
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from .views import endpoint, ApiError, text, amount, User
from .models import InternalCall, CallSignal, CallTicket, Message, Profile, SessionToken

LIVE = ['ringing', 'accepted']


def available():
    return bool(os.environ.get('LAYALI_TURN_SECRET') and not os.environ['LAYALI_TURN_SECRET'].startswith('replace-') and os.environ.get('LAYALI_TURN_HOST'))


def expire_calls():
    now = timezone.now()
    InternalCall.objects.filter(status='ringing', created_at__lt=now-timedelta(seconds=60)).update(status='missed', ended_at=now)
    stale = Q(caller_seen_at__lt=now-timedelta(seconds=45)) | Q(callee_seen_at__lt=now-timedelta(seconds=45))
    InternalCall.objects.filter(stale, status='accepted').update(status='ended', ended_at=now)
    InternalCall.objects.filter(status__in=LIVE, created_at__lt=now-timedelta(hours=2)).update(status='ended', ended_at=now)
    CallSignal.objects.exclude(call__status__in=LIVE).delete()


def get_call(user, call_id, locked=False):
    query = InternalCall.objects.select_for_update() if locked else InternalCall.objects
    row = query.filter(Q(caller=user) | Q(callee=user), pk=call_id).select_related('caller', 'callee').first()
    if row is None:
        raise ApiError('call_unavailable', 404)
    if not row.caller.is_active or not row.callee.is_active:
        raise ApiError('call_unavailable', 403)
    return row


def call_json(row):
    return {'id': row.pk, 'caller_id': row.caller_id, 'callee_id': row.callee_id, 'caller_name':getattr(getattr(row.caller,'profile',None),'display_name','') or row.caller.username, 'callee_name':getattr(getattr(row.callee,'profile',None),'display_name','') or row.callee.username, 'mode': row.mode, 'status': row.status, 'ringing_at':row.ringing_at, 'caller_avatar':getattr(getattr(row.caller,'profile',None),'avatar_url',''), 'callee_avatar':getattr(getattr(row.callee,'profile',None),'avatar_url','')}


@endpoint(['GET', 'POST'])
def calls(request):
    expire_calls()
    if request.method == 'GET':
        incoming = InternalCall.objects.filter(callee=request.actor, status='ringing', caller__is_active=True).select_related('caller', 'callee').first()
        if incoming and incoming.ringing_at is None:
            incoming.ringing_at=timezone.now(); incoming.save(update_fields=['ringing_at'])
        return JsonResponse({'available': available(), 'incoming': call_json(incoming) if incoming else None})
    if not available():
        raise ApiError('calls_setup_required', 503)
    mode = request.payload.get('mode', 'audio')
    if mode not in ('audio', 'video'):
        raise ApiError('invalid_call_mode')
    peer_id = amount(request.payload, 'peer_id')
    if peer_id == request.actor.pk:
        raise ApiError('recipient_unavailable', 404)
    with transaction.atomic():
        people = {u.pk: u for u in User.objects.select_for_update().filter(pk__in=sorted([request.actor.pk, peer_id])).order_by('pk')}
        peer = people.get(peer_id)
        if not peer or not peer.is_active or (peer.is_staff and peer.is_superuser) or not Profile.objects.filter(user=peer).exists():
            raise ApiError('recipient_unavailable', 404)
        if Profile.objects.filter(user=peer, chat_price_cents__gt=0).exists():
            raise ApiError('paid_chat_requires_verified_payment', 409)
        existing = InternalCall.objects.filter(status__in=LIVE).filter(Q(caller_id__in=people) | Q(callee_id__in=people)).first()
        if existing:
            if existing.caller_id == request.actor.pk and existing.callee_id == peer.pk and existing.mode == mode:
                return JsonResponse(call_json(existing))
            raise ApiError('call_busy', 409)
        if InternalCall.objects.filter(caller=request.actor, created_at__gt=timezone.now()-timedelta(minutes=1)).count() >= 5:
            raise ApiError('too_many_calls', 429)
        row = InternalCall.objects.create(caller=request.actor, callee=peer, mode=mode)
        Message.objects.create(sender=request.actor, recipient=peer, call=row, body='مكالمة فيديو' if mode == 'video' else 'مكالمة صوتية')
    return JsonResponse(call_json(row), status=201)


@endpoint(['GET', 'POST'])
def call_detail(request, call_id):
    expire_calls()
    with transaction.atomic():
        row = get_call(request.actor, call_id, locked=True)
        if request.method == 'POST':
            action = request.payload.get('action')
            if action not in ('accept', 'reject', 'end'):
                raise ApiError('invalid_action')
            if action in ('accept', 'reject') and request.actor.pk != row.callee_id:
                raise ApiError('call_action_forbidden', 403)
            if action == 'accept':
                if row.status not in LIVE:
                    raise ApiError('call_finished', 409)
                if row.status == 'ringing':
                    row.status, row.accepted_at = 'accepted', timezone.now()
                    row.caller_seen_at = row.callee_seen_at = timezone.now()
                    row.save(update_fields=['status', 'accepted_at', 'caller_seen_at', 'callee_seen_at'])
            elif row.status in LIVE:
                row.status = 'rejected' if action == 'reject' else 'ended'
                row.ended_at = timezone.now()
                row.save(update_fields=['status', 'ended_at'])
    return JsonResponse(call_json(row))


@endpoint(['POST'])
def ticket(request, call_id):
    expire_calls()
    row = get_call(request.actor, call_id)
    if row.status not in LIVE or (request.actor.pk == row.callee_id and row.status != 'accepted'):
        raise ApiError('call_not_accepted', 409)
    raw = secrets.token_urlsafe(48)
    CallTicket.objects.filter(expires_at__lt=timezone.now()).delete()
    CallTicket.objects.create(digest=hashlib.sha256(raw.encode()).hexdigest(), call=row, session=request.api_token, expires_at=timezone.now()+timedelta(seconds=60))
    return JsonResponse({'url': request.build_absolute_uri(f'/api/calls/{row.pk}/room'), 'ticket': raw})


def ice_servers(user, call_id):
    host = os.environ['LAYALI_TURN_HOST']
    username = f'{int(time.time()) + 7200}:{user.pk}:{call_id}'
    credential = base64.b64encode(hmac.new(os.environ['LAYALI_TURN_SECRET'].encode(), username.encode(), hashlib.sha1).digest()).decode()
    return [{'urls': [f'stun:{host}:3478']}, {'urls': [f'turn:{host}:3478?transport=udp', f'turn:{host}:3478?transport=tcp'], 'username': username, 'credential': credential}]


@csrf_exempt
def room(request, call_id):
    if request.method != 'POST' or not available():
        return HttpResponse(status=405 if request.method != 'POST' else 503)
    expire_calls()
    raw = request.POST.get('ticket', '')
    if not isinstance(raw, str) or not 20 <= len(raw) <= 100:
        return HttpResponse(status=403)
    with transaction.atomic():
        row = CallTicket.objects.select_for_update().filter(digest=hashlib.sha256(raw.encode()).hexdigest(), call_id=call_id, used_at__isnull=True, expires_at__gt=timezone.now(), session__expires_at__gt=timezone.now(), session__user__is_active=True).select_related('session__user').first()
        if row is None:
            return HttpResponse(status=403)
        user = row.session.user
        try:
            call = get_call(user, call_id)
        except ApiError:
            return HttpResponse(status=403)
        if call.status not in LIVE or (user.pk == call.callee_id and call.status != 'accepted'):
            return HttpResponse(status=409)
        row.used_at = timezone.now(); row.save(update_fields=['used_at'])
    token = signing.dumps({'call': str(call.pk), 'user': user.pk, 'session': row.session_id}, salt='internal-call')
    nonce = secrets.token_urlsafe(24)
    peer = call.callee if user.pk == call.caller_id else call.caller
    peer_profile = Profile.objects.filter(user=peer).first()
    config = {'peerAvatar':peer_profile.avatar_url if peer_profile else '', 'peerName': (peer_profile.display_name if peer_profile else '') or peer.username, 'call': str(call.pk), 'caller': user.pk == call.caller_id, 'video': call.mode == 'video', 'token': token, 'iceServers': ice_servers(user, call.pk), 'relayOnly': os.environ.get('LAYALI_TESTING') == '1' and os.environ.get('LAYALI_CALL_TEST_RELAY_ONLY') == '1'}
    response = render(request, 'core/call_room.html', {'config': config, 'nonce': nonce})
    response['Cache-Control'] = 'no-store'
    response['Referrer-Policy'] = 'no-referrer'
    response['Content-Security-Policy'] = f"default-src 'none'; script-src 'nonce-{nonce}'; style-src 'unsafe-inline'; media-src 'self' blob:; connect-src 'self'; img-src 'self' data: https:; frame-ancestors 'none'; base-uri 'none'"
    return response


@csrf_exempt
def signal(request, call_id):
    if request.method not in ('GET', 'POST'):
        return JsonResponse({'error': 'method_not_allowed'}, status=405)
    try:
        data = signing.loads(request.headers.get('X-Call-Token', ''), salt='internal-call', max_age=7200)
        if data.get('call') != str(call_id):
            raise ApiError('call_token_invalid', 403)
        session = SessionToken.objects.select_related('user').filter(pk=data.get('session'), user_id=data.get('user'), expires_at__gt=timezone.now(), user__is_active=True).first()
        if session is None:
            raise ApiError('invalid_session', 401)
        expire_calls()
        with transaction.atomic():
            call = get_call(session.user, call_id, locked=True)
            if call.status not in LIVE:
                return JsonResponse({'status': call.status, 'events': []})
            field = 'caller_seen_at' if session.user.pk == call.caller_id else 'callee_seen_at'
            setattr(call, field, timezone.now()); call.save(update_fields=[field])
            if request.method == 'POST':
                if request.content_type != 'application/json' or len(request.body) > 65536:
                    raise ApiError('invalid_signal')
                p = json.loads(request.body)
                if not isinstance(p, dict) or p.get('kind') not in ('offer', 'answer', 'ice', 'reconnect', 'hangup') or not isinstance(p.get('data'), dict):
                    raise ApiError('invalid_signal')
                kind, payload = p['kind'], p['data']
                if (kind == 'offer' and call.caller_id != session.user.pk) or (kind == 'answer' and call.callee_id != session.user.pk):
                    raise ApiError('call_action_forbidden', 403)
                if kind == 'hangup':
                    call.status='ended';call.ended_at=timezone.now();call.save(update_fields=['status','ended_at'])
                    return JsonResponse({'ok':True})
                revision=payload.get('revision',0)
                if type(revision) is not int or not 0<=revision<=8:raise ApiError('invalid_signal')
                if kind in ('offer', 'answer'):
                    if payload.get('type') != kind or not isinstance(payload.get('sdp'), str) or not 1 <= len(payload['sdp']) <= 60000:
                        raise ApiError('invalid_signal')
                    prior=CallSignal.objects.filter(call=call,kind=kind)
                    for previous in prior:
                        if previous.data.get('revision',0)==revision:
                            if previous.data==payload:return JsonResponse({'ok':True})
                            raise ApiError('signal_already_sent',409)
                    if revision>0 and call.status!='accepted':raise ApiError('call_not_accepted',409)
                    if kind=='offer' and revision>0 and not prior.filter(data__revision=revision-1).exists():
                        if revision!=1 or not prior.exists():raise ApiError('invalid_signal')
                elif kind == 'ice':
                    if not isinstance(payload.get('candidate'), str) or len(payload['candidate']) > 4096:
                        raise ApiError('invalid_signal')
                    if payload.get('sdpMid') is not None and not isinstance(payload['sdpMid'], str):
                        raise ApiError('invalid_signal')
                    if payload.get('sdpMLineIndex') is not None and type(payload['sdpMLineIndex']) is not int:
                        raise ApiError('invalid_signal')
                if CallSignal.objects.filter(call=call).count() >= 2000:
                    raise ApiError('signal_limit', 429)
                CallSignal.objects.create(call=call, sender=session.user, kind=kind, data=payload)
                return JsonResponse({'ok': True}, status=201)
            cursor = max(0, int(request.GET.get('after', '0')))
            events = list(CallSignal.objects.filter(call=call, id__gt=cursor).exclude(sender=session.user).order_by('id').values('id', 'kind', 'data')[:100])
        return JsonResponse({'status': call.status, 'ringing_at':call.ringing_at, 'events': events})
    except (signing.BadSignature, ValueError, TypeError, KeyError):
        return JsonResponse({'error': 'call_token_invalid'}, status=403)
    except ApiError as exc:
        return JsonResponse({'error': exc.message}, status=exc.status)
