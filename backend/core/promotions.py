from django.db.models import Sum
from .models import PromotionalGrant

def grant(user,points,reference,reason,actor=None):
    if points>0:
        row,_=PromotionalGrant.objects.get_or_create(reference=reference,defaults={'user':user,'points':points,'reason':reason,'actor':actor})
        if row.user_id!=user.pk or row.points!=points or row.actor_id!=getattr(actor,'pk',None):
            from .views import ApiError
            raise ApiError('promotion_request_conflict',409)
        return row

def balance(user):return PromotionalGrant.objects.filter(user=user).aggregate(v=Sum('points'))['v'] or 0
