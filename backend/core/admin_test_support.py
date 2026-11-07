"""Explicit MFA/independent-review fixtures for legacy business tests only."""
import hashlib
from datetime import timedelta
from django.utils import timezone
from django.contrib.auth import get_user_model
from .models import SessionToken,AdminApproval
from .views import issue_token as normal_token
from .admin_security import digest

def issue_token(user):
    raw=normal_token(user)
    if user.is_staff:SessionToken.objects.filter(digest=hashlib.sha256(raw.encode()).hexdigest()).update(admin_verified_at=timezone.now())
    return raw

def reviewed_payload(actor,path,data,target):
    reviewer,_=get_user_model().objects.get_or_create(username='independent_fixture_reviewer',defaults={'is_staff':True,'is_superuser':True})
    proposal=AdminApproval.objects.create(requester=actor,reviewer=reviewer,path=path,payload_digest=digest(data),payload=data,target_user_id=target,reason='Independent test review',status='approved',expires_at=timezone.now()+timedelta(hours=1))
    return dict(data,approval_id=str(proposal.pk))
