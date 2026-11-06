import uuid
from django.conf import settings
from django.db import models
from django.db.models import Q

class Profile(models.Model):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    date_of_birth = models.DateField(null=True, blank=True)
    policy_version = models.CharField(max_length=32, default='2026-11-06')
    policy_accepted_at = models.DateTimeField(auto_now_add=True)
    creator = models.BooleanField(default=False)
    identity_verified = models.BooleanField(default=False)
    chat_price_cents = models.PositiveIntegerField(default=0)
    bio = models.CharField(max_length=300, blank=True)
    display_name = models.CharField(max_length=100, blank=True)
    selected_frame = models.PositiveSmallIntegerField(default=0)

class Service(models.Model):
    owner = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    title = models.CharField(max_length=150)
    description = models.TextField(max_length=600)
    price_cents = models.PositiveIntegerField()
    image_url = models.URLField(max_length=500, blank=True)
    category = models.CharField(max_length=30, default='خدمة')
    approved = models.BooleanField(default=False)
    active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

class Booking(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    buyer = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    service = models.ForeignKey(Service, on_delete=models.PROTECT)
    price_cents = models.PositiveIntegerField()
    status = models.CharField(max_length=32, default='awaiting_payment', editable=False)
    created_at = models.DateTimeField(auto_now_add=True)

class Wallet(models.Model):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    balance_cents = models.BigIntegerField(default=0, editable=False)
    reserved_cents = models.BigIntegerField(default=0, editable=False)
    class Meta:
        constraints = [models.CheckConstraint(condition=Q(balance_cents__gte=0) & Q(reserved_cents__gte=0) & Q(balance_cents__gte=models.F('reserved_cents')), name='wallet_valid_balance')]

class Withdrawal(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    amount_cents = models.PositiveIntegerField()
    destination = models.CharField(max_length=300)
    request_key = models.UUIDField()
    status = models.CharField(max_length=32, default='pending', editable=False)
    created_at = models.DateTimeField(auto_now_add=True)
    class Meta:
        constraints = [models.UniqueConstraint(fields=['user', 'request_key'], name='unique_withdrawal_request')]

class Message(models.Model):
    sender = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='sent_messages')
    recipient = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='received_messages')
    body = models.CharField(max_length=2000)
    created_at = models.DateTimeField(auto_now_add=True)

class Reel(models.Model):
    owner = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    video_url = models.URLField(max_length=500)
    approved = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

class Report(models.Model):
    reporter = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    target_type = models.CharField(max_length=32)
    target_id = models.PositiveBigIntegerField()
    reason = models.CharField(max_length=500)
    resolved = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

class SessionToken(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    digest = models.CharField(max_length=64, unique=True)
    expires_at = models.DateTimeField()

class AuthRateLimit(models.Model):
    key = models.CharField(max_length=64, primary_key=True)
    window_start = models.DateTimeField()
    attempts = models.PositiveIntegerField(default=0)


class ServiceReaction(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    service = models.ForeignKey(Service, on_delete=models.CASCADE)
    kind = models.CharField(max_length=10, choices=[('like', 'Like'), ('save', 'Save')])
    class Meta:
        constraints = [models.UniqueConstraint(fields=['user', 'service', 'kind'], name='unique_service_reaction')]

class Follow(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='follows')
    creator = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='followers')
    class Meta:
        constraints = [models.UniqueConstraint(fields=['user', 'creator'], name='unique_follow')]

class SupportTicket(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    body = models.CharField(max_length=2000)
    reply = models.CharField(max_length=2000, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
