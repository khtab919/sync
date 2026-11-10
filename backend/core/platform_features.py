"""Account blocking, message actions, service escrow and financial receipts."""
import uuid
from django.db import transaction
from django.db.models import Q
from django.http import JsonResponse
from django.utils import timezone
from .views import endpoint,ApiError,User,text,amount
from .models import UserBlock,Message,Booking,Service,FinancialOperation,ChatCharge,TopUp,Withdrawal
from .mobile_admin import require_admin,audit
from . import finance

def blocked(a,b):return UserBlock.objects.filter(Q(user=a,blocked=b)|Q(user=b,blocked=a)).exists()

@endpoint(['GET','POST'])
def blocks(request):
    if request.method=='POST':
        target=User.objects.filter(pk=amount(request.payload,'user_id')).exclude(pk=request.actor.pk).first()
        if not target:raise ApiError('account_unavailable',404)
        if request.payload.get('blocked') is True:
            UserBlock.objects.get_or_create(user=request.actor,blocked=target)
            from .models import InternalCall
            InternalCall.objects.filter(Q(caller=request.actor,callee=target)|Q(caller=target,callee=request.actor),status__in=['ringing','accepted']).update(status='ended',ended_at=timezone.now())
        else:UserBlock.objects.filter(user=request.actor,blocked=target).delete()
    return JsonResponse({'items':[{'id':b.blocked_id,'name':getattr(getattr(b.blocked,'profile',None),'display_name','') or b.blocked.username} for b in UserBlock.objects.filter(user=request.actor).select_related('blocked__profile')]})

@endpoint(['POST'])
def message_action(request,message_id):
    row=Message.objects.filter(pk=message_id).filter(Q(sender=request.actor)|Q(recipient=request.actor)).first()
    if not row:raise ApiError('message_unavailable',404)
    if request.payload.get('action')!='delete' or row.sender_id!=request.actor.pk:raise ApiError('message_action_forbidden',403)
    row.deleted_at=row.deleted_at or timezone.now();row.body='';row.save(update_fields=['deleted_at','body'])
    # Financial and moderation evidence stays; deleted media grants are invalidated below.
    return JsonResponse({'ok':True})

@endpoint(['POST'])
def booking_action(request,booking_id):
    preview=Booking.objects.select_related('service__owner','buyer').filter(pk=booking_id).first()
    if not preview or (request.actor.pk not in (preview.buyer_id,preview.service.owner_id) and not (request.path.startswith('/api/admin/') and 'store.edit' in __import__('core.admin_security',fromlist=['scopes']).scopes(request.actor))):raise ApiError('booking_unavailable',404)
    action=request.payload.get('action')
    with transaction.atomic():
        list(User.objects.select_for_update().filter(pk__in=sorted([preview.buyer_id,preview.service.owner_id])).order_by('pk'))
        row=Booking.objects.select_for_update(of=('self',)).select_related('operation__buyer','operation__creator','service__owner','buyer').get(pk=booking_id)
        if action=='accept':
            if request.actor.pk!=row.service.owner_id or row.status!='paid_reserved':raise ApiError('invalid_booking_action',409)
            row.status='accepted'
        elif action=='complete':
            if request.actor.pk!=row.buyer_id or row.status!='accepted':raise ApiError('invalid_booking_action',409)
            finance.settle(row.operation);row.status='completed'
        elif action in ('cancel','reject','refund'):
            if action=='cancel' and (request.actor.pk!=row.buyer_id or row.status not in ('paid_reserved','awaiting_payment')):raise ApiError('invalid_booking_action',409)
            if action=='reject' and request.actor.pk!=row.service.owner_id:raise ApiError('invalid_booking_action',403)
            if action=='refund':
                require_admin(request)
                text(request.payload,'reason',3,300)
            if row.operation_id:
                if row.operation.status=='settled':raise ApiError('financial_review_required',409)
                finance.refund(row.operation,request.payload.get('reason','إلغاء الحجز قبل التسوية'))
            row.status='refunded' if row.operation_id else 'cancelled'
        else:raise ApiError('invalid_action')
        row.save(update_fields=['status']);audit(request.actor,row,'Booking '+action)
    return JsonResponse({'id':row.pk,'status':row.status})

@endpoint(['GET'])
def financial_history(request, reference=None):
    def scope(query, kind, field="pk"):
        if reference is None:return query
        prefix, value = reference.split(":",1)
        return query.filter(**{field:value}) if prefix==kind else query.none()
    items=[]
    for op in scope(FinancialOperation.objects.filter(Q(buyer=request.actor)|Q(creator=request.actor)).select_related('buyer','creator'),'op').order_by('-created_at')[:100]:
        incoming=op.creator_id==request.actor.pk
        items.append({'id':'op:'+str(op.pk),'kind':op.kind,'status':op.status,'amount_cents':(op.creator_cents if op.status=='settled' else op.amount_cents*80//100) if incoming else op.amount_cents,'incoming':incoming,'reference':op.reference,'created_at':op.created_at,'creator_share':80,'reason':op.reason})
    for row in scope(ChatCharge.objects.filter(Q(buyer=request.actor)|Q(creator=request.actor)),'chat').order_by('-created_at')[:100]:
        incoming=row.creator_id==request.actor.pk
        items.append({'id':'chat:'+str(row.pk),'kind':'chat_'+row.kind,'status':'settled','amount_cents':row.creator_cents if incoming else row.amount_cents,'incoming':incoming,'reference':str(row.message_id),'created_at':row.created_at,'creator_share':80})
    for row in scope(TopUp.objects.filter(user=request.actor),'topup').order_by('-created_at')[:50]:items.append({'id':'topup:'+str(row.pk),'kind':'topup','status':row.status,'amount_cents':row.amount_cents,'incoming':True,'reference':row.transaction_hash,'created_at':row.created_at})
    from .models import GatewayInvoice
    for row in scope(GatewayInvoice.objects.filter(quote__user=request.actor,credited_at__isnull=False).select_related('quote'),'gateway',"quote_id").order_by('-created_at')[:50]:items.append({'id':'gateway:'+str(row.quote_id),'kind':'topup','status':'approved','amount_cents':row.quote.amount_cents,'incoming':True,'reference':row.payment_id or row.invoice_id,'created_at':row.credited_at})
    for row in scope(Withdrawal.objects.filter(user=request.actor),'withdrawal').order_by('-created_at')[:100]:items.append({'id':'withdrawal:'+str(row.pk),'kind':'withdrawal','status':row.status,'amount_cents':row.amount_cents,'incoming':False,'reference':row.payout_reference or str(row.pk),'created_at':row.created_at})
    items.sort(key=lambda item:item['created_at'],reverse=True)
    return JsonResponse({'items':items[:150],'currency':'USD','units_per_usd':100})

@endpoint(['GET'])
def receipt(request,reference):
    history=__import__('json').loads(financial_history.__wrapped__(request, reference).content)['items']
    found=next((row for row in history if row['id']==reference),None)
    if found is None:raise ApiError('receipt_unavailable',404)
    found['account_name']=request.actor.profile.display_name or request.actor.username
    found['receipt_notice']='إيصال حركة رصيد داخل المنصة، وليس إثبات سحب خارجي.'
    return JsonResponse(found)

@endpoint(['GET','POST'])
def payouts(request):
    require_admin(request)
    if request.method=='POST':
        row=Withdrawal.objects.filter(pk=text(request.payload,'id',36,36)).first()
        if not row:raise ApiError('withdrawal_unavailable',404)
        with transaction.atomic():
            User.objects.select_for_update().get(pk=row.user_id)
            row=Withdrawal.objects.select_for_update().get(pk=row.pk)
            if row.status!='pending':return JsonResponse({'id':row.pk,'status':row.status})
            from .models import Wallet
            wallet=Wallet.objects.select_for_update().get(user_id=row.user_id)
            if wallet.reserved_cents<row.amount_cents:raise ApiError('financial_review_required',409)
            action=request.payload.get('action')
            if action=='paid':
                if request.payload.get('paid_verified') is not True:raise ApiError('payout_verification_required',409)
                row.payout_reference=text(request.payload,'payout_reference',6,300);wallet.balance_cents-=row.amount_cents;row.status='paid'
            elif action=='reject':row.status='rejected'
            else:raise ApiError('invalid_action')
            wallet.reserved_cents-=row.amount_cents;wallet.save(update_fields=['balance_cents','reserved_cents']);row.processed_at=timezone.now();row.save();audit(request.actor,row,'Withdrawal '+action)
    return JsonResponse({'items':[{'id':r.pk,'user_id':r.user_id,'name':r.user.profile.display_name or r.user.username,'amount_cents':r.amount_cents,'destination':r.destination,'status':r.status,'payout_reference':r.payout_reference,'processed_at':r.processed_at} for r in Withdrawal.objects.select_related('user__profile').order_by('-created_at')[:100]]})

@endpoint(['GET','POST'])
def admin_bookings(request):
    require_admin(request)
    if request.method=='POST':
        if request.payload.get('action')!='refund':raise ApiError('invalid_action')
        return booking_action.__wrapped__(request,uuid.UUID(text(request.payload,'id',36,36)))
    return JsonResponse({'items':[{'id':r.pk,'title':r.service.title,'buyer':r.buyer.username,'owner':r.service.owner.username,'status':r.status,'price_cents':r.price_cents,'payment_status':r.operation.status if r.operation_id else 'unpaid'} for r in Booking.objects.select_related('service__owner','buyer','operation').order_by('-created_at')[:100]]})
