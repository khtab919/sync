from django.http import JsonResponse
from django.db import transaction
from .views import ApiError, endpoint, text
from .models import Service,Reel,PaidPost,PublicationComment,UploadedMedia
from .paging import visible_owners

def options(payload):
    private=payload.get("owner_only",False); comments=payload.get("comments_allowed",True)
    if type(private) is not bool or type(comments) is not bool:raise ApiError("invalid_publication_options")
    return {"owner_only":private,"active":not private,"comments_allowed":comments}

def cover(payload,user):
    raw=payload.get("cover_id")
    if not raw:return None
    result=UploadedMedia.objects.filter(pk=raw,owner=user,purpose="offer",mime="image/jpeg").first()
    if not result:raise ApiError("invalid_cover")
    return result

def notify(user,kind,row):
    from .wallet_v2 import notice
    notice(user,"تم النشر بنجاح","اكتمل نشر محتواك.","published",kind+":"+str(row.pk))

@endpoint(["GET","POST"])
def comments(request,kind,item_id):
    model={"photo":PaidPost,"reel":Reel,"service":Service}.get(kind)
    if not model:raise ApiError("unavailable",404)
    row=model.objects.filter(pk=item_id,owner__is_active=True).first()
    if not row or not (row.active or row.owner_only and row.owner_id==request.actor.pk):raise ApiError("unavailable",404)
    if row.owner_id!=request.actor.pk and (row.owner_only or not getattr(row,"approved",True) or row.owner_id in visible_owners(request.actor)):raise ApiError("unavailable",404)
    if not row.comments_allowed:raise ApiError("comments_closed",409)
    if request.method=="POST":
        from .admin_security import blocked
        if blocked(request.actor,"messages"):raise ApiError("feature_restricted",403)
        PublicationComment.objects.create(kind=kind,item_id=item_id,user=request.actor,body=text(request.payload,"body",1,500))
    qs=PublicationComment.objects.filter(kind=kind,item_id=item_id,user__is_active=True).exclude(user_id__in=visible_owners(request.actor)).select_related("user__profile").order_by("-id")[:100]
    return JsonResponse({"items":[{"name":x.user.profile.display_name or x.user.username,"body":x.body} for x in qs]})
