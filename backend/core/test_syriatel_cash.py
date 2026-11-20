"""Syriatel Cash is a quoted manual funding rail; images never auto-credit money."""
import json
from decimal import Decimal
from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings
from .admin_test_support import issue_token
from .models import PaymentSettings, Profile, Wallet, TopUp, UploadedMedia


@override_settings(SECURE_SSL_REDIRECT=False)
class SyriatelCashTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.customer = User.objects.create_user('syriatel_user', password='test-only-123')
        self.admin = User.objects.create_superuser('syriatel_admin', 'syriatel-admin@example.test', 'test-only-123')
        for user in (self.customer, self.admin):
            Profile.objects.create(user=user)
            Wallet.objects.create(user=user)
        self.token = issue_token(self.customer)
        self.admin_token = issue_token(self.admin)

    def request(self, path, data=None, admin=False):
        auth = 'Bearer ' + (self.admin_token if admin else self.token)
        if data is None:
            return self.client.get(path, HTTP_AUTHORIZATION=auth)
        return self.client.post(path, json.dumps(data), content_type='application/json',
                                HTTP_AUTHORIZATION=auth, secure=admin)

    def test_disabled_until_admin_enables_rate_and_validates_number(self):
        info = self.request('/api/payment-info').json()
        self.assertEqual(info['syriatel_cash_number'], '')
        self.assertFalse(info['syriatel_cash_enabled'])
        disabled = self.request('/api/topup-quotes', {'method':'syriatelcash','amount_cents':1000})
        self.assertEqual(disabled.status_code, 409, disabled.content)
        endpoint = '/api/admin/payment-settings'
        config = {'enabled':True, 'number':'0997991404',
                  'syp_per_usd':'0', 'instructions':'التحويل ثم إرسال الإثبات'}
        unauthorized = self.request(endpoint, {'syriatel_cash':config})
        self.assertEqual(unauthorized.status_code, 403)
        config['syp_per_usd']='15000'
        invalid = self.request(endpoint, {'syriatel_cash':dict(config, number='09999') ,
                            'reason':'Set Syriatel Cash'}, admin=True)
        self.assertEqual(invalid.status_code, 400)
        result = self.request(endpoint, {'syriatel_cash':config,
                               'reason':'Set Syriatel Cash exchange rate'},admin=True)
        self.assertEqual(result.status_code, 200, result.content)
        setting = PaymentSettings.objects.get(pk=1)
        self.assertTrue(setting.syriatel_cash_enabled)
        self.assertEqual(setting.syriatel_cash_number, '0997991404')
        self.assertEqual(setting.syriatel_cash_syp_per_usd, Decimal('15000'))

    def test_immutable_quote_receipt_reference_and_no_auto_credit(self):
        settings,_=PaymentSettings.objects.update_or_create(pk=1,defaults={
            'syriatel_cash_enabled':True, 'syriatel_cash_number':'0997991404',
            'syriatel_cash_syp_per_usd':Decimal('12000')})
        quote = self.request('/api/topup-quotes',{'method':'syriatelcash','amount_cents':500})
        self.assertEqual(quote.status_code,201,quote.content)
        offer=quote.json()
        self.assertEqual(offer['payment_currency'],'SYP')
        self.assertEqual(offer['destination'],'0997991404')
        self.assertEqual(offer['payment_amount'],'60000.00')
        self.assertEqual(self.request('/api/topup-quotes',
                         {'method':'syriatelcash','amount_cents':500}).json()['id'],offer['id'])
        no_receipt=self.request('/api/topups',{'quote_id':offer['id'],
                                             'transaction_hash':'ref9876','receipt_id':'bad'})
        self.assertEqual(no_receipt.status_code,400)
        self.assertEqual(Wallet.objects.get(user=self.customer).balance_cents,0)
        receipt=UploadedMedia.objects.create(owner=self.customer,purpose='receipt',
             file='receipts/test.png',mime='image/png',size=256)
        data={'quote_id':offer['id'], 'transaction_hash':'ref9876',
              'receipt_id':str(receipt.id)}
        posted=self.request('/api/topups',data)
        self.assertEqual(posted.status_code,201,posted.content)
        row=TopUp.objects.get()
        self.assertEqual(row.method,'syriatelcash')
        self.assertEqual(row.status,'pending')
        self.assertEqual(row.transaction_hash,'syriatelcash:ref9876')
        self.assertEqual(Wallet.objects.get(user=self.customer).balance_cents,0)
        duplicated=self.request('/api/topups',data)
        self.assertEqual(duplicated.status_code,200,duplicated.content)
        self.assertEqual(TopUp.objects.count(),1)
        self.assertEqual(Wallet.objects.get(user=self.customer).balance_cents,0)
        view=self.request('/api/topups').json()['items']
        self.assertEqual(view[0]['method'],'syriatelcash')
