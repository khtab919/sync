"""Permanent, capped activity awards and server-authoritative frame ownership."""
from .models import ExperienceAward, BenefitGrant
from django.db.models import Sum
from django.utils import timezone
MAX_LEVEL=100
XP_PER_LEVEL=200

def profile_progress(user):
    xp=ExperienceAward.objects.filter(user=user).aggregate(n=Sum('points'))['n'] or 0
    level=min(MAX_LEVEL,1+xp//XP_PER_LEVEL)
    grants=set(BenefitGrant.objects.filter(user=user,kind='frame',expires_at__gt=timezone.now(),value__gte=1,value__lte=100).values_list('value',flat=True))
    owned=set(range(0,11)) if user.profile.legacy_frames else {0}
    if level>=2:owned.update(range(1,level+1))
    owned.update(grants)
    return {'level':level,'xp_points':xp,'next_level_xp':None if level==MAX_LEVEL else level*XP_PER_LEVEL,'owned_frames':sorted(owned)}

def can_equip_frame(user,frame_id):
    return type(frame_id) is int and 0<=frame_id<=100 and frame_id in profile_progress(user)['owned_frames']
