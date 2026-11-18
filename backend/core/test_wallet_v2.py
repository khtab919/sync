"""USDT payout regression tests: wallet reservations, provider IPN and private notices."""
import hashlib
import hmac
import json
import uuid
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings
from .models import Profile, UserNotification, Wallet, Withdrawal
from .views import issue_token


@override_settings(SECURE_SSL_REDIRECT=False, PASSWORD_HASHERS=['django.contrib.auth.hashers.MD5PasswordHasher'])
class WalletV2Tests(TestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_user(
            username='creator_v2', email='creator-v2@example.org', password='test_pass_123')
        self.other = get_user_model().objects.create_user(
            username='other_v2', email='other-v2@example.org', password='test_pass_123')
        self.admin = get_user_model().objects.create_superuser(
            username='admin_v2', email='admin-v2@example.org', password='test_pass_123')
        for person in (self.user, self.other, self.admin):
            Profile.objects.create(user=person, publishing_allowed=True)
            Wallet.objects.create(user=person, balance_cents=5000 if person == self.user else 0)
        self.user_token = issue_token(self.user)
        self.other_token = issue_token(self.other)
        self.admin_token = issue_token(self.admin)
        self.secret = 'unit-test-only-payout-ipn-key'
        self.destination = 'T' + 'A' * 33

    def post(self, path, data, token=None, secure=False):
        return self.client.post(
            path, json.dumps(data), content_type='application/json',
            HTTP_AUTHORIZATION='Bearer ' + (token or self.user_token), secure=secure
        )

    def get(self, path, token=None):
        return self.client.get(
            path, HTTP_AUTHORIZATION='Bearer ' + (token or self.user_token)
        )

    def create(self, network='usdttrc20', destination=None, key=None, amount=1000):
        body = {'network': network, 'destination': destination or self.destination,
                'request_key': key or str(uuid.uuid4()), 'amount_cents': amount}
        return self.post('/api/withdrawals', body)

    def administration(self, row, action, **extra):
        return self.post('/api/admin/payouts',
                         {'id': str(row.pk), 'action': action,
                          'reason': 'Payment reviewed', **extra},
                         token=self.admin_token, secure=True)

    def signed_callback(self, body):
        canonical = json.dumps(body, sort_keys=True, separators=(',', ':'),
                               ensure_ascii=False, allow_nan=False).encode()
        signature = hmac.new(self.secret.encode(), canonical, hashlib.sha512).hexdigest()
        return self.client.post('/api/nowpayments/payout-ipn',
            json.dumps(body), content_type='application/json',
            HTTP_X_NOWPAYMENTS_SIG=signature)

    def test_network_validation_reservation_and_idempotence(self):
        self.assertEqual(self.create(network='BTC').status_code, 400)
        self.assertEqual(self.create(network='usdtbsc').status_code, 400)
        bsc = '0x' + 'a' * 40
        self.assertEqual(self.create(network='usdtbsc', destination=bsc).status_code, 201)
        Wallet.objects.get(user=self.user).refresh_from_db()
        self.assertEqual(Wallet.objects.get(user=self.user).reserved_cents, 1000)
        key = str(uuid.uuid4())
        body = {'network': 'usdttrc20', 'destination': self.destination,
                'request_key': key, 'amount_cents': 500}
        first = self.post('/api/withdrawals', body)
        self.assertEqual(first.status_code, 201, first.content)
        self.assertEqual(self.post('/api/withdrawals', body).status_code, 200)
        body['amount_cents'] = 600
        self.assertEqual(self.post('/api/withdrawals', body).status_code, 409)
        self.assertEqual(Wallet.objects.get(user=self.user).reserved_cents, 1500)
        self.assertEqual(self.create(amount=4000).status_code, 409)

    def test_legacy_destination_can_never_be_dispatched(self):
        legacy = self.post('/api/withdrawals', {
            'request_key': str(uuid.uuid4()), 'amount_cents': 500,
            'destination': 'legacy reference'
        })
        self.assertEqual(legacy.status_code, 201)
        row = Withdrawal.objects.get(pk=legacy.json()['id'])
        self.assertEqual(row.network, 'legacy')
        self.assertEqual(self.administration(row, 'approve').status_code, 409)

    def test_rejection_returns_only_reserved_funds_and_notifies(self):
        row = Withdrawal.objects.get(pk=self.create().json()['id'])
        self.assertEqual(self.administration(row, 'reject').status_code, 200)
        wallet = Wallet.objects.get(user=self.user)
        self.assertEqual((wallet.balance_cents, wallet.reserved_cents), (5000, 0))
        self.assertEqual(Withdrawal.objects.get(pk=row.pk).status, 'rejected')
        self.assertGreaterEqual(UserNotification.objects.filter(user=self.user).count(), 2)

    def test_payout_only_settles_on_signed_finished_ipn_once(self):
        row = Withdrawal.objects.get(pk=self.create().json()['id'])
        self.assertEqual(self.administration(row, 'approve').status_code, 200)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 5000)

        env = {'NOWPAYMENTS_API_KEY':'test-only-key',
               'NOWPAYMENTS_PAYOUT_EMAIL':'test@example.org',
               'NOWPAYMENTS_PAYOUT_PASSWORD':'test-pass',
               'NOWPAYMENTS_IPN_SECRET':self.secret}
        with patch.dict('os.environ', env), patch(
                'core.wallet_v2._payout_create', return_value=('12345', '67890')):
            dispatched = self.administration(row, 'dispatch')
        self.assertEqual(dispatched.status_code, 200, dispatched.content)
        self.assertEqual(dispatched.json()['status'], 'verification_required')
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 5000)
        self.assertEqual(self.administration(row, 'dispatch').status_code, 409)
        with patch('core.wallet_v2._payout_verify', return_value={}):
            verified = self.administration(row, 'verify', verification_code='123456')
        self.assertEqual(verified.status_code, 200, verified.content)

        completed = {'id': '67890', 'currency': 'usdttrc20',
                     'address': self.destination, 'amount': '10.00',
                     'hash': 'a' * 64, 'status': 'FINISHED'}
        with patch.dict('os.environ', {'NOWPAYMENTS_IPN_SECRET': self.secret}):
            self.assertEqual(self.signed_callback(completed).status_code, 200)
            self.assertEqual(self.signed_callback(completed).status_code, 200)
            self.assertEqual(self.signed_callback(
                {**completed, 'id':'11111'}).status_code, 400)
        wallet = Wallet.objects.get(user=self.user)
        self.assertEqual((wallet.balance_cents, wallet.reserved_cents), (4000, 0))
        row.refresh_from_db()
        self.assertEqual(row.status, 'paid')
        self.assertEqual(row.payout_reference, 'a' * 64)
        self.assertEqual(UserNotification.objects.filter(
            user=self.user, title='اكتمل سحب USDT').count(), 1)

    def test_bad_webhook_rejected_and_provider_failure_keeps_hold(self):
        row = Withdrawal.objects.get(pk=self.create().json()['id'])
        self.assertEqual(self.administration(row, 'approve').status_code, 200)
        with patch.dict('os.environ', {
                'NOWPAYMENTS_API_KEY':'test', 'NOWPAYMENTS_PAYOUT_EMAIL':'test@example.org',
                'NOWPAYMENTS_PAYOUT_PASSWORD':'pw', 'NOWPAYMENTS_IPN_SECRET':self.secret
            }), patch('core.wallet_v2._payout_create', side_effect=TimeoutError()):
            result = self.administration(row, 'dispatch')
        self.assertEqual(result.status_code, 409)
        row.refresh_from_db()
        self.assertEqual(row.status, 'dispatch_review')
        self.assertEqual(Wallet.objects.get(user=self.user).reserved_cents, 1000)
        self.assertEqual(self.administration(row, 'dispatch').status_code, 409)
        invalid = self.client.post('/api/nowpayments/payout-ipn', json.dumps(
            {'status':'FINISHED'}), content_type='application/json',
            HTTP_X_NOWPAYMENTS_SIG='b' * 128)
        self.assertEqual(invalid.status_code, 503)  # No configured secret here.
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 5000)

    def test_private_notification_read_state(self):
        self.create()
        notices = self.get('/api/notifications').json()
        self.assertGreaterEqual(notices['unread'], 1)
        other = self.get('/api/notifications', self.other_token).json()
        self.assertEqual(other['unread'], 0)
        item_id = notices['items'][0]['id']
        self.assertEqual(self.post('/api/notifications', {'ids':[item_id]}).status_code, 200)
        self.assertEqual(self.get('/api/notifications').json()['unread'],
            notices['unread'] - 1)
        self.assertEqual(self.post('/api/notifications', {'ids':[item_id]},
            token=self.other_token).json()['read'], 0)
