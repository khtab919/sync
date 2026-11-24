"""Server-side Stripe Checkout for Relaxia coin packages.

Never trust a browser redirect as payment proof; only authenticated Stripe
webhooks can credit coins. Keys and signature secrets are server-only.
"""
import hashlib
import hmac
import json
import os
import re
import time
import uuid
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen

from django.db import transaction
from django.http import HttpResponse, JsonResponse
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt

from .models import StripeCheckout, TopUpQuote, Wallet
from .views import ApiError, endpoint, text

PAYMENT_EVENTS = {
    'checkout.session.completed',
    'checkout.session.async_payment_succeeded',
    'checkout.session.async_payment_failed',
    'checkout.session.expired',
}
REVIEW_EVENTS = {'charge.refunded', 'charge.dispute.created', 'charge.dispute.closed'}


def configured():
    """Feature is opt-in; a configured account is not proof of Stripe approval."""
    key = os.environ.get('STRIPE_SECRET_KEY', '').strip()
    secret = os.environ.get('STRIPE_WEBHOOK_SECRET', '').strip()
    return (
        os.environ.get('STRIPE_ENABLED') == '1'
        and key.startswith(('sk_test_', 'sk_live_', 'rk_test_', 'rk_live_')) and len(key) > 12
        and secret.startswith('whsec_') and len(secret) > 12
    )


def _live_mode():
    return os.environ.get('STRIPE_SECRET_KEY', '').strip().startswith(('sk_live_', 'rk_live_'))


def _stripe_api_create(quote, request):
    key = os.environ['STRIPE_SECRET_KEY'].strip()
    origin = request.build_absolute_uri('/').rstrip('/')
    fields = {
        'mode': 'payment',
        'payment_method_types[0]': 'card',
        'client_reference_id': str(quote.pk),
        'metadata[quote_id]': str(quote.pk),
        'metadata[user_id]': str(quote.user_id),
        'line_items[0][price_data][currency]': 'usd',
        'line_items[0][price_data][unit_amount]': str(quote.amount_cents),
        'line_items[0][price_data][product_data][name]': 'Relaxia coins (' + str(quote.amount_cents) + ')',
        'line_items[0][quantity]': '1',
        'success_url': origin + '/api/stripe/return?status=success',
        'cancel_url': origin + '/api/stripe/return?status=cancel',
        # Checkout Session must be 30min or more from creation to expire.
        'expires_at': str(int(time.time()) + 1920),  # 2-minute clock/latency margin beyond Stripe's 30-minute minimum
    }
    req = Request(
        'https://api.stripe.com/v1/checkout/sessions',
        data=urlencode(fields).encode('utf-8'),
        method='POST',
        headers={
            'Authorization': 'Bearer ' + key,
            'Content-Type': 'application/x-www-form-urlencoded',
            'Idempotency-Key': 'relaxia-topup-' + str(quote.pk),
        },
    )
    with urlopen(req, timeout=20) as reply:
        body = reply.read(65537)
    if len(body) > 65536:
        raise ValueError('stripe_response_too_large')
    result = json.loads(body)
    if not isinstance(result, dict):
        raise ValueError('invalid_stripe_response')
    return result


def _session_valid_for_quote(data, quote, require_paid=False):
    meta = data.get('metadata') or {}
    if not isinstance(meta, dict):
        return False
    sid = data.get('id', '')
    pi = data.get('payment_intent')
    if not (isinstance(sid, str) and re.fullmatch(r'cs_(?:test|live)_[A-Za-z0-9]+', sid)):
        return False
    if not (
        data.get('object') == 'checkout.session'
        and data.get('mode') == 'payment'
        and data.get('currency') == 'usd'
        and type(data.get('amount_total')) is int
        and data['amount_total'] == quote.amount_cents
        and str(data.get('client_reference_id')) == str(quote.pk)
        and str(meta.get('quote_id')) == str(quote.pk)
        and str(meta.get('user_id')) == str(quote.user_id)
        and data.get('livemode') is _live_mode()
    ):
        return False
    if require_paid:
        return (
            data.get('payment_status') == 'paid'
            and data.get('status') == 'complete'
            and isinstance(pi, str)
            and re.fullmatch(r'pi_[A-Za-z0-9]+', pi) is not None
        )
    return True


def _response(invoice):
    return {
        'quote_id': str(invoice.quote_id),
        'checkout_url': invoice.session_url,
        'status': invoice.status,
        'credited': invoice.credited_at is not None,
        'review_required': invoice.review_required,
    }


@endpoint(['POST'])
def create_checkout(request):
    if not configured():
        raise ApiError('stripe_not_configured', 409)
    quote_id = uuid.UUID(text(request.payload, 'quote_id', 36, 36))
    with transaction.atomic():
        quote = (
            TopUpQuote.objects.select_for_update()
            .filter(pk=quote_id, user=request.actor, method='stripe')
            .first()
        )
        if not quote:
            raise ApiError('invalid_quote', 404)
        if quote.expires_at <= timezone.now():
            raise ApiError('quote_expired', 409)
        invoice, created = StripeCheckout.objects.get_or_create(quote=quote)
        if invoice.session_id and invoice.session_url:
            return JsonResponse(_response(invoice))
        if (not created and invoice.status == 'creating'
                and (timezone.now() - invoice.created_at).total_seconds() < 90):
            raise ApiError('checkout_creation_pending', 409)
        # If the process died mid-creation, retry with the original provider
        # idempotency key rather than leaving this top-up unusable forever.
        invoice.status = 'creating'
        invoice.save(update_fields=['status'])

    try:
        result = _stripe_api_create(quote, request)
        url = str(result.get('url', ''))
        parsed = urlparse(url)
        if not (
            _session_valid_for_quote(result, quote)
            and parsed.scheme == 'https'
            and parsed.hostname == 'checkout.stripe.com'
            and parsed.username is None
            and parsed.password is None
            and parsed.port in (None, 443)
            and len(url) <= 2048
        ):
            raise ValueError('stripe_checkout_response_invalid')
        with transaction.atomic():
            locked = StripeCheckout.objects.select_for_update().get(pk=invoice.pk)
            if locked.session_id and locked.session_id != result['id']:
                raise ValueError('stripe_checkout_conflict')
            locked.session_id = result['id']
            locked.session_url = url
            locked.status = 'open'
            locked.save(update_fields=['session_id', 'session_url', 'status'])
        return JsonResponse(_response(locked), status=201)
    except (ValueError, HTTPError, URLError, TimeoutError, OSError, KeyError):
        # Provider may have accepted the session before a network timeout.
        # A retry uses the SAME immutable Stripe idempotency key.
        StripeCheckout.objects.filter(pk=invoice.pk, credited_at__isnull=True).update(
            status='creation_unknown'
        )
        raise ApiError('checkout_creation_pending', 409)


@endpoint(['GET'])
def status(request, quote_id):
    invoice = (
        StripeCheckout.objects.select_related('quote')
        .filter(quote_id=quote_id, quote__user=request.actor)
        .first()
    )
    if not invoice:
        raise ApiError('checkout_unavailable', 404)
    return JsonResponse(_response(invoice))


def _verify_signature(payload, header):
    secret = os.environ.get('STRIPE_WEBHOOK_SECRET', '').strip()
    if not secret or len(payload) > 65536 or not isinstance(header, str):
        return False
    timestamp = None
    signatures = []
    for part in header.split(','):
        k, sep, v = part.strip().partition('=')
        if sep and k == 't' and timestamp is None:
            try:
                timestamp = int(v)
            except (ValueError, OverflowError):
                return False
        elif sep and k == 'v1' and re.fullmatch(r'[0-9a-fA-F]{64}', v):
            signatures.append(v.lower())
    if timestamp is None or not signatures or abs(int(time.time()) - timestamp) > 300:
        return False
    signed = str(timestamp).encode() + b'.' + payload
    expected = hmac.new(secret.encode(), signed, hashlib.sha256).hexdigest()
    return any(hmac.compare_digest(expected, value) for value in signatures)


def _apply_session(event_type, data):
    quote_id = uuid.UUID(str(data.get('client_reference_id', '')))
    with transaction.atomic():
        invoice = (
            StripeCheckout.objects.select_for_update()
            .select_related('quote')
            .filter(quote_id=quote_id, quote__method='stripe')
            .first()
        )
        if not invoice or not invoice.session_id or invoice.session_id != data.get('id'):
            # Don't trust an event before an immutable session ID is persisted.
            # Return 4xx so Stripe retries after checkout creation completes.
            raise ValueError('unknown_checkout')
        if not _session_valid_for_quote(data, invoice.quote):
            raise ValueError('stripe_session_mismatch')
        if invoice.review_required:
            return
        if event_type in ('checkout.session.completed', 'checkout.session.async_payment_succeeded'):
            if not _session_valid_for_quote(data, invoice.quote, require_paid=True):
                if data.get('payment_status') == 'unpaid':
                    return  # delayed payment; await success event
                raise ValueError('not_a_paid_checkout')
            if invoice.credited_at:
                return  # Stripe event retries or reorders; never double-credit.
            if invoice.status in ('refunded', 'disputed', 'failed'):
                invoice.review_required = True
                invoice.save(update_fields=['review_required'])
                return
            Wallet.objects.get_or_create(user_id=invoice.quote.user_id)
            wallet = Wallet.objects.select_for_update().get(user_id=invoice.quote.user_id)
            wallet.balance_cents += invoice.quote.amount_cents
            from .promotions import grant
            grant(invoice.quote.user, invoice.quote.bonus_cents,
                  'stripe:' + str(invoice.quote_id),
                  'Verified Stripe Checkout promotional points')
            wallet.save(update_fields=['balance_cents'])
            invoice.credited_at = timezone.now()
            invoice.payment_intent_id = data['payment_intent']
            invoice.status = 'paid'
            invoice.save(update_fields=['credited_at', 'payment_intent_id', 'status'])
        elif not invoice.credited_at:
            if event_type == 'checkout.session.expired':
                invoice.status = 'expired'
            elif event_type == 'checkout.session.async_payment_failed':
                invoice.status = 'failed'
            invoice.save(update_fields=['status'])


def _review_charge(data, event_type):
    pi = data.get('payment_intent')
    if not isinstance(pi, str) or not pi.startswith('pi_'):
        return
    with transaction.atomic():
        invoice = StripeCheckout.objects.select_for_update().filter(
            payment_intent_id=pi
        ).first()
        if not invoice:
            return
        invoice.status = 'disputed' if 'dispute' in event_type else 'refunded'
        invoice.review_required = True
        invoice.save(update_fields=['status', 'review_required'])
        # Do not silently remove money already spent. Flag for admin reconciliation.


@csrf_exempt
def webhook(request):
    if request.method != 'POST':
        return HttpResponse(status=405)
    if not os.environ.get('STRIPE_WEBHOOK_SECRET', '').strip():
        return HttpResponse(status=503)
    if len(request.body) > 65536:
        return HttpResponse(status=413)
    if not _verify_signature(request.body, request.headers.get('Stripe-Signature', '')):
        return HttpResponse(status=400)
    try:
        event = json.loads(request.body)
        event_type = event.get('type')
        data = (event.get('data') or {}).get('object') or {}
        if not isinstance(data, dict) or event.get('livemode') is not _live_mode():
            raise ValueError('stripe_event_mismatch')
        if event_type in PAYMENT_EVENTS:
            _apply_session(event_type, data)
        elif event_type in REVIEW_EVENTS:
            _review_charge(data, event_type)
    except (ValueError, TypeError, KeyError, OverflowError):
        return JsonResponse({'error': 'stripe_event_invalid'}, status=400)
    return JsonResponse({'ok': True})


def return_page(request):
    # A customer can visit this page without paying. It NEVER mutates a wallet.
    return HttpResponse(
        '<!doctype html><html lang="ar" dir="rtl"><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1">'
        '<title>Relaxia</title><body style="font-family:system-ui;background:#10294f;'
        'color:#fff;padding:26px"><h2>العودة إلى Relaxia</h2>'
        '<p>افتح التطبيق وتحقق من حالة الطلب. لن يضاف الرصيد قبل تأكيد Stripe '
        'الموثق على الخادم.</p></body></html>', content_type='text/html'
    )
