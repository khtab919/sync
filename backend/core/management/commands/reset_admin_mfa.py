from django.core.management.base import BaseCommand,CommandError
from django.contrib.auth import get_user_model
from django.db import transaction
from core.models import AdminAccess,SessionToken
from core.admin_security import event
class Command(BaseCommand):
    help='Console-only emergency MFA reset. Revokes every session and records an audit event.'
    def add_arguments(self,parser):parser.add_argument('username')
    def handle(self,*args,**options):
        u=get_user_model().objects.filter(username=options['username'],is_staff=True).first()
        if not u:raise CommandError('Administrative account not found')
        why=input('Recovery reason: ').strip()
        if len(why)<10 or input('Type the exact username to confirm session revocation: ')!=u.username:raise CommandError('Recovery not confirmed')
        with transaction.atomic():
            a=AdminAccess.objects.select_for_update().filter(user=u).first()
            if not a:raise CommandError('No enrollment exists')
            a.totp_secret='';a.totp_enabled=False;a.last_counter=-1;a.failed_attempts=0;a.locked_until=None;a.save();SessionToken.objects.filter(user=u).delete();event(u,'console_mfa_recovery',u.pk,why)
        self.stdout.write('Enrollment cleared; all sessions revoked. Use the regular app login to enroll again.')
