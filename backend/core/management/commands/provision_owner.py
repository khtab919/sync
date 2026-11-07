from getpass import getpass
from django.core.management.base import BaseCommand,CommandError
from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from django.db import transaction
from core.models import Profile,Wallet,AdminAccess
class Command(BaseCommand):
    help='Provision the reserved super_admin owner; no password is printed or stored in source.'
    def handle(self,*args,**kwargs):
        U=get_user_model()
        if U.objects.filter(username='super_admin').exists():raise CommandError('super_admin already exists; no existing account was modified.')
        email=input('Owner email: ').strip();password=getpass('Owner password: ')
        if password!=getpass('Confirm password: '):raise CommandError('Passwords differ')
        from django.core.validators import validate_email
        validate_email(email);validate_password(password,U(username='super_admin',email=email))
        with transaction.atomic():
            u=U.objects.create_superuser('super_admin',email,password);Profile.objects.create(user=u,display_name='المالك الرئيسي');Wallet.objects.create(user=u);AdminAccess.objects.create(user=u,role='owner')
        self.stdout.write('Owner created. Sign in through the app and enroll an authenticator before administration.')
