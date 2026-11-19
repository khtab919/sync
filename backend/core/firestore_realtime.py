"""Optional Firebase bridge for revocable, user-scoped live moderation signals.

Requires RELAXIA_FIRESTORE_MODERATION=1, FIREBASE_PROJECT_ID and a Google
service-account JSON file provided by GOOGLE_APPLICATION_CREDENTIALS.
Django/Postgres/LiveKit remain authoritative when this mirror is unavailable.
Clients cannot update these documents (see firestore.rules).
"""
import json,os,re,time,logging
from datetime import datetime,timezone as tz
from urllib.parse import quote
from django.http import JsonResponse
from django.db import transaction
from django.utils import timezone
from .views import endpoint,ApiError

log=logging.getLogger('relaxia.firestore.moderation')
SCOPES=['https://www.googleapis.com/auth/datastore']
FIREBASE_AUD='https://identitytoolkit.googleapis.com/google.identity.identitytoolkit.v1.IdentityToolkit'


def configured():
    return (os.environ.get('RELAXIA_FIRESTORE_MODERATION')=='1'
            and bool(re.fullmatch(r'[a-zA-Z0-9-]{6,100}',os.environ.get('FIREBASE_PROJECT_ID','')))
            and bool(os.environ.get('GOOGLE_APPLICATION_CREDENTIALS')))


def doc_fields(mapping):
    result={}
    for key,value in mapping.items():
        if value is None:result[key]={'nullValue':None}
        elif type(value) is bool:result[key]={'booleanValue':value}
        elif type(value) is int:result[key]={'integerValue':str(value)}
        elif isinstance(value,datetime):result[key]={'timestampValue':value.astimezone(tz.utc).isoformat().replace('+00:00','Z')}
        else:result[key]={'stringValue':str(value)}
    return result


def write_document(path,mapping):
    if not configured():return False
    import google.auth
    from google.auth.transport.requests import AuthorizedSession
    try:
        credentials,_=google.auth.default(scopes=SCOPES)
        session=AuthorizedSession(credentials)
        project=os.environ['FIREBASE_PROJECT_ID']
        url=('https://firestore.googleapis.com/v1/projects/'+quote(project,safe='')+
             '/databases/(default)/documents/'+path)
        params=[('updateMask.fieldPaths',k) for k in mapping]
        response=session.patch(url,params=params,json={'fields':doc_fields(mapping)},timeout=4)
        response.raise_for_status()
        return True
    except Exception:
        log.exception('Firestore moderation mirror failed for %s',path)
        return False


def mirror_account(row):
    # No private device/IP identifiers. Reason is visible only to its owner.
    suspended=row.status=='suspended' and (not row.suspended_until or row.suspended_until>timezone.now())
    account_status=row.status if suspended or row.status=='banned_permanent' else 'active'
    return write_document('users/'+str(row.user_id),{
        'accountStatus':account_status,
        'isBanned':row.status=='banned_permanent',
        'suspendedUntil':row.suspended_until if suspended else None,
        'reason':row.reason if account_status!='active' else '',
        'version':row.version})


def mirror_room_kick(room_id,user_id,reason='room_kick'):
    return write_document('rooms/'+str(room_id)+'/kickedUsers/'+str(user_id),{
        'kicked':True,'roomId':str(room_id),'reason':reason,
        'at':timezone.now()})


def on_commit_account(row):
    if configured():
        pk=row.pk
        from .models import AccountModerationState
        transaction.on_commit(lambda:mirror_account(AccountModerationState.objects.get(pk=pk)))


def on_commit_room_kick(room_id,user_id,reason='room_kick'):
    if configured():
        transaction.on_commit(lambda:mirror_room_kick(room_id,user_id,reason))


@endpoint(['GET'])
def custom_token(request):
    if not configured():raise ApiError('firebase_realtime_unavailable',503)
    try:
        from google.auth import jwt
        from google.oauth2.service_account import Credentials
        file=os.environ['GOOGLE_APPLICATION_CREDENTIALS']
        credentials=Credentials.from_service_account_file(file)
        email=credentials.service_account_email
        iat=int(time.time())
        claims={'iss':email,'sub':email,'aud':FIREBASE_AUD,'iat':iat,'exp':iat+3600,
                'uid':str(request.actor.pk)}
        token=jwt.encode(credentials.signer,claims).decode('ascii')
        response=JsonResponse({'token':token,'uid':str(request.actor.pk),
            'project_id':os.environ['FIREBASE_PROJECT_ID']})
        response['Cache-Control']='no-store'
        return response
    except Exception:
        log.exception('Firebase custom token generation unavailable')
        raise ApiError('firebase_realtime_unavailable',503)
