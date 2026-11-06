"""Hosted NOWPayments invoices; no browser return URL can credit a wallet."""
import hashlib
import hmac
import json
import os
import re
from decimal import Decimal,InvalidOperation
from urllib.request import Request,build_opener,HTTPRedirectHandler
from urllib.parse import urlparse
from django.db import transaction,IntegrityError
from django.http import JsonResponse,HttpResponse
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from .views import endpoint,ApiError,text
from .models import GatewayInvoice,TopUpQuote,Wallet


def configured():
    return bool(os.environ.get('NOWPAYMENTS_API_KEY','').strip() and os.environ.get('NOWPAYMENTS_IPN_SECRET','').strip())


def provider_request(path,body=None):
    request=Request('https://api.nowpayments.io/v1/'+path,data=json.dumps(body,separators=(',',':')).encode() if body is not None else None,headers={'x-api-key':os.environ['NOWPAYMENTS_API_KEY'].strip(),'Content-Type':'application/json','Accept':'application/json'},method='POST' if body is not None else 'GET')
    class NoRedirect(HTTPRedirectHandler):
        def redirect_request(self,*args,**kwargs):return None
    with build_opener(NoRedirect()).open(request,timeout=12) as response:
        data=response.read(262145)
        if len(data)>262144:raise ValueError('provider response too large')
        result=json.loads(data)
        if not isinstance(result,dict):raise ValueError('provider response invalid')
        return result


def invoice_json(invoice):
    return {'id':invoice.pk,'quote_id':str(invoice.quote_id),'invoice_id':invoice.invoice_id,'invoice_url':invoice.invoice_url,'status':invoice.status,'credited':invoice.credited_at is not None,'review_required':invoice.review_required}


@endpoint(['POST'])
def create_invoice(request):
    if not configured():raise ApiError('nowpayments_not_configured',409)
    import uuid
    quote_id=uuid.UUID(text(request.payload,'quote_id',36,36))
    with transaction.atomic():
        quote=TopUpQuote.objects.select_for_update().filter(pk=quote_id,user=request.actor,method='nowpayments').first()
        if not quote:raise ApiError('invalid_quote')
        invoice=GatewayInvoice.objects.filter(quote=quote).first()
        if invoice:
            if invoice.invoice_url:return JsonResponse(invoice_json(invoice))
            raise ApiError('invoice_creation_pending',409)
        if quote.expires_at<timezone.now():raise ApiError('quote_expired',409)
        invoice=GatewayInvoice.objects.create(quote=quote)
    try:
        result=provider_request('invoice',{'price_amount':str(Decimal(quote.amount_cents)/100),'price_currency':'usd','order_id':str(quote.pk),'order_description':f'Relaxia coin balance: {quote.amount_cents+quote.bonus_cents} coins','ipn_callback_url':request.build_absolute_uri('/api/nowpayments/ipn'),'success_url':request.build_absolute_uri('/api/nowpayments/return'),'cancel_url':request.build_absolute_uri('/api/nowpayments/return?cancel=1')})
        url=str(result.get('invoice_url',''));parsed=urlparse(url);identifier=str(result.get('id',''))
        if parsed.scheme!='https' or parsed.hostname not in ('nowpayments.io','www.nowpayments.io') or parsed.username or parsed.port not in (None,443) or not identifier.isdigit() or len(identifier)>64 or len(url)>500:raise ValueError('invalid provider invoice')
        with transaction.atomic():
            locked=GatewayInvoice.objects.select_for_update().get(pk=invoice.pk)
            locked.invoice_id=identifier;locked.invoice_url=url
            if not locked.credited_at:locked.status='waiting'
            locked.save(update_fields=['invoice_id','invoice_url','status'])
        return JsonResponse(invoice_json(locked),status=201)
    except Exception:
        GatewayInvoice.objects.filter(pk=invoice.pk,credited_at__isnull=True).update(status='creation_unknown',review_required=True)
        raise ApiError('invoice_creation_pending',409)


def apply_payment(data):
    import uuid
    quote_id=uuid.UUID(str(data.get('order_id','')))
    payment_id=str(data.get('payment_id',''))
    if not payment_id.isdigit() or len(payment_id)>64:raise ValueError('invalid payment id')
    status=str(data.get('payment_status',''))
    with transaction.atomic():
        invoice=GatewayInvoice.objects.select_for_update().select_related('quote').filter(quote_id=quote_id,quote__method='nowpayments').first()
        if not invoice:raise ValueError('unknown invoice')
        # The signed merchant order id is mandatory; invoice id is additionally bound when present.
        incoming_invoice=data.get('invoice_id')
        if incoming_invoice is not None and invoice.invoice_id is not None and str(incoming_invoice)!=invoice.invoice_id:raise ValueError('invoice mismatch')
        if invoice.credited_at and invoice.payment_id!=payment_id:
            invoice.review_required=True;invoice.save(update_fields=['review_required']);return invoice
        price=Decimal(str(data.get('price_amount','0')))
        if not price.is_finite() or price!=Decimal(invoice.quote.amount_cents)/100 or str(data.get('price_currency','')).lower()!='usd':raise ValueError('price mismatch')
        if invoice.credited_at:
            if status in ('refunded','failed'):
                invoice.review_required=True;invoice.save(update_fields=['review_required'])
            return invoice
        invoice.payment_id=payment_id
        invoice.status=status if status in ('waiting','confirming','confirmed','sending','finished','failed','expired','refunded','partially_paid') else 'review_required'
        if status=='finished':
            expected=Decimal(str(data.get('pay_amount','0')));paid=Decimal(str(data.get('actually_paid','0')))
            if not expected.is_finite() or not paid.is_finite() or expected<=0 or paid<expected:raise ValueError('insufficient provider payment')
            Wallet.objects.get_or_create(user_id=invoice.quote.user_id)
            wallet=Wallet.objects.select_for_update().get(user_id=invoice.quote.user_id)
            wallet.balance_cents+=invoice.quote.amount_cents+invoice.quote.bonus_cents
            wallet.save(update_fields=['balance_cents']);invoice.credited_at=timezone.now()
        invoice.save(update_fields=['payment_id','status','credited_at'])
    return invoice


@csrf_exempt
def ipn(request):
    if request.method!='POST':return HttpResponse(status=405)
    if not configured():return JsonResponse({'error':'gateway_not_configured'},status=503)
    if len(request.body)>65536:return HttpResponse(status=413)
    signature=request.headers.get('x-nowpayments-sig','')
    if not re.fullmatch('[0-9a-fA-F]{128}',signature):return HttpResponse(status=403)
    try:
        data=json.loads(request.body)
        if not isinstance(data,dict):raise ValueError()
        canonical=json.dumps(data,sort_keys=True,separators=(',',':'),ensure_ascii=False,allow_nan=False).encode()
        expected=hmac.new(os.environ['NOWPAYMENTS_IPN_SECRET'].strip().encode(),canonical,hashlib.sha512).hexdigest()
        if not hmac.compare_digest(expected,signature.lower()):return HttpResponse(status=403)
        apply_payment(data)
    except (ValueError,TypeError,InvalidOperation,IntegrityError):return JsonResponse({'error':'payment_validation_failed'},status=400)
    return JsonResponse({'ok':True})


@endpoint(['GET'])
def status(request,quote_id):
    invoice=GatewayInvoice.objects.select_related('quote').filter(quote_id=quote_id,quote__user=request.actor).first()
    if not invoice:raise ApiError('invoice_unavailable',404)
    if invoice.payment_id and not invoice.credited_at and configured():
        try:invoice=apply_payment(provider_request('payment/'+invoice.payment_id))
        except Exception:raise ApiError('provider_check_pending',409)
    return JsonResponse(invoice_json(invoice))


def return_page(request):
    # Deliberately informational only: return URLs are never proof of payment.
    return HttpResponse('<!doctype html><html lang="ar" dir="rtl"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Relaxia</title><body style="font-family:system-ui;background:#28131e;color:#fff;padding:32px"><h1>ارجع إلى Relaxia</h1><p>سيُضاف الرصيد بعد تأكيد الدفع من البوابة. العودة إلى هذه الصفحة لا تعني اكتمال الدفع.</p></body></html>',content_type='text/html')
