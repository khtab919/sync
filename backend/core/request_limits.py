"""Mutation limits keyed by account; media chunks and delivery acknowledgements are separate."""
import hashlib
from datetime import timedelta
from django.db import transaction
from django.utils import timezone
from .models import AuthRateLimit

def enforce(request):
    if request.method!='POST':return
    path=request.path
    cap=10 if path in ('/api/reels','/api/services','/api/uploads','/api/reports','/api/account/security') else 60 if path.startswith('/api/messages/') and path.count('/')==3 else None
    if cap is None:return
    from .views import ApiError
    key=hashlib.sha256(('mutation:'+str(request.actor.pk)+':'+path).encode()).hexdigest();now=timezone.now()
    with transaction.atomic():
        AuthRateLimit.objects.get_or_create(key=key,defaults={'window_start':now})
        row=AuthRateLimit.objects.select_for_update().get(key=key)
        if now-row.window_start>=timedelta(minutes=1):row.window_start=now;row.attempts=0
        row.attempts+=1;row.save()
        exceeded=row.attempts>cap
    if exceeded:raise ApiError('too_many_attempts',429)
