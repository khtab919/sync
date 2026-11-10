"""Durable, account-bound push outbox. Payloads contain no message or financial details."""
import json, os
from datetime import timedelta
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from django.db import transaction
from django.http import JsonResponse
from django.utils import timezone
from .models import PushDevice, PushDelivery
from .views import endpoint, text

@endpoint(['POST','DELETE'])
def devices(request):
    if request.method=='DELETE':
        PushDevice.objects.filter(user=request.actor).update(enabled=False)
    else:
        token=text(request.payload,'token',20,4096)
        PushDevice.objects.update_or_create(token=token,defaults={'user':request.actor,'enabled':request.payload.get('enabled',True) is True})
    return JsonResponse({'ok':True})

def enqueue(user_id,key,category,target,title,seconds=86400):
    now=timezone.now()
    for device in PushDevice.objects.filter(user_id=user_id,enabled=True,user__is_active=True):
        PushDelivery.objects.get_or_create(device=device,event_key=key,defaults={'recipient_id':user_id,'category':category,'target':str(target),'title':title,'expires_at':now+timedelta(seconds=seconds)})

def send_one():
    # No credentials: leave the outbox intact and report unconfigured status.
    project=os.environ.get('FCM_PROJECT_ID','')
    if not project:return False
    now=timezone.now()
    with transaction.atomic():
        row=PushDelivery.objects.select_for_update(skip_locked=True).filter(sent_at__isnull=True,failed=False,available_at__lte=now).order_by('id').first()
        if not row:return False
        device=row.device
        if row.expires_at<=now or not device.enabled or device.user_id!=row.recipient_id or not row.recipient.is_active:
            row.failed=True;row.save(update_fields=['failed']);return True
        from .platform_features import blocked
        if row.category=='messages':
            from django.contrib.auth import get_user_model
            peer=get_user_model().objects.filter(pk=row.target).first()
            if not peer or blocked(row.recipient,peer):
                row.failed=True;row.save(update_fields=['failed']);return True
        if row.category=='calls':
            from .models import InternalCall
            if not InternalCall.objects.filter(pk=row.target,callee_id=row.recipient_id,status='ringing').exists():
                row.failed=True;row.save(update_fields=['failed']);return True
        row.attempts+=1;row.available_at=now+timedelta(minutes=2);row.save(update_fields=['attempts','available_at'])
    try:
        import google.auth
        from google.auth.transport.requests import Request as AuthRequest
        credentials,_=google.auth.default(scopes=['https://www.googleapis.com/auth/firebase.messaging'])
        credentials.refresh(AuthRequest())
        message={'token':device.token,'data':{'category':row.category,'target':row.target,'account_id':str(row.recipient_id),'title':row.title,'event_id':str(row.pk)},'android':{'priority':'HIGH','ttl':str(max(1,int((row.expires_at-timezone.now()).total_seconds())))+'s'}}
        # Data-only: Android checks the current account before showing an alert.
        req=Request('https://fcm.googleapis.com/v1/projects/'+project+'/messages:send',data=json.dumps({'message':message}).encode(),headers={'Authorization':'Bearer '+credentials.token,'Content-Type':'application/json'},method='POST')
        with urlopen(req,timeout=15) as response:response.read(65536)
        PushDelivery.objects.filter(pk=row.pk).update(sent_at=timezone.now())
    except HTTPError as error:
        detail=error.read(65536).decode('utf-8','replace')
        # INVALID_ARGUMENT can indicate a bad payload; only explicit unregistered disables a token.
        if 'UNREGISTERED' in detail:
            PushDevice.objects.filter(pk=device.pk,token=device.token).update(enabled=False)
            PushDelivery.objects.filter(pk=row.pk).update(failed=True)
        else:retry(row)
    except Exception:retry(row)
    return True

def retry(row):
    PushDelivery.objects.filter(pk=row.pk,sent_at__isnull=True).update(failed=row.attempts>=8,available_at=timezone.now()+timedelta(seconds=min(3600,2**row.attempts*10)))
