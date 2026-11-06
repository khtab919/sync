import json
import uuid
from datetime import date
from django.test import TestCase, TransactionTestCase, override_settings
from django.contrib.auth import get_user_model
from .models import Profile, Wallet, Service, Withdrawal, Message
from .views import issue_token

@override_settings(SECURE_SSL_REDIRECT=False)
class ApiTests(TestCase):
    def setUp(self):
        self.user = get_user_model().objects.create_user('buyer', password='unique-pass-2026!')
        self.creator = get_user_model().objects.create_user('creator', password='unique-pass-2026!')
        for user in (self.user, self.creator):
            Profile.objects.create(user=user, date_of_birth=date(1990, 1, 1), creator=user == self.creator, identity_verified=True)
            Wallet.objects.create(user=user)
        self.token = issue_token(self.user)
        self.creator_token = issue_token(self.creator)

    def post(self, path, data, token=None):
        return self.client.post(path, json.dumps(data), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + (token or self.token))

    def test_registration_enforces_age_and_policy(self):
        p = {'username': 'newuser', 'email': 'new@example.com', 'password': 'some-unique-2026-password!', 'date_of_birth': date.today().isoformat(), 'accept_non_explicit_policy': True}
        self.assertEqual(self.post('/api/register', p).status_code, 400)
        p['date_of_birth'] = '1990-01-01'
        p['accept_non_explicit_policy'] = False
        self.assertEqual(self.post('/api/register', p).status_code, 400)
        p['accept_non_explicit_policy'] = True
        self.assertEqual(self.post('/api/register', p).status_code, 201)
        user = get_user_model().objects.get(username='newuser')
        self.assertFalse(user.is_staff)
        self.assertTrue(user.check_password(p['password']))

    def test_booking_uses_server_price_and_is_unpaid(self):
        service = Service.objects.create(owner=self.creator, title='Call', description='Description', price_cents=200, approved=True)
        response = self.post('/api/bookings', {'service_id': service.pk, 'price_cents': 1, 'status': 'confirmed'})
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()['price_cents'], 200)
        self.assertEqual(response.json()['status'], 'awaiting_payment')

    def test_unapproved_service_is_hidden_and_cannot_be_booked(self):
        service = Service.objects.create(owner=self.creator, title='Call', description='Description', price_cents=200)
        self.assertEqual(self.post('/api/bookings', {'service_id': service.pk}).status_code, 404)
        response = self.client.get('/api/services', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(response.json()['items'], [])

    def test_client_cannot_set_balance_or_role(self):
        response = self.client.patch('/api/me', json.dumps({'walletBalanceUsd': 5000, 'creator': True}), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(response.status_code, 400)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 0)

    def test_withdrawal_reservation_and_idempotency(self):
        Wallet.objects.filter(user=self.creator).update(balance_cents=1000)
        data = {'amount_cents': 600, 'destination': 'test-destination', 'request_key': str(uuid.uuid4())}
        first = self.post('/api/withdrawals', data, self.creator_token)
        second = self.post('/api/withdrawals', data, self.creator_token)
        self.assertEqual(first.status_code, 201)
        self.assertEqual(first.json()['id'], second.json()['id'])
        self.assertEqual(Withdrawal.objects.count(), 1)
        self.assertEqual(Wallet.objects.get(user=self.creator).reserved_cents, 600)
        data['request_key'] = str(uuid.uuid4())
        self.assertEqual(self.post('/api/withdrawals', data, self.creator_token).status_code, 409)

    def test_paid_chat_does_not_send_without_payment(self):
        Profile.objects.filter(user=self.creator).update(chat_price_cents=100)
        self.assertEqual(self.post(f'/api/messages/{self.creator.pk}', {'body': 'hello'}).status_code, 409)
        self.assertEqual(Message.objects.count(), 0)

    def test_banned_user_token_and_logout(self):
        self.user.is_active = False
        self.user.save()
        self.assertEqual(self.post('/api/logout', {}).status_code, 401)
        self.user.is_active = True
        self.user.save()
        self.assertEqual(self.post('/api/logout', {}).status_code, 200)
        self.assertEqual(self.post('/api/logout', {}).status_code, 401)

    def test_free_chat_only_visible_to_participants(self):
        response = self.post(f'/api/messages/{self.creator.pk}', {'body': 'hello'})
        self.assertEqual(response.status_code, 201)
        outsider = get_user_model().objects.create_user('outsider', password='unique-pass-2026!')
        outsider_token = issue_token(outsider)
        result = self.client.get(f'/api/messages/{self.creator.pk}', HTTP_AUTHORIZATION='Bearer ' + outsider_token)
        self.assertEqual(result.json()['items'], [])

    def test_unverified_user_cannot_watch_reels_or_publish(self):
        Profile.objects.filter(user=self.user).update(identity_verified=False)
        response = self.client.get('/api/reels', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(response.status_code, 403)
        self.assertEqual(self.post('/api/services', {'title': 'test', 'description': 'test description', 'price_cents': 200}).status_code, 403)

    def test_login_rate_limit(self):
        for _ in range(20):
            self.assertEqual(self.post('/api/login', {'username': 'buyer', 'password': 'wrong'}).status_code, 401)
        self.assertEqual(self.post('/api/login', {'username': 'buyer', 'password': 'wrong'}).status_code, 429)


@override_settings(SECURE_SSL_REDIRECT=False)
class WithdrawalConcurrencyTests(TransactionTestCase):
    def test_parallel_requests_cannot_overdraw(self):
        from concurrent.futures import ThreadPoolExecutor
        from django.db import connection, close_old_connections, connections
        from django.test import Client
        if connection.vendor != 'postgresql':
            self.skipTest('Row locking requires PostgreSQL')
        user = get_user_model().objects.create_user('concurrent', password='unique-pass-2026!')
        Profile.objects.create(user=user, date_of_birth=date(1990, 1, 1), creator=True, identity_verified=True)
        Wallet.objects.create(user=user, balance_cents=1000)
        token = issue_token(user)
        def withdraw(_):
            close_old_connections()
            try:
                response = Client().post('/api/withdrawals', json.dumps({'request_key': str(uuid.uuid4()), 'amount_cents': 700, 'destination': 'test-destination'}), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + token)
                return response.status_code
            finally:
                connections.close_all()
        with ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(withdraw, range(2)))
        self.assertEqual(sorted(results), [201, 409])
        self.assertEqual(Wallet.objects.get(user=user).reserved_cents, 700)
