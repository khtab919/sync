"""Stripe-only regression checks. No network, secrets or actual charges."""
import hashlib
import hmac
import json
import time
from datetime import date
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings

from .models import Profile, PromotionalGrant, StripeCheckout, TopUpQuote, Wallet
from .views import issue_token


@override_settings(SECURE_SSL_REDIRECT=False)
class StripeCheckoutTests(TestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_user(
            username='stripe_buyer', email='stripe@example.com', password='test-pass-Stripe26'
        )
        Profile.objects.create(user=self.user, date_of_birth=date(1998, 1, 1))
        Wallet.objects.create(user=self.user)
        self.token = issue_token(self.user)
        self.secret = 'whsec_test_only_long_signature_secret_2026'
        self.env = patch.dict('os.environ', {
            'STRIPE_ENABLED': '1',
            'STRIPE_SECRET_KEY': 'sk_test_mocked_key_for_tests_12345',
            'STRIPE_WEBHOOK_SECRET': self.secret,
        })
        self.env.start()
        self.addCleanup(self.env.stop)

    def post(self, path, data):
        return self.client.post(
            path, data=json.dumps(data), content_type='application/json',
            HTTP_AUTHORIZATION='Bearer ' + self.token
        )

    def quote(self, amount=2500):
        response = self.post('/api/topup-quotes', {'amount_cents': amount, 'method': 'stripe'})
        self.assertEqual(response.status_code, 201, response.content)
        return response.json()

    def fixture(self, quote):
        return {
            'id': 'cs_test_RelaxiaExample001',
            'object': 'checkout.session',
            'mode': 'payment',
            'status': 'open',
            'payment_status': 'unpaid',
            'payment_intent': None,
            'livemode': False,
            'currency': 'usd',
            'amount_total': quote['amount_cents'],
            'client_reference_id': quote['id'],
            'metadata': {'quote_id': quote['id'], 'user_id': str(self.user.pk)},
            'url': 'https://checkout.stripe.com/c/pay/cs_test_RelaxiaExample001',
        }

    def signed_webhook(self, event, signature=None):
        raw = json.dumps(event, separators=(',', ':')).encode()
        timestamp = str(int(time.time()))
        sig = signature or hmac.new(
            self.secret.encode(), timestamp.encode() + b'.' + raw, hashlib.sha256
        ).hexdigest()
        return self.client.post(
            '/api/stripe/webhook', data=raw, content_type='application/json',
            HTTP_STRIPE_SIGNATURE='t=' + timestamp + ',v1=' + sig
        )

    def setup_checkout(self, amount=2500):
        quote = self.quote(amount)
        session = self.fixture(quote)
        with patch('core.stripe_checkout._stripe_api_create', return_value=session):
            result = self.post('/api/stripe/checkout', {'quote_id': quote['id']})
        self.assertEqual(result.status_code, 201, result.content)
        self.assertEqual(result.json()['checkout_url'], session['url'])
        return quote, session

    def test_requires_keys_and_explicit_enablement(self):
        with patch.dict('os.environ', {'STRIPE_ENABLED': '0'}):
            self.assertFalse(self.client.get('/api/payment-info',
                HTTP_AUTHORIZATION='Bearer ' + self.token).json()['stripe_enabled'])
            rejected = self.post('/api/topup-quotes', {
                'amount_cents': 2500, 'method': 'stripe'
            })
            self.assertEqual(rejected.status_code, 409)
        self.assertTrue(self.client.get('/api/payment-info',
            HTTP_AUTHORIZATION='Bearer ' + self.token).json()['stripe_enabled'])

    def test_hosted_checkout_not_direct_manual_topup(self):
        quote, session = self.setup_checkout()
        self.assertEqual(
            self.post('/api/topups', {'quote_id': quote['id']}).status_code, 409
        )
        with patch('core.stripe_checkout._stripe_api_create') as provider:
            result = self.post('/api/stripe/checkout', {'quote_id': quote['id']})
        self.assertEqual(result.status_code, 200)
        self.assertEqual(result.json()['checkout_url'], session['url'])
        provider.assert_not_called()
        self.assertFalse(self.client.get('/api/stripe/status/' + quote['id'],
            HTTP_AUTHORIZATION='Bearer ' + self.token).json()['credited'])
        self.assertEqual(self.client.get('/api/stripe/return?status=success').status_code, 200)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 0)

    def test_signed_paid_webhook_credits_once_only(self):
        quote, session = self.setup_checkout()
        session.update(status='complete', payment_status='paid', payment_intent='pi_RelaxiaPaid001')
        event = {'id': 'evt_relaxia_001', 'type': 'checkout.session.completed',
                 'livemode': False, 'data': {'object': session}}
        for _ in range(3):
            self.assertEqual(self.signed_webhook(event).status_code, 200)
        wallet = Wallet.objects.get(user=self.user)
        self.assertEqual(wallet.balance_cents, 2500)
        self.assertEqual(PromotionalGrant.objects.filter(
            reference='stripe:' + quote['id']
        ).count(), 1)
        invoice = StripeCheckout.objects.get(quote_id=quote['id'])
        self.assertEqual(invoice.status, 'paid')
        self.assertIsNotNone(invoice.credited_at)
        self.assertEqual(self.client.get('/api/stripe/status/' + quote['id'],
            HTTP_AUTHORIZATION='Bearer ' + self.token).json()['credited'], True)

    def test_rejects_fake_signature_and_tampered_amount(self):
        quote, session = self.setup_checkout()
        session.update(status='complete', payment_status='paid', payment_intent='pi_RelaxiaPaid002')
        event = {'id': 'evt_relaxia_002', 'type': 'checkout.session.completed',
                 'livemode': False, 'data': {'object': session}}
        self.assertEqual(self.signed_webhook(event, signature='0' * 64).status_code, 400)
        session['amount_total'] = 9999
        self.assertEqual(self.signed_webhook(event).status_code, 400)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 0)

    def test_unpaid_checkout_never_credits_wallet(self):
        quote, session = self.setup_checkout()
        session.update(status='complete', payment_status='unpaid')
        event = {'id': 'evt_relaxia_wait', 'type': 'checkout.session.completed',
                 'livemode': False, 'data': {'object': session}}
        self.assertEqual(self.signed_webhook(event).status_code, 200)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 0)
        self.assertFalse(StripeCheckout.objects.get(quote_id=quote['id']).credited_at)

    def test_refunded_payment_is_flagged_for_manual_reconciliation(self):
        quote, session = self.setup_checkout()
        session.update(status='complete', payment_status='paid', payment_intent='pi_RelaxiaPaid003')
        event = {'id': 'evt_relaxia_paid3', 'type': 'checkout.session.completed',
                 'livemode': False, 'data': {'object': session}}
        self.assertEqual(self.signed_webhook(event).status_code, 200)
        refund = {'id': 'evt_refund', 'type': 'charge.refunded',
                  'livemode': False, 'data': {'object': {
                      'object': 'charge', 'payment_intent': 'pi_RelaxiaPaid003'}}}
        self.assertEqual(self.signed_webhook(refund).status_code, 200)
        invoice = StripeCheckout.objects.get(quote_id=quote['id'])
        self.assertEqual(invoice.status, 'refunded')
        self.assertTrue(invoice.review_required)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 2500)
