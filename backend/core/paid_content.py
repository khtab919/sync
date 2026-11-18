"""Paid photos and reels. Original bytes never appear in an unpaid feed."""
import io
import os
import subprocess
import uuid
from django.core.files.base import ContentFile
from django.db import transaction
from django.http import JsonResponse
from django.db.models import Q
from PIL import Image, ImageFilter, ImageOps, UnidentifiedImageError
from .views import endpoint, ApiError, text, amount, User, profile_json, service_json
from .models import (
    PaidPost, Reel, ContentUnlock, UploadedMedia, Profile,
    Service, UserBlock, FinancialOperation
)
from .paging import visible_owners
from .commerce import grant_url


def _photo_preview(media):
    """Create a small downscaled and *heavily* blurred, non-reversible public JPG."""
    try:
        if media.mime.startswith('image/'):
            with media.file.open('rb') as f:
                img=Image.open(f)
                img.load()
        elif media.mime=='video/mp4':
            # ffmpeg ships in the project's Dockerfile. Never a client URL.
            with media.file.open('rb') as f:
                raw=f.read(21*1024*1024)
            if len(raw)>20*1024*1024:
                raise ValueError('oversize_video')
            result=subprocess.run([
                'ffmpeg','-nostdin','-v','error','-ss','0.5','-i','pipe:0',
                '-frames:v','1','-vf','scale=480:-2','-f','image2pipe',
                '-vcodec','mjpeg','pipe:1'
            ],input=raw,capture_output=True,timeout=20,check=True)
            if len(result.stdout)>4*1024*1024:
                raise ValueError('preview_too_large')
            img=Image.open(io.BytesIO(result.stdout))
            img.load()
        else:
            raise ValueError('invalid_preview_type')
        img=ImageOps.exif_transpose(img).convert('RGB')
        img.thumbnail((360,520),Image.Resampling.LANCZOS)
        img=img.filter(ImageFilter.GaussianBlur(radius=28))
        img=img.resize((max(img.width,480),max(img.height,480)))
        out=io.BytesIO()
        img.save(out,format='JPEG',quality=65,optimize=True)
        data=out.getvalue()
        if len(data)>250000:
            raise ValueError('preview_too_large')
    except (ValueError,UnidentifiedImageError,OSError,subprocess.SubprocessError):
        raise ApiError('blurred_preview_unavailable',503)
    preview=UploadedMedia(owner=media.owner,purpose='paid_preview',mime='image/jpeg',size=len(data))
    preview.file.save(uuid.uuid4().hex+'.jpg',ContentFile(data),save=False)
    preview.save()
    return preview


def _content_access(user,owner,content):
    if user.pk==owner.pk:return True
    from .admin_security import blocked
    if blocked(owner,'ban') or blocked(user,'ban'):return False
    if UserBlock.objects.filter(Q(user=user,blocked=owner)|Q(user=owner,blocked=user)).exists():return False
    if isinstance(content,Reel):
        return ContentUnlock.objects.filter(buyer=user,reel=content,operation__status='settled').exists()
    return ContentUnlock.objects.filter(buyer=user,post=content,operation__status='settled').exists()


def post_json(row,request):
    unlocked=_content_access(request.actor,row.owner,row)
    return {
        'type':'photo','id':row.pk,'owner_id':row.owner_id,
        'caption':row.caption,'price_cents':row.price_cents,
        'locked':not unlocked,
        'preview_url':request.build_absolute_uri('/api/media/'+str(row.preview_id)),
        'media_url':grant_url(request,row.media) if unlocked else '',
        'created_at':row.created_at.isoformat(),
    }


def reel_json(row,request):
    unlocked=row.price_cents<=0 or _content_access(request.actor,row.owner,row)
    return {
        'id':row.pk,'owner_id':row.owner_id,'caption':row.caption,
        'price_cents':row.price_cents,'locked':not unlocked,
        'preview_url':request.build_absolute_uri('/api/media/'+str(row.preview_id)) if row.preview_id else '',
        'video_url':grant_url(request,row.media) if unlocked and row.media_id else (row.video_url if unlocked else ''),
    }


@endpoint(['GET','POST'])
def paid_posts(request):
    if request.method=='GET':
        owner=request.GET.get('owner')
        queryset=PaidPost.objects.filter(active=True,owner__is_active=True)
        if owner:
            owner_id=int(owner)
            if owner_id in visible_owners(request.actor):
                return JsonResponse({'items':[]})
            queryset=queryset.filter(owner_id=owner_id)
        else:
            queryset=queryset.exclude(owner_id__in=visible_owners(request.actor))
        posts=list(queryset.select_related('owner','media','preview').order_by('-created_at')[:100])
        return JsonResponse({'items':[post_json(p,request) for p in posts]})
    if not Profile.objects.filter(user=request.actor,publishing_allowed=True).exists():
        raise ApiError('publishing_not_allowed',403)
    media_id=uuid.UUID(text(request.payload,'media_id',36,36))
    media=UploadedMedia.objects.filter(pk=media_id,owner=request.actor,purpose='paid_photo').first()
    if not media:raise ApiError('invalid_paid_photo',403)
    cost=amount(request.payload,'price_cents')
    caption=text(request.payload,'caption',0,500)
    with transaction.atomic():
        existing=PaidPost.objects.filter(media=media).first()
        if existing:
            if existing.price_cents!=cost or existing.caption!=caption:raise ApiError('media_already_published',409)
            return JsonResponse(post_json(existing,request))
        thumbnail=_photo_preview(media)
        row=PaidPost.objects.create(owner=request.actor,media=media,preview=thumbnail,
                                    caption=caption,price_cents=cost)
    return JsonResponse({'id':row.pk,'status':'published','item':post_json(row,request)},status=201)


@endpoint(['PATCH','DELETE'])
def paid_post_detail(request,post_id):
    row=PaidPost.objects.filter(pk=post_id,owner=request.actor).first()
    if not row:raise ApiError('post_unavailable',404)
    if request.method=='DELETE':
        row.active=False;row.save(update_fields=['active'])
        return JsonResponse({'deleted':True})
    if set(request.payload)-{'caption','price_cents'}:raise ApiError('unsupported_post_field')
    if 'caption' in request.payload:row.caption=text(request.payload,'caption',0,500)
    if 'price_cents' in request.payload:row.price_cents=amount(request.payload,'price_cents')
    row.save(update_fields=['caption','price_cents'])
    return JsonResponse({'item':post_json(row,request)})


def _target(kind,item_id,request,owned=False):
    if kind=='reel':
        row=Reel.objects.select_related('owner','media','preview').filter(pk=item_id,active=True).first()
    elif kind=='photo':
        row=PaidPost.objects.select_related('owner','media','preview').filter(pk=item_id,active=True).first()
    else:row=None
    if not row or not row.owner.is_active or (kind=='reel' and not row.approved):
        raise ApiError('content_unavailable',404)
    if owned and row.owner_id!=request.actor.pk:raise ApiError('content_unavailable',404)
    if row.owner_id in visible_owners(request.actor):raise ApiError('content_unavailable',404)
    if kind=='reel' and row.price_cents<=0:raise ApiError('not_paid_content',409)
    return row


@endpoint(['POST'])
def unlock(request,kind,item_id):
    row=_target(kind,item_id,request)
    if row.owner_id==request.actor.pk:raise ApiError('cannot_purchase_self',403)
    expected=amount(request.payload,'price_cents')
    key=uuid.UUID(text(request.payload,'request_key',36,36))
    with transaction.atomic():
        # Serialize concurrent purchases from the same customer before checking.
        User.objects.select_for_update().get(pk=request.actor.pk)
        row=_target(kind,item_id,request)
        if _content_access(request.actor,row.owner,row):
            return JsonResponse({'unlocked':True,'already_owned':True,
                'item':reel_json(row,request) if kind=='reel' else post_json(row,request)})
        if expected!=row.price_cents:raise ApiError('paid_price_changed',409)
        from .finance import hold,settle
        payment=hold(request.actor,row.owner,'paid_'+kind,key,kind+':'+str(row.pk),row.price_cents)
        settle(payment)
        ContentUnlock.objects.create(buyer=request.actor,operation=payment,**{('reel' if kind=='reel' else 'post'):row})
    return JsonResponse({'unlocked':True,'already_owned':False,
        'item':reel_json(row,request) if kind=='reel' else post_json(row,request)})


@endpoint(['GET'])
def creator_content(request,creator_id):
    owner=User.objects.filter(pk=creator_id,is_active=True).first()
    if not owner or (creator_id!=request.actor.pk and creator_id in visible_owners(request.actor)):
        raise ApiError('creator_unavailable',404)
    photos=PaidPost.objects.filter(owner=owner,active=True).select_related('owner','media','preview').order_by('-created_at')[:100]
    reels=Reel.objects.filter(owner=owner,approved=True,active=True).select_related('owner','media','preview').order_by('-created_at')[:100]
    offers=Service.objects.filter(owner=owner,approved=True,active=True).order_by('-created_at')[:100]
    entries=[]
    for p in photos:entries.append({'kind':'photo',**post_json(p,request)})
    for r in reels:entries.append({'kind':'reel',**reel_json(r,request),'created_at':r.created_at.isoformat()})
    for s in offers:entries.append({'kind':'service',**service_json(s,request),'created_at':s.created_at.isoformat()})
    entries.sort(key=lambda x:x['created_at'],reverse=True)
    return JsonResponse({'items':entries[:150]})
