"""Native USDT Tron payment regression tests. Provider calls are mocked."""
import hashlib
import hmac
import json
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings

from .models import GatewayInvoice, Profile, Wallet
from .views import issue_token


@override_settings(SECURE_SSL_REDIRECT=False)
class NowPaymentsDirectTests(TestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_user(
            'trc20_user', email='trc20@example.com', password='Safe-pass-test-2026'
        )
        Profile.objects.create(user=self.user)
        Wallet.objects.create(user=self.user)
        self.token = issue_token(self.user)
        self.key = 'test_ipn_secret_for_nowpayments_2026'
        self.patch_env = patch.dict('os.environ', {
            'NOWPAYMENTS_API_KEY':'test_api_key',
            'NOWPAYMENTS_IPN_SECRET':self.key,
        })
        self.patch_env.start()
        self.addCleanup(self.patch_env.stop)

    def post(self, path, data):
        return self.client.post(
            path, data=json.dumps(data), content_type='application/json',
            HTTP_AUTHORIZATION='Bearer '+self.token,
        )

    def get(self, path):
        return self.client.get(path, HTTP_AUTHORIZATION='Bearer '+self.token)

    def quote(self):
        r=self.post('/api/topup-quotes', {
            'method':'nowpayments', 'amount_cents':1000,
        })
        self.assertEqual(r.status_code,201,r.content)
        return r.json()

    def fake_payment(self, quote, status='waiting', currency='usdttrc20'):
        return {
            'payment_id':2312399001, 'order_id':quote['id'],
            'pay_address':'T'+'A'*33, 'pay_amount':'10.125',
            'price_amount':10, 'price_currency':'usd',
            'pay_currency':currency, 'payment_status':status,
            'actually_paid': '10.125' if status=='finished' else 0,
        }

    def setup_payment(self):
        quote=self.quote()
        fake=self.fake_payment(quote)
        with patch('core.nowpayments.provider_request',return_value=fake) as provider:
            result=self.post('/api/nowpayments/direct',{'quote_id':quote['id']})
        self.assertEqual(result.status_code,201,result.content)
        self.assertEqual(result.json()['pay_currency'],'usdttrc20')
        self.assertEqual(result.json()['pay_amount'],'10.125')
        self.assertEqual(result.json()['pay_address'],fake['pay_address'])
        self.assertEqual(result.json()['invoice_url'],'')
        self.assertEqual(provider.call_args.args[0],'payment')
        self.assertEqual(provider.call_args.args[1]['pay_currency'],'usdttrc20')
        return quote,fake

    def signed_ipn(self, data):
        raw=json.dumps(data,separators=(',',':')).encode()
        canonical=json.dumps(data,sort_keys=True,separators=(',',':'),
            ensure_ascii=False,allow_nan=False).encode()
        signature=hmac.new(self.key.encode(),canonical,hashlib.sha512).hexdigest()
        return self.client.post('/api/nowpayments/ipn',raw,
            content_type='application/json',HTTP_X_NOWPAYMENTS_SIG=signature)

    def test_only_trc20_in_api_and_repeated_create_is_idempotent(self):
        quote,fake=self.setup_payment()
        with patch('core.nowpayments.provider_request',return_value=fake) as provider:
            result=self.post('/api/nowpayments/direct',{'quote_id':quote['id']})
        self.assertEqual(result.status_code,200)
        self.assertEqual(provider.call_args.args[0],'payment/'+str(fake['payment_id']))
        self.assertEqual(GatewayInvoice.objects.count(),1)
        rejected=self.post('/api/nowpayments/direct',{
            'quote_id':quote['id'],'pay_currency':'btc',
        })
        self.assertEqual(rejected.status_code,400)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,0)

    def test_signed_finished_webhook_credits_once(self):
        quote,fake=self.setup_payment()
        paid=self.fake_payment(quote,status='finished')
        for _ in range(2):
            response=self.signed_ipn(paid)
            self.assertEqual(response.status_code,200,response.content)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,1000)
        self.assertIsNotNone(GatewayInvoice.objects.get(quote_id=quote['id']).credited_at)

    def test_waiting_and_wrong_network_never_credit(self):
        quote,fake=self.setup_payment()
        self.assertEqual(self.signed_ipn(fake).status_code,200)
        wrong=self.fake_payment(quote,status='finished',currency='btc')
        self.assertEqual(self.signed_ipn(wrong).status_code,400)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,0)

    def test_user_cannot_credit_from_return_or_repeated_status(self):
        quote,fake=self.setup_payment()
        self.assertEqual(self.get('/api/nowpayments/return').status_code,200)
        with patch('core.nowpayments.provider_request',return_value=fake):
            pending=self.get('/api/nowpayments/status/'+quote['id'])
        self.assertEqual(pending.status_code,200,pending.content)
        self.assertFalse(pending.json()['credited'])
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,0)
