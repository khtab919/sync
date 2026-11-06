import time
from django.core.management.base import BaseCommand
from django.db import close_old_connections
from core.models import LiveBroadcast
from core.live import synchronize,ApiError,LIVE,enabled,close,provider,room_name
from django.utils import timezone
from datetime import timedelta
class Command(BaseCommand):
    help='Reconcile actual SFU presence and financial escrow; never stores media.'
    def add_arguments(self,parser):parser.add_argument('--loop',action='store_true')
    def handle(self,*args,**options):
        while True:
            if enabled():
                for row in LiveBroadcast.objects.filter(status__in=LIVE).select_related('owner__profile')[:100]:
                    try:synchronize(row,True)
                    except ApiError:
                        anchor=row.last_checked_at or row.created_at
                        if timezone.now()-anchor>timedelta(seconds=120):close(row,"تعذر استعادة خادم البث",True)
                    except Exception:self.stderr.write('Live reconciliation failed; financial state preserved for review.')
                for row in LiveBroadcast.objects.filter(cleanup_pending=True)[:100]:
                    try:provider('DeleteRoom',{'room':room_name(row)});LiveBroadcast.objects.filter(pk=row.pk).update(cleanup_pending=False)
                    except ApiError:pass
            from core.models import Booking
            from core import finance
            from django.db import transaction
            from core.views import User
            for pending in Booking.objects.filter(status='paid_reserved',created_at__lt=timezone.now()-timedelta(hours=24)).select_related('service','operation')[:100]:
                with transaction.atomic():
                    list(User.objects.select_for_update().filter(pk__in=sorted([pending.buyer_id,pending.service.owner_id])).order_by('pk'))
                    booking=Booking.objects.select_for_update(of=('self',)).select_related('operation__buyer','operation__creator').get(pk=pending.pk)
                    if booking.status=='paid_reserved' and booking.operation_id:finance.refund(booking.operation,'لم يقبل مقدم الخدمة خلال 24 ساعة');booking.status='refunded';booking.save(update_fields=['status'])
            close_old_connections()
            if not options['loop']:break
            time.sleep(5)
