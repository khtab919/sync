"""Read-only release diagnostics. Never prints secrets or private account data."""
import json,os
from pathlib import Path
from django.core.management.base import BaseCommand
from django.db import connection
from django.db.migrations.executor import MigrationExecutor
from django.conf import settings
class Command(BaseCommand):
    def handle(self,*args,**options):
        from core.models import PushDelivery,UploadSession
        plan=MigrationExecutor(connection).migration_plan(MigrationExecutor(connection).loader.graph.leaf_nodes())
        result={'build':'11.6','database':connection.vendor,'pending_migrations':len(plan),'push_project_configured':bool(os.environ.get('FCM_PROJECT_ID')),'push_credentials_file_present':Path(os.environ.get('GOOGLE_APPLICATION_CREDENTIALS','/nonexistent')).is_file(),'email_configured':bool(settings.EMAIL_HOST and settings.DEFAULT_FROM_EMAIL),'media_probe_enabled':os.environ.get('MEDIA_PROBE_ENABLED')=='1','live_configured':os.environ.get('LAYALI_LIVE_ENABLED')=='1' and bool(os.environ.get('LIVEKIT_API_KEY'))}
        if not plan:
            result.update(push_pending=PushDelivery.objects.filter(sent_at__isnull=True,failed=False).count(),push_failed=PushDelivery.objects.filter(failed=True).count(),incomplete_uploads=UploadSession.objects.filter(media__isnull=True).count())
        self.stdout.write(json.dumps(result,indent=2))
