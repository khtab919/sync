"""Rank approved feed items by real engagement with author diversity.

No fake impressions, purchases or likes. Scores are computed on request from
verified likes, completed bookings, followed creators, and recency; the small
stable exploration term prevents an all-new feed becoming strictly chronological.
"""
import hashlib
import math
from datetime import timedelta
from django.utils import timezone


def rank_feed(rows, *, likes=None, bookings=None, following=None, viewer_id=0):
    likes = likes or {}
    bookings = bookings or {}
    following = set(following or ())
    now = timezone.now()
    slot = int(now.timestamp() // (6 * 3600))
    ranked = []
    for item in rows:
        age_hours = max(0.0, (now - item.created_at).total_seconds() / 3600)
        freshness = max(0.0, 7.0 - age_hours / 18.0)
        engagement = 9.0 * math.log1p(likes.get(item.pk, 0))
        conversions = 14.0 * math.log1p(bookings.get(item.pk, 0))
        followed = 3.0 if item.owner_id in following else 0.0
        # Stable during a six-hour slot, varying per viewer; no random sort.
        digest = hashlib.blake2s(
            f'{slot}:{viewer_id}:{item.pk}'.encode(), digest_size=4
        ).digest()
        exploration = int.from_bytes(digest, 'big') / (2 ** 32) * 2.5
        ranked.append((engagement + conversions + freshness + followed + exploration, item))
    ranked.sort(key=lambda pair: (-pair[0], -pair[1].created_at.timestamp(), -pair[1].pk))

    result = []
    while ranked:
        selected = 0
        if result and ranked[0][1].owner_id == result[-1].owner_id:
            # A nearby candidate from another author gets the next slot when
            # its performance is comparable; excellent posts still rank high.
            for i, (score, item) in enumerate(ranked[1:16], start=1):
                if item.owner_id != result[-1].owner_id and ranked[0][0] - score <= 8.0:
                    selected = i
                    break
        result.append(ranked.pop(selected)[1])
    return result
