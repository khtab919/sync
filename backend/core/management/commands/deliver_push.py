import time
from django.core.management.base import BaseCommand
from core.push import send_one
class Command(BaseCommand):
    help='Deliver committed push events; requires FCM_PROJECT_ID and Google application credentials.'
    def add_arguments(self,parser):parser.add_argument('--loop',action='store_true')
    def handle(self,*args,**options):
        while True:
            sent=send_one()
            if not options['loop']:break
            if not sent:time.sleep(3)
