import base64,hashlib,json,re
from datetime import timedelta
from django.db import transaction
from django.http import JsonResponse
from django.utils import timezone
from .views import endpoint,ApiError,text
from .models import UploadSession,UploadChunk

@endpoint(['POST'])
def start(request):
    p=request.payload;purpose=p.get('purpose');size=p.get('size');digest=p.get('sha256','')
    if purpose not in ('offer','receipt','chat_image','chat_audio','chat_video','reel','avatar'):raise ApiError('invalid_purpose')
    maximum=20*1024*1024 if purpose=='reel' else 8*1024*1024
    if type(size) is not int or not 16<=size<=maximum:raise ApiError('file_too_large')
    if not isinstance(digest,str) or not re.fullmatch('[a-f0-9]{64}',digest):raise ApiError('invalid_digest')
    from .views import User
    with transaction.atomic():
        User.objects.select_for_update().get(pk=request.actor.pk)
        # Partial uploads expire; completed sessions retain idempotent retry results.
        UploadSession.objects.filter(user=request.actor,media__isnull=True,created_at__lt=timezone.now()-timedelta(days=1)).delete()
        row=UploadSession.objects.filter(user=request.actor,digest=digest,purpose=purpose).first()
        if not row:
            if UploadSession.objects.filter(user=request.actor,media__isnull=True).count()>=3:raise ApiError('upload_limit',429)
            row=UploadSession.objects.create(user=request.actor,purpose=purpose,size=size,digest=digest)
    return JsonResponse({'id':str(row.pk),'offset':row.received,'chunk_size':262144})

@endpoint(['POST'])
def chunk(request,upload_id):
    with transaction.atomic():
        row=UploadSession.objects.select_for_update().filter(pk=upload_id,user=request.actor).first()
        if not row:raise ApiError('upload_unavailable',404)
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
    with transaction.atomic():
        row=UploadSession.objects.select_for_update().filter(pk=upload_id,user=request.actor).first()
        if not row:raise ApiError('upload_unavailable',404)
        if row.media_id:return JsonResponse({'id':str(row.media_id),'url':grant_url(request,row.media),'mime':row.media.mime})
        if row.received!=row.size:raise ApiError('upload_incomplete',409)
        raw=b''.join(bytes(c.data) for c in UploadChunk.objects.filter(session=row).order_by('offset'))
        if len(raw)!=row.size or hashlib.sha256(raw).hexdigest()!=row.digest:raise ApiError('upload_checksum',409)
        request.payload={'purpose':row.purpose,'data':base64.b64encode(raw).decode()}
        from inspect import unwrap
        response=unwrap(upload)(request)
        row.media_id=json.loads(response.content)['id'];row.save(update_fields=['media'])
        UploadChunk.objects.filter(session=row).delete()
        return response
