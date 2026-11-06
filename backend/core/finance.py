"""Escrow operations. Callers lock all involved users in primary-key order first."""
from django.utils import timezone
from .models import Wallet,FinancialOperation
from .views import ApiError

def accounts(buyer,creator):
    for user in sorted([buyer,creator],key=lambda u:u.pk):Wallet.objects.get_or_create(user=user)
    rows={w.user_id:w for w in Wallet.objects.select_for_update().filter(user_id__in=[buyer.pk,creator.pk]).order_by('user_id')}
    return rows[buyer.pk],rows[creator.pk]

def hold(buyer,creator,kind,key,reference,amount):
    previous=FinancialOperation.objects.filter(buyer=buyer,kind=kind,request_key=key).first()
    if previous:
        if previous.creator_id!=creator.pk or previous.reference!=reference or previous.amount_cents!=amount:raise ApiError('payment_request_conflict',409)
        return previous
    source,_=accounts(buyer,creator)
    if source.balance_cents-source.reserved_cents<amount:raise ApiError('insufficient_balance',409)
    source.reserved_cents+=amount;source.save(update_fields=['reserved_cents'])
    return FinancialOperation.objects.create(buyer=buyer,creator=creator,kind=kind,request_key=key,reference=reference,amount_cents=amount)

def settle(operation):
    if operation.status!='held':return
    source,target=accounts(operation.buyer,operation.creator)
    if source.reserved_cents<operation.amount_cents:raise ApiError('financial_review_required',409)
    source.reserved_cents-=operation.amount_cents;source.balance_cents-=operation.amount_cents
    share=operation.amount_cents*80//100;target.balance_cents+=share
    source.save(update_fields=['reserved_cents','balance_cents']);target.save(update_fields=['balance_cents'])
    operation.creator_cents=share;operation.platform_cents=operation.amount_cents-share;operation.status='settled';operation.settled_at=timezone.now();operation.save()

def refund(operation,reason):
    if operation.status!='held':return
    source,_=accounts(operation.buyer,operation.creator)
    if source.reserved_cents<operation.amount_cents:raise ApiError('financial_review_required',409)
    source.reserved_cents-=operation.amount_cents;source.save(update_fields=['reserved_cents'])
    operation.status='refunded';operation.reason=reason[:300];operation.save(update_fields=['status','reason'])
