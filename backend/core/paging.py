"""Bounded keyset pages: no offset drift or silent truncation of older content."""
from django.db.models import Q
from django.utils import timezone

def visible_owners(user):
    from .models import UserBlock, AccountSanction
    blocks=UserBlock.objects.filter(Q(user=user)|Q(blocked=user))
    ids={b.blocked_id if b.user_id==user.pk else b.user_id for b in blocks}
    ids.update(AccountSanction.objects.filter(kind='ban',revoked_at__isnull=True).filter(Q(expires_at__isnull=True)|Q(expires_at__gt=timezone.now())).values_list('user_id',flat=True))
    return ids

def page_rows(queryset, request):
    limit=max(1,min(100,int(request.GET.get('limit','100'))))
    before=request.GET.get('before')
    if before:
        value=int(before)
        if value<=0:raise ValueError('invalid_cursor')
        queryset=queryset.filter(pk__lt=value)
    rows=list(queryset.order_by('-pk')[:limit+1]); more=len(rows)>limit
    rows=rows[:limit]
    return rows,str(rows[-1].pk) if more else None
