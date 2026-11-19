"""One authoritative public identity snapshot for profile, room seats and chat.

All fields come from the Django user/profile, XP awards and active grants.
No locally generated level, frame or avatar is ever serialized as user data.
"""
from collections import defaultdict
from django.db.models import Sum
from django.utils import timezone
from .models import ExperienceAward, BenefitGrant, Profile

def public_presence_bulk(users):
    by_id={u.pk:u for u in users if u is not None and u.pk}
    if not by_id:return {}
    ids=list(by_id)
    xp_by_id=dict(ExperienceAward.objects.filter(user_id__in=ids)
        .values('user_id').annotate(total=Sum('points'))
        .values_list('user_id','total'))
    grants=BenefitGrant.objects.filter(user_id__in=ids,
        kind__in=('level','vip','frame'),expires_at__gt=timezone.now()
    ).order_by('id').values('user_id','kind','value')
    levels=defaultdict(int)
    vip=defaultdict(int)
    owned_grant_frames=defaultdict(set)
    newest_frame={}
    for grant in grants:
        uid,kind,value=grant['user_id'],grant['kind'],grant['value']
        if kind=='level' and 1<=value<=100:levels[uid]=max(levels[uid],value)
        elif kind=='vip' and 1<=value<=5:vip[uid]=max(vip[uid],value)
        elif kind=='frame' and value in range(110,201,10):
            owned_grant_frames[uid].add(value)
            newest_frame[uid]=value
    profiles=Profile.objects.filter(user_id__in=ids)
    profile_map={p.user_id:p for p in profiles}
    result={}
    for uid,user in by_id.items():
        profile=profile_map.get(uid)
        xp=xp_by_id.get(uid) or 0
        earned=min(100,1+xp//200)
        level=max(earned,levels[uid])
        color=next(name for minimum,name in (
            (81,'royal'),(61,'diamond'),(41,'gold'),(26,'purple'),(11,'blue'),(1,'green')
        ) if level>=minimum)
        owned_frames={0}|{100+n for n in range(10,101,10) if level>=n}|owned_grant_frames[uid]
        selected=0 if not profile else newest_frame.get(uid,profile.selected_frame)
        if selected not in owned_frames:selected=0
        result[uid]={
            'user_id':uid,
            'name':(profile.display_name if profile else '') or user.username,
            'public_id':profile.public_id if profile else '',
            'avatar_url':profile.avatar_url if profile else '',
            'selected_frame':selected,
            'level':level,
            'earned_level':earned,
            'level_color':color,
            'vip_tier':vip[uid],
            'identity_verified':bool(profile and profile.identity_verified),
            'xp_points':xp,
            'next_level_xp':None if earned==100 else earned*200,
        }
    return result

def public_presence(user):
    return public_presence_bulk([user])[user.pk]
