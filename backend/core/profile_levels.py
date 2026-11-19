"""Permanent, capped activity awards and server-authoritative frame ownership."""
from .public_presence import public_presence

MAX_LEVEL = 100
XP_PER_LEVEL = 200


def profile_progress(user):
    identity = public_presence(user)
    return {key: identity[key] for key in (
        'level', 'earned_level', 'level_color', 'vip_tier', 'xp_points',
        'next_level_xp', 'owned_frames',
    )}


def can_equip_frame(user,frame_id):
    return type(frame_id) is int and (frame_id==0 or frame_id in range(110,201,10)) and frame_id in profile_progress(user)['owned_frames']
