import os
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parent.parent
SECRET_KEY = os.environ['DJANGO_SECRET_KEY']
DEBUG = False
ALLOWED_HOSTS = os.environ.get('ALLOWED_HOSTS', 'localhost').split(',')
INSTALLED_APPS = ['django.contrib.auth', 'django.contrib.contenttypes', 'django.contrib.sessions', 'django.contrib.messages', 'django.contrib.staticfiles', 'django.contrib.admin', 'core']
MIDDLEWARE = ['django.middleware.security.SecurityMiddleware', 'django.contrib.sessions.middleware.SessionMiddleware', 'django.middleware.common.CommonMiddleware', 'django.middleware.csrf.CsrfViewMiddleware', 'django.contrib.auth.middleware.AuthenticationMiddleware', 'django.contrib.messages.middleware.MessageMiddleware', 'django.middleware.clickjacking.XFrameOptionsMiddleware']
ROOT_URLCONF = 'config.urls'
TEMPLATES = [{'BACKEND': 'django.template.backends.django.DjangoTemplates', 'APP_DIRS': True, 'OPTIONS': {'context_processors': ['django.template.context_processors.request', 'django.contrib.auth.context_processors.auth', 'django.contrib.messages.context_processors.messages']}}]
WSGI_APPLICATION = 'config.wsgi.application'
DATABASES = {'default': {'ENGINE': 'django.db.backends.postgresql', 'HOST': os.environ.get('DB_HOST', 'db'), 'NAME': os.environ.get('POSTGRES_DB', 'layali'), 'USER': os.environ.get('POSTGRES_USER', 'layali'), 'PASSWORD': os.environ['POSTGRES_PASSWORD'], 'CONN_MAX_AGE': 60}}
if os.environ.get('LAYALI_TESTING') == '1':
    DATABASES = {'default': {'ENGINE': 'django.db.backends.sqlite3', 'NAME': ':memory:'}}
AUTH_PASSWORD_VALIDATORS = [{'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator'}, {'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator'}, {'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator'}, {'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator'}]
LANGUAGE_CODE = 'ar'
TIME_ZONE = 'UTC'
USE_TZ = True
STATIC_URL = '/static/'
STATIC_ROOT = BASE_DIR / 'staticfiles'
DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'
SECURE_PROXY_SSL_HEADER = ('HTTP_X_FORWARDED_PROTO', 'https')
SESSION_COOKIE_SECURE = True
CSRF_COOKIE_SECURE = True
SECURE_CONTENT_TYPE_NOSNIFF = True
X_FRAME_OPTIONS = 'DENY'
DATA_UPLOAD_MAX_MEMORY_SIZE = 32 * 1024 * 1024
CSRF_TRUSTED_ORIGINS = ['https://' + h for h in ALLOWED_HOSTS if h not in ('localhost', 'testserver')]

SECURE_SSL_REDIRECT = os.environ.get('LAYALI_TESTING') != '1'
SECURE_HSTS_SECONDS = 3600

MEDIA_ROOT = BASE_DIR / "media"

PASSWORD_RESET_TIMEOUT=1800
EMAIL_BACKEND='django.core.mail.backends.smtp.EmailBackend'
EMAIL_HOST=os.environ.get('EMAIL_HOST','')
EMAIL_PORT=int(os.environ.get('EMAIL_PORT','587'))
EMAIL_USE_TLS=os.environ.get('EMAIL_USE_TLS','1')=='1'
EMAIL_HOST_USER=os.environ.get('EMAIL_HOST_USER','')
EMAIL_HOST_PASSWORD=os.environ.get('EMAIL_HOST_PASSWORD','')
EMAIL_TIMEOUT=10
DEFAULT_FROM_EMAIL=os.environ.get('DEFAULT_FROM_EMAIL','')
