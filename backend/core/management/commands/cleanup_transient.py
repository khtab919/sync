from datetime import timedelta
from django.core.management.base import BaseCommand
from django.utils import timezone
from core.models import UploadSession,PushDelivery,AuthRateLimit
class Command(BaseCommand):
    help='Remove expired upload fragments, old notification payloads and rate-limit counters; keep user content and finance.'
    def handle(self,*args,**options):
        now=timezone.now()
        # Completed upload sessions expire after a week; durable media is never deleted here.
        UploadSession.objects.filter(media__isnull=True,created_at__lt=now-timedelta(days=1)).delete()
        UploadSession.objects.filter(media__isnull=False,created_at__lt=now-timedelta(days=7)).delete()
        PushDelivery.objects.filter(expires_at__lt=now-timedelta(days=7)).delete()
        AuthRateLimit.objects.filter(window_start__lt=now-timedelta(days=2)).delete()
        self.stdout.write('Expired transient data removed. Financial records and published media retained.')
