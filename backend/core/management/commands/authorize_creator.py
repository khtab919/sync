from django.core.management.base import BaseCommand, CommandError
from django.contrib.auth import get_user_model
from django.db import transaction
from core.models import Profile, Wallet

class Command(BaseCommand):
    help = 'Enable publishing and creator discovery for an existing account matched by email.'
    def add_arguments(self, parser):
        parser.add_argument('email')
    @transaction.atomic
    def handle(self, *args, **options):
        users = list(get_user_model().objects.select_for_update().filter(email__iexact=options['email'].strip())[:2])
        if len(users) != 1:
            raise CommandError('Expected exactly one existing account with this email; no changes made.')
        user = users[0]
        if not user.is_active:
            raise CommandError('Account is banned; review it in administration first.')
        profile, _ = Profile.objects.get_or_create(user=user)
        profile.creator = True
        profile.publishing_allowed = True
        profile.save(update_fields=['creator','publishing_allowed'])
        Wallet.objects.get_or_create(user=user)
        self.stdout.write(self.style.SUCCESS('Publishing and creator discovery enabled for account #' + str(user.pk)))
