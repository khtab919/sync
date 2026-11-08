"""Server-authoritative user levels and cosmetic frame entitlements.

Levels use persisted, real activity. No client-supplied XP, demo stats, or
monetary balances count as experience. Existing frames 0-10 remain usable to
avoid removing previously available profile customizations.
"""
from .models import Reel, Service, Booking, LiveBroadcast, BenefitGrant
from django.utils import timezone

MAX_LEVEL = 100
XP_PER_LEVEL = 200


def profile_progress(user):
    reels = Reel.objects.filter(owner=user, approved=True).count()
    services = Service.objects.filter(owner=user, approved=True, active=True).count()
    completed_bookings = Booking.objects.filter(
        service__owner=user, status='completed'
    ).count()
    completed_lives = LiveBroadcast.objects.filter(
        owner=user, status='ended', started_at__isnull=False
    ).count()
    xp = reels * 30 + services * 15 + completed_bookings * 80 + completed_lives * 40
    level = min(MAX_LEVEL, 1 + xp // XP_PER_LEVEL)
    next_xp = min(MAX_LEVEL - 1, level) * XP_PER_LEVEL
    grants = set(BenefitGrant.objects.filter(
        user=user, kind='frame', expires_at__gt=timezone.now(),
        value__gte=1, value__lte=100,
    ).values_list('value', flat=True))
    owned = sorted(set(range(0, min(10, level) + 1)) | set(range(0, 11)) |
                   set(range(11, level + 1)) | grants)
    return {'level': level, 'xp_points': xp, 'next_level_xp': next_xp,
            'owned_frames': owned}


def can_equip_frame(user, frame_id):
    return (type(frame_id) is int and 0 <= frame_id <= 100
            and frame_id in profile_progress(user)['owned_frames'])
