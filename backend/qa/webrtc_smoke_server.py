"""Isolated browser-test fixture: loopback only, temporary SQLite, synthetic users."""
import hashlib
import json
import os
from pathlib import Path
import secrets
import sys
import tempfile
from datetime import timedelta

os.environ.update(DJANGO_SETTINGS_MODULE='config.settings', DJANGO_SECRET_KEY='local-webrtc-test-only', POSTGRES_PASSWORD='test', LAYALI_TESTING='1', LAYALI_TURN_HOST='127.0.0.1', LAYALI_TURN_SECRET='local-rtc-test-secret', LAYALI_CALL_TEST_RELAY_ONLY='1')
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import django
django.setup()
from django.conf import settings
from django.core.management import call_command
from django.contrib.auth import get_user_model
from django.utils import timezone
from django.core.wsgi import get_wsgi_application
from wsgiref.simple_server import make_server, WSGIRequestHandler
from core.models import Profile, Wallet, SessionToken

with tempfile.TemporaryDirectory(prefix='relaxia-rtc-') as directory:
    settings.DATABASES['default']['NAME'] = str(Path(directory) / 'smoke.sqlite3')
    settings.ALLOWED_HOSTS = ['127.0.0.1', 'localhost']
    settings.SECURE_SSL_REDIRECT = False
    call_command('migrate', verbosity=0)
    ids, tokens = [], []
    for name in ('rtc_test_caller', 'rtc_test_callee'):
        user = get_user_model().objects.create_user(name, password=secrets.token_urlsafe(32))
        Profile.objects.create(user=user)
        Wallet.objects.create(user=user)
        token = secrets.token_urlsafe(48)
        SessionToken.objects.create(user=user, digest=hashlib.sha256(token.encode()).hexdigest(), expires_at=timezone.now()+timedelta(hours=1))
        ids.append(user.pk); tokens.append(token)
    config = Path(os.environ['CALL_SMOKE_CONFIG'])
    config.write_text(json.dumps({'ids': ids, 'tokens': tokens}))
    config.chmod(0o600)
    class Quiet(WSGIRequestHandler):
        def log_message(self, *args): pass
    print('Isolated WebRTC test server ready', flush=True)
    make_server('127.0.0.1', 8765, get_wsgi_application(), handler_class=Quiet).serve_forever()
