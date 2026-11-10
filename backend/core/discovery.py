from django.db import transaction
from django.db.models import Sum,Count
from django.http import JsonResponse,HttpResponse
from .models import Reel,ReelPreference,ReelLike
from .views import endpoint,ApiError
from .paging import visible_owners

@endpoint(['POST'])
def reel_action(request,reel_id):
    reel=Reel.objects.filter(pk=reel_id,approved=True,owner__is_active=True).exclude(owner_id__in=visible_owners(request.actor)).first()
    if not reel:raise ApiError('reel_unavailable',404)
    with transaction.atomic():
        row,_=ReelPreference.objects.get_or_create(user=request.actor,reel=reel)
        row=ReelPreference.objects.select_for_update().get(pk=row.pk)
        action=request.payload.get('action')
        if action in ('saved','hidden'):
            if type(request.payload.get('enabled')) is not bool:raise ApiError('invalid_input')
            setattr(row,action,request.payload['enabled'])
        elif action=='watch':
            seconds=request.payload.get('seconds')
            if type(seconds) is not int or not 0<=seconds<=180:raise ApiError('invalid_input')
            row.watch_seconds=max(row.watch_seconds,seconds)
        else:raise ApiError('invalid_action')
        row.save()
    return JsonResponse({'saved':row.saved,'hidden':row.hidden})

@endpoint(['GET'])
def insights(request):
    rows=Reel.objects.filter(owner=request.actor)
    engagement=ReelPreference.objects.filter(reel__in=rows).exclude(user=request.actor)
    return JsonResponse({'reels':rows.count(),'likes':ReelLike.objects.filter(reel__in=rows).exclude(user=request.actor).count(),'viewers':engagement.filter(watch_seconds__gte=3).values('user_id').distinct().count(),'watch_seconds':engagement.aggregate(n=Sum('watch_seconds'))['n'] or 0,'measurement':'Unique signed-in viewers; watch time is client-reported, capped per viewer and reel. Not used for money or XP.'})

def share(request,kind,target):
    # A neutral landing page; private content still requires app authentication.
    if kind not in ('reel','profile','live'):return HttpResponse(status=404)
    from django.utils.html import escape
    import re
    if not re.fullmatch('[a-zA-Z0-9-]{1,100}',target):return HttpResponse(status=404)
    url='intent://'+kind+'/'+target+'#Intent;scheme=relaxia;package=com.aistudio.layalilounge.x9m4kp;end'
    return HttpResponse('<!doctype html><html lang="ar" dir="rtl"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Relaxia</title><body style="font-family:sans-serif;text-align:center;padding:48px;background:#f5f7fc;color:#173477"><h1>Relaxia</h1><p>افتح الرابط في التطبيق لعرض المحتوى بعد تسجيل الدخول.</p><p><a href="'+escape(url)+'">فتح Relaxia</a></p><p>يتطلب تثبيت التطبيق على هاتف أندرويد.</p></body></html>')
