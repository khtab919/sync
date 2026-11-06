"""Native administrator API. Permissions are checked for every request."""
from django.contrib.admin.models import LogEntry, CHANGE
from django.db import transaction
from django.db.models import Q
from django.http import JsonResponse
from .views import endpoint, ApiError, User, text, amount
from .models import Profile, Wallet, SessionToken, Service, Reel, BalanceAdjustment


def require_admin(request):
    if not request.actor.is_staff or not request.actor.is_superuser:
        raise ApiError('administrator_required', 403)


def audit(actor, obj, message):
    LogEntry.objects.log_actions(user_id=actor.pk, queryset=[obj], action_flag=CHANGE, change_message=message)


def account_json(user):
    profile, _ = Profile.objects.get_or_create(user=user)
    wallet, _ = Wallet.objects.get_or_create(user=user)
    return {'id': user.pk, 'username': user.username, 'email': user.email, 'display_name': profile.display_name or user.username,
            'bio': profile.bio, 'avatar_url': profile.avatar_url, 'selected_frame': profile.selected_frame,
            'active': user.is_active, 'creator': profile.creator, 'protected': user.is_superuser,
            'balance_cents': wallet.balance_cents, 'reserved_cents': wallet.reserved_cents}


@endpoint(['GET'])
def accounts(request):
    require_admin(request)
    query = request.GET.get('q', '').strip()[:100]
    try:
        offset = max(0, min(int(request.GET.get('offset', 0)), 1000000))
    except ValueError:
        raise ApiError('invalid_offset')
    users = User.objects.order_by('-id')
    if query:
        users = users.filter(Q(username__icontains=query) | Q(email__icontains=query))
    return JsonResponse({'items': [account_json(u) for u in users[offset:offset + 50]], 'total': users.count(), 'offset': offset})


@endpoint(['GET', 'POST'])
def account(request, user_id):
    require_admin(request)
    user = User.objects.filter(pk=user_id).first()
    if not user:
        raise ApiError('account_unavailable', 404)
    if request.method == 'POST':
        action = request.payload.get('action')
        if action not in ('ban', 'unban', 'creator_on', 'creator_off', 'credit'):
            raise ApiError('invalid_action')
        with transaction.atomic():
            locked = {u.pk: u for u in User.objects.select_for_update().filter(pk__in=sorted({user_id, request.actor.pk})).order_by("pk")}
            user = locked[user_id]
            if action in ('ban', 'unban', 'creator_on', 'creator_off') and (user.is_superuser or user.pk == request.actor.pk):
                raise ApiError('protected_account', 403)
            if action in ('ban', 'unban'):
                user.is_active = action == 'unban'
                user.save(update_fields=['is_active'])
                if action == 'ban':
                    SessionToken.objects.filter(user=user).delete()
            elif action in ('creator_on', 'creator_off'):
                profile, _ = Profile.objects.get_or_create(user=user)
                profile.creator = action == 'creator_on'
                profile.save(update_fields=['creator'])
            else:
                cents = amount(request.payload, 'amount_cents')
                reason = text(request.payload, 'reason', 3, 300)
                # A retry must not credit the same request twice. The same admin/request
                # pair is serialized by the administrator row lock in every credit request.
                request_key = text(request.payload, 'request_key', 36, 36)
                import uuid
                try:
                    request_key = str(uuid.UUID(request_key))
                except ValueError:
                    raise ApiError('invalid_request_key')
                previous = BalanceAdjustment.objects.filter(administrator=request.actor, request_key=request_key).first()
                if previous:
                    if previous.user_id != user.pk or previous.amount_cents != cents or previous.reason != reason:
                        raise ApiError('credit_request_conflict', 409)
                else:
                    Wallet.objects.get_or_create(user=user)
                    wallet = Wallet.objects.select_for_update().get(user=user)
                    wallet.balance_cents += cents
                    wallet.save(update_fields=['balance_cents'])
                    BalanceAdjustment.objects.create(user=user, administrator=request.actor, amount_cents=cents, reason=reason, request_key=request_key)
                    audit(request.actor, user, f'Credit {cents} USD cents: {reason}')
            if action != 'credit':
                audit(request.actor, user, action)
    data = account_json(user)
    data['services'] = list(Service.objects.filter(owner=user).values('id', 'title', 'approved', 'active')[:100])
    data['reels'] = list(Reel.objects.filter(owner=user).values('id', 'approved')[:100])
    return JsonResponse(data)


@endpoint(['GET', 'POST'])
def moderation(request):
    require_admin(request)
    if request.method == 'POST':
        kind = request.payload.get('kind')
        approved = request.payload.get('approved')
        if kind not in ('service', 'reel') or type(approved) is not bool:
            raise ApiError('invalid_action')
        model = Service if kind == 'service' else Reel
        with transaction.atomic():
            obj = model.objects.select_for_update().filter(pk=amount(request.payload, 'id')).first()
            if not obj:
                raise ApiError('content_unavailable', 404)
            obj.approved = approved
            obj.save(update_fields=['approved'])
            audit(request.actor, obj, 'approved' if approved else 'hidden')
    services = Service.objects.exclude(category='كورس').select_related('owner').order_by('approved', '-id')[:100]
    reels = Reel.objects.select_related('owner').order_by('approved', '-id')[:100]
    from .commerce import grant_url
    return JsonResponse({'items': [dict(id=o.pk, kind='service', title=o.title, owner=o.owner.username, approved=o.approved, url=grant_url(request, o.image) if o.image_id else o.image_url) for o in services] + [dict(id=o.pk, kind='reel', title='ريلز #' + str(o.pk), owner=o.owner.username, approved=o.approved, url=grant_url(request, o.media) if o.media_id else o.video_url) for o in reels]})
