import base64,hashlib,json,re,uuid
from datetime import timedelta
from django.db import transaction
from django.http import JsonResponse
from django.utils import timezone
from .views import endpoint,ApiError,text
from .models import UploadSession,UploadChunk

@endpoint(['POST'])
def start(request):
    p=request.payload;purpose=p.get('purpose');size=p.get('size');digest=p.get('sha256','')
    if purpose not in ('offer','receipt','chat_image','chat_audio','chat_video','reel','avatar','paid_photo'):raise ApiError('invalid_purpose')
    maximum=96*1024*1024 if purpose=='reel' else 8*1024*1024
    if type(size) is not int or not 16<=size<=maximum:raise ApiError('file_too_large')
    if not isinstance(digest,str) or not re.fullmatch('[a-f0-9]{64}',digest):raise ApiError('invalid_digest')
    key=uuid.UUID(request.payload['request_key']) if request.payload.get('request_key') else uuid.uuid5(uuid.NAMESPACE_URL,purpose+':'+digest)
    start=p.get("clip_start",0);end=p.get("clip_end",0)
    if type(start) is not int or type(end) is not int or start<0 or end<0 or max(start,end)>600 or (end and end<=start) or ((start or end) and purpose!="reel"):
        raise ApiError("invalid_clip")
    authorize_purpose(request.actor,purpose)
    from .views import User
    with transaction.atomic():
        User.objects.select_for_update().get(pk=request.actor.pk)
        # Partial uploads expire; completed sessions retain idempotent retry results.
        UploadSession.objects.filter(user=request.actor,media__isnull=True,created_at__lt=timezone.now()-timedelta(days=1)).delete()
        row=UploadSession.objects.filter(user=request.actor,request_key=key).first()
        if row and (row.digest!=digest or row.purpose!=purpose or row.size!=size or row.clip_start!=start or row.clip_end!=end):raise ApiError('upload_request_conflict',409)
        if row and row.processing=="failed":
            row.received=0
            row.processing=""
            row.save(update_fields=["received","processing"])
        if not row:
            if UploadSession.objects.filter(user=request.actor,media__isnull=True).count()>=3:raise ApiError('upload_limit',429)
            row=UploadSession.objects.create(user=request.actor,purpose=purpose,size=size,digest=digest,clip_start=start,clip_end=end,request_key=key)
    return JsonResponse({'id':str(row.pk),'offset':row.received,'chunk_size':262144})

@endpoint(['POST'])
def chunk(request,upload_id):
    with transaction.atomic():
        row=UploadSession.objects.select_for_update().filter(pk=upload_id,user=request.actor).first()
        if not row:raise ApiError('upload_unavailable',404)
        authorize_purpose(request.actor,row.purpose)
        if row.created_at<timezone.now()-timedelta(days=1) and not row.media_id:raise ApiError('upload_expired',409)
        if row.media_id:return JsonResponse({'offset':row.received})
        p=request.payload;offset=p.get('offset');raw=base64.b64decode(text(p,'data',4,350000),validate=True)
        if type(offset) is not int or not raw or len(raw)>262144 or offset+len(raw)>row.size:raise ApiError('invalid_chunk')
        old=UploadChunk.objects.filter(session=row,offset=offset).first()
        if old:
            if bytes(old.data)!=raw:raise ApiError('chunk_conflict',409)
        else:
            if offset!=row.received:raise ApiError('offset_conflict',409)
            UploadChunk.objects.create(session=row,offset=offset,data=raw)
            row.received+=len(raw);row.save(update_fields=['received'])
        return JsonResponse({'offset':row.received})

@endpoint(['POST'])
def finish(request,upload_id):
    from .commerce import upload,grant_url
    current=UploadSession.objects.filter(pk=upload_id,user=request.actor).first()
    if current and current.processing=="queued" and not current.media_id:
        return JsonResponse({"processing":True},status=202)
    if current and current.processing=="failed":
        raise ApiError("video_processing_failed",422)
    with transaction.atomic():
        row=UploadSession.objects.select_for_update().filter(pk=upload_id,user=request.actor).first()
        if not row:raise ApiError('upload_unavailable',404)
        authorize_purpose(request.actor,row.purpose)
        if row.media_id:return JsonResponse({'id':str(row.media_id),'url':grant_url(request,row.media),'mime':row.media.mime})
        if row.received!=row.size:raise ApiError('upload_incomplete',409)
        if row.purpose=="reel" and (row.size>20*1024*1024 or row.clip_start or row.clip_end):
            row.processing="queued";row.save(update_fields=["processing"])
            return JsonResponse({"processing":True},status=202)
        raw=b''.join(bytes(c.data) for c in UploadChunk.objects.filter(session=row).order_by('offset'))
        if len(raw)!=row.size or hashlib.sha256(raw).hexdigest()!=row.digest:raise ApiError('upload_checksum',409)
        request.payload={'purpose':row.purpose,'data':base64.b64encode(raw).decode()}
        from inspect import unwrap
        response=unwrap(upload)(request)
        row.media_id=json.loads(response.content)['id'];row.save(update_fields=['media'])
        UploadChunk.objects.filter(session=row).delete()
        return response


def authorize_purpose(user,purpose):
    from .models import Profile,AdminFeature
    from .admin_security import blocked
    if purpose in ('offer','reel','paid_photo'):
        if blocked(user,'store'):raise ApiError('feature_restricted',403)
        if AdminFeature.objects.filter(key='store',enabled=False).exists():raise ApiError('feature_maintenance',503)
        if not Profile.objects.filter(user=user,publishing_allowed=True).exists():raise ApiError('creator_account_required',403)
    if purpose.startswith('chat_'):
        if blocked(user,'messages'):raise ApiError('feature_restricted',403)
        if AdminFeature.objects.filter(key='messages',enabled=False).exists():raise ApiError('feature_maintenance',503)
