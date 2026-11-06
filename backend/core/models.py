import uuid
from django.conf import settings
from django.db import models
from django.db.models import Q
from django.utils import timezone

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
    avatar_url = models.URLField(max_length=500, blank=True)
    last_active_at = models.DateTimeField(null=True, blank=True)

class Service(models.Model):
    image = models.ForeignKey("UploadedMedia", null=True, blank=True, on_delete=models.PROTECT)
    owner = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    title = models.CharField(max_length=150)
    description = models.TextField(max_length=600)
    price_cents = models.PositiveIntegerField()
    image_url = models.URLField(max_length=500, blank=True)
    category = models.CharField(max_length=30, default='خدمة', choices=[('خدمة', 'خدمة'), ('جلسة', 'جلسة'), ('منتج', 'منتج')])
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
    client_key = models.UUIDField(null=True, blank=True)
    delivered_at = models.DateTimeField(null=True, blank=True)
    read_at = models.DateTimeField(null=True, blank=True)
    media_price_cents = models.PositiveIntegerField(default=0)
    class Meta:
        constraints = [models.UniqueConstraint(fields=['sender','client_key'], name='unique_message_client_key')]

    call = models.OneToOneField("InternalCall", null=True, blank=True, on_delete=models.PROTECT)
    attachment = models.OneToOneField("UploadedMedia", null=True, blank=True, on_delete=models.PROTECT)
    call_url = models.URLField(blank=True)
    sender = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='sent_messages')
    recipient = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='received_messages')
    body = models.CharField(max_length=2000)
    created_at = models.DateTimeField(auto_now_add=True)

class Reel(models.Model):
    media = models.OneToOneField("UploadedMedia", null=True, blank=True, on_delete=models.PROTECT)
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


class BalanceAdjustment(models.Model):
    request_key = models.UUIDField(null=True, blank=True, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='balance_adjustments')
    administrator = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='issued_balance_adjustments')
    class Meta:
        constraints = [models.UniqueConstraint(fields=['administrator', 'request_key'], name='unique_admin_credit_request')]
    amount_cents = models.PositiveBigIntegerField()
    reason = models.CharField(max_length=300)
    created_at = models.DateTimeField(auto_now_add=True)

class UploadedMedia(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    owner = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    purpose = models.CharField(max_length=20)
    file = models.FileField(upload_to='uploads/%Y/%m/')
    mime = models.CharField(max_length=40)
    size = models.PositiveIntegerField()
    created_at = models.DateTimeField(auto_now_add=True)

class SellerApplication(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    description = models.CharField(max_length=1000)
    status = models.CharField(max_length=16, default='pending')
    review_note = models.CharField(max_length=300, blank=True)
    reviewer = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, on_delete=models.PROTECT, related_name='seller_reviews')
    created_at = models.DateTimeField(auto_now_add=True)

class TopUp(models.Model):
    quote = models.OneToOneField("TopUpQuote", null=True, blank=True, on_delete=models.PROTECT)
    method = models.CharField(max_length=12, default='usdt')
    late_submission = models.BooleanField(default=False)

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    amount_cents = models.PositiveIntegerField()
    bonus_cents = models.PositiveIntegerField(default=0)
    transaction_hash = models.CharField(max_length=120, unique=True)
    receipt = models.OneToOneField(UploadedMedia, on_delete=models.PROTECT)
    status = models.CharField(max_length=16, default='pending')
    review_note = models.CharField(max_length=300, blank=True)
    reviewer = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, on_delete=models.PROTECT, related_name='topup_reviews')
    created_at = models.DateTimeField(auto_now_add=True)

class PaidWheelConfiguration(models.Model):
    enabled = models.BooleanField(default=False)
    cost_cents = models.PositiveIntegerField(default=350)
    price_step_cents = models.PositiveIntegerField(default=50)
    maximum_cost_cents = models.PositiveIntegerField(default=1000)
    first_reward_cents = models.PositiveIntegerField(default=500)
    grand_unlock_spend_cents = models.PositiveIntegerField(default=20000)
    prizes = models.JSONField(default=list)
    bankroll_cents = models.PositiveBigIntegerField(default=0)

class PaidSpin(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    request_key = models.UUIDField()
    cost_cents = models.PositiveIntegerField()
    prize_cents = models.PositiveIntegerField()
    prizes_snapshot = models.JSONField()
    created_at = models.DateTimeField(auto_now_add=True)
    class Meta:
        constraints = [models.UniqueConstraint(fields=['user', 'request_key'], name='unique_paid_spin_request')]

class InternalCall(models.Model):
    ringing_at = models.DateTimeField(null=True, blank=True)
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    caller = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='outgoing_calls')
    callee = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='incoming_calls')
    mode = models.CharField(max_length=8, default='audio')
    status = models.CharField(max_length=12, default='ringing')
    created_at = models.DateTimeField(auto_now_add=True)
    accepted_at = models.DateTimeField(null=True)
    ended_at = models.DateTimeField(null=True)
    caller_seen_at = models.DateTimeField(default=timezone.now)
    callee_seen_at = models.DateTimeField(null=True)

class CallSignal(models.Model):
    call = models.ForeignKey(InternalCall, on_delete=models.CASCADE)
    sender = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    kind = models.CharField(max_length=12)
    data = models.JSONField()
    created_at = models.DateTimeField(auto_now_add=True)

class CallTicket(models.Model):
    digest = models.CharField(max_length=64, unique=True)
    call = models.ForeignKey(InternalCall, on_delete=models.CASCADE)
    session = models.ForeignKey(SessionToken, on_delete=models.CASCADE)
    expires_at = models.DateTimeField()
    used_at = models.DateTimeField(null=True)

class PaymentSettings(models.Model):
    shamcash_enabled = models.BooleanField(default=False)
    shamcash_account = models.CharField(max_length=160, blank=True)
    shamcash_currency = models.CharField(max_length=3, default='USD', choices=[('USD','USD'),('SYP','SYP')])
    shamcash_rate = models.DecimalField(max_digits=16, decimal_places=6, default=1)
    shamcash_instructions = models.CharField(max_length=500, blank=True)

class TopUpQuote(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT)
    method = models.CharField(max_length=12)
    amount_cents = models.PositiveIntegerField()
    bonus_cents = models.PositiveIntegerField()
    payment_amount = models.DecimalField(max_digits=18, decimal_places=2)
    payment_currency = models.CharField(max_length=4)
    destination = models.CharField(max_length=160)
    expires_at = models.DateTimeField()
    created_at = models.DateTimeField(auto_now_add=True)

class GatewayInvoice(models.Model):
    quote = models.OneToOneField(TopUpQuote,on_delete=models.PROTECT)
    invoice_id = models.CharField(max_length=64,null=True,blank=True,unique=True)
    payment_id = models.CharField(max_length=64,null=True,blank=True,unique=True)
    invoice_url = models.URLField(max_length=500,blank=True)
    status = models.CharField(max_length=32,default='creating')
    credited_at = models.DateTimeField(null=True,blank=True)
    review_required = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)


class ChatCharge(models.Model):
    message = models.ForeignKey(Message,on_delete=models.PROTECT)
    kind = models.CharField(max_length=12,choices=[('message','Message'),('unlock','Unlock')])
    buyer = models.ForeignKey(settings.AUTH_USER_MODEL,on_delete=models.PROTECT,related_name='chat_purchases')
    creator = models.ForeignKey(settings.AUTH_USER_MODEL,on_delete=models.PROTECT,related_name='chat_earnings')
    amount_cents = models.PositiveIntegerField()
    creator_cents = models.PositiveIntegerField()
    platform_cents = models.PositiveIntegerField()
    created_at = models.DateTimeField(auto_now_add=True)
    class Meta:
        constraints=[models.UniqueConstraint(fields=['message','kind','buyer'],name='unique_chat_charge'),models.CheckConstraint(condition=Q(amount_cents=models.F('creator_cents')+models.F('platform_cents')),name='chat_charge_conserved')]
