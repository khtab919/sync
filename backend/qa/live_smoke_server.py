"""Synthetic, temporary broadcast fixture. No real users, payments or media files."""
import os,sys,json,secrets,tempfile,hashlib,threading
from pathlib import Path
from datetime import timedelta
os.environ.update(DJANGO_SETTINGS_MODULE='config.settings',DJANGO_SECRET_KEY='test-only-live-smoke',POSTGRES_PASSWORD='test',LAYALI_TESTING='1',LAYALI_LIVE_ENABLED='1',LIVEKIT_API_KEY='smoke-key',LIVEKIT_API_SECRET='smoke-secret-at-least-thirty-two-characters',LIVEKIT_INTERNAL_URL='http://127.0.0.1:7880',LIVEKIT_PUBLIC_URL='ws://127.0.0.1:8880/livekit')
sys.path.insert(0,str(Path(__file__).resolve().parents[1]));import django;django.setup()
from django.conf import settings
from django.core.management import call_command
from django.contrib.auth import get_user_model
from django.utils import timezone
from django.core.wsgi import get_wsgi_application
from django.urls import path
from django.contrib.staticfiles.views import serve
from wsgiref.simple_server import make_server,WSGIRequestHandler,WSGIServer
from socketserver import ThreadingMixIn
from core.models import Profile,Wallet,SessionToken,LiveBroadcast
from core import live
import config.urls
from django.db import close_old_connections
import time
config.urls.urlpatterns.insert(0,path('static/<path:path>',lambda request,path:serve(request,path,insecure=True)))
class ThreadingServer(ThreadingMixIn,WSGIServer):daemon_threads=True
class Quiet(WSGIRequestHandler):
    def log_message(self,*args):pass
with tempfile.TemporaryDirectory(prefix='live-sfu-test-') as directory:
    settings.DATABASES['default']['NAME']=str(Path(directory)/'test.sqlite3');settings.MEDIA_ROOT=Path(directory)/'media';settings.ALLOWED_HOSTS=['127.0.0.1','localhost'];settings.SECURE_SSL_REDIRECT=False
    call_command('migrate',verbosity=0)
    ids=[];tokens=[]
    for i in range(14):
        u=get_user_model().objects.create_user('sfu_test_'+str(i),password=secrets.token_urlsafe(24));Profile.objects.create(user=u,creator=i==0);Wallet.objects.create(user=u,balance_cents=10000 if i>0 else 0)
        token=secrets.token_urlsafe(48);SessionToken.objects.create(user=u,digest=hashlib.sha256(token.encode()).hexdigest(),expires_at=timezone.now()+timedelta(hours=1));ids.append(u.pk);tokens.append(token)
    Path(os.environ['LIVE_SMOKE_CONFIG']).write_text(json.dumps({'ids':ids,'tokens':tokens}));Path(os.environ['LIVE_SMOKE_CONFIG']).chmod(0o600)
    # SQLite is a media-fixture store, not the production concurrency test database.
    # Serialize fixture HTTP/reconciliation writes; PostgreSQL races are tested separately.
    fixture_lock=threading.RLock()
    def reconcile():
        while True:
            try:
                with fixture_lock:
                    for row in LiveBroadcast.objects.filter(status__in=live.LIVE):live.synchronize(row,True)
            except Exception:pass
            close_old_connections();time.sleep(5)
    threading.Thread(target=reconcile,daemon=True).start()
    print('Isolated live SFU fixture ready',flush=True)
    import logging
    logger=logging.getLogger('django.request');logger.addHandler(logging.StreamHandler());logger.setLevel(logging.ERROR)
    actual_app=get_wsgi_application()
    def fixture_app(environ,start_response):
        with fixture_lock:
            response=actual_app(environ,start_response)
            try:return list(response)
            finally:
                if hasattr(response,'close'):response.close()
    make_server('127.0.0.1',8766,fixture_app,server_class=ThreadingServer,handler_class=Quiet).serve_forever()
