"""Permanent, capped activity awards and server-authoritative frame ownership."""
from .models import ExperienceAward, BenefitGrant
from django.db.models import Sum, Max
from django.utils import timezone
MAX_LEVEL=100
XP_PER_LEVEL=200

def profile_progress(user):
    xp=ExperienceAward.objects.filter(user=user).aggregate(n=Sum('points'))['n'] or 0
    earned_level=min(MAX_LEVEL,1+xp//XP_PER_LEVEL)
    now=timezone.now()
    level_award=BenefitGrant.objects.filter(user=user,kind='level',expires_at__gt=now,value__gte=1,value__lte=100).aggregate(n=Max('value'))['n'] or 0
    vip=BenefitGrant.objects.filter(user=user,kind='vip',expires_at__gt=now,value__gte=1,value__lte=5).aggregate(n=Max('value'))['n'] or 0
    level=max(earned_level,level_award)
    color=next(name for minimum,name in ((81,'royal'),(61,'diamond'),(41,'gold'),(26,'purple'),(11,'blue'),(1,'green')) if level>=minimum)
    # Preserve historical grants in storage, but expose only the current frame catalog.
    # Legacy frame IDs 1..100 must not reappear in the picker or be equipped.
    grants=set(BenefitGrant.objects.filter(user=user,kind='frame',expires_at__gt=timezone.now(),value__in=range(110, 201, 10)).values_list('value',flat=True))
    owned={0}
    owned.update(100 + milestone for milestone in range(10, 101, 10) if level >= milestone)
    owned.update(grants)
    return {'level':level,'earned_level':earned_level,'level_color':color,'vip_tier':vip,'xp_points':xp,'next_level_xp':None if earned_level==MAX_LEVEL else earned_level*XP_PER_LEVEL,'owned_frames':sorted(owned)}

def can_equip_frame(user,frame_id):
    return type(frame_id) is int and (frame_id==0 or frame_id in range(110,201,10)) and frame_id in profile_progress(user)['owned_frames']
