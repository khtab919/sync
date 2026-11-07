"""Public account identifiers are independent of immutable database primary keys."""
import re,secrets
from django.db.models.functions import Lower,Trim

def lock_registry():
    from .models import IdentityRegistry
    IdentityRegistry.objects.get_or_create(pk=1)
    return IdentityRegistry.objects.select_for_update().get(pk=1)

def allocate_public_id():
    from .models import Profile
    for _ in range(100):
        value=str(secrets.randbelow(90000)+10000)
        if not Profile.objects.filter(public_id=value).exists():return value
    used=set(Profile.objects.values_list('public_id',flat=True))
    for n in range(10000,100000):
        if str(n) not in used:return str(n)
    from .views import ApiError
    raise ApiError('account_id_capacity',503)

def normalize_email(value):
    from .views import ApiError
    from django.core.validators import validate_email
    if not isinstance(value,str):raise ApiError('invalid_email')
    value=value.strip().lower()
    try:validate_email(value)
    except Exception:raise ApiError('invalid_email')
    if len(value)>150:raise ApiError('invalid_email')
    return value

def email_matches(value):
    from django.contrib.auth import get_user_model
    return get_user_model().objects.annotate(_email_key=Lower(Trim('email'))).filter(_email_key=value)

def normalize_handle(value):
    from .views import ApiError
    if not isinstance(value,str):raise ApiError('invalid_username')
    value=value.strip().removeprefix('@').lower()
    if not re.fullmatch(r'[a-z][a-z0-9_.]{2,29}',value):raise ApiError('invalid_username')
    return value

def target_user(payload):
    from django.contrib.auth import get_user_model
    from .views import ApiError
    if 'public_id' in payload:
        value=payload['public_id']
        if not isinstance(value,str) or not re.fullmatch(r'[1-9][0-9]{4}',value):raise ApiError('invalid_public_id')
        return get_user_model().objects.get(profile__public_id=value)
    return get_user_model().objects.get(pk=payload.get('user_id'))
