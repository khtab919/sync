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
            Profile.objects.create(user=user, date_of_birth=date(1990, 1, 1), creator=user == self.creator, identity_verified=False)
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

    def test_registration_without_birth_date_requires_explicit_adult_confirmation(self):
        p = {'username': 'adultnew', 'email': 'adult@example.com', 'password': 'unique-new-account-pass-2026!', 'accept_non_explicit_policy': True}
        self.assertEqual(self.post('/api/register', p).status_code, 400)
        p['adult_confirmed'] = 'true'
        self.assertEqual(self.post('/api/register', p).status_code, 400)
        p['adult_confirmed'] = False
        self.assertEqual(self.post('/api/register', p).status_code, 400)
        p['adult_confirmed'] = True
        p['accept_non_explicit_policy'] = False
        self.assertEqual(self.post('/api/register', p).status_code, 400)
        p['accept_non_explicit_policy'] = True
        self.assertEqual(self.post('/api/register', p).status_code, 201)
        profile = Profile.objects.get(user__username='adultnew')
        self.assertIsNone(profile.date_of_birth)

    def test_registration_requires_name_and_email_with_simple_password(self):
        p = {'username': '  لي  ', 'password': 'Sunrise8', 'adult_confirmed': True, 'accept_non_explicit_policy': True}
        self.assertEqual(self.post('/api/register', p).status_code, 400)
        p['email'] = ''
        self.assertEqual(self.post('/api/register', p).status_code, 400)
        p['email'] = 'member@example.com'
        response = self.post('/api/register', p)
        self.assertEqual(response.status_code, 201)
        user = get_user_model().objects.get(username='لي')
        self.assertEqual(user.email, 'member@example.com')
        self.assertTrue(user.check_password('Sunrise8'))
        self.assertEqual(self.post('/api/login', {'username': 'لي', 'password': 'Sunrise8'}).status_code, 200)

    def test_simple_registration_errors_identify_the_field(self):
        p = {'username': 'newmember', 'email': 'newmember@example.com', 'password': 'short', 'adult_confirmed': True, 'accept_non_explicit_policy': True}
        self.assertEqual(self.post('/api/register', p).json()['error'], 'invalid_password')
        p['password'] = '12345678'
        self.assertEqual(self.post('/api/register', p).json()['error'], 'password_too_common')
        p['password'] = 'Sunrise8'
        p['email'] = 'not-an-email'
        self.assertEqual(self.post('/api/register', p).json()['error'], 'invalid_email')
        p['email'] = 'nour@example.com'
        p['username'] = 'Nour Hassan'
        self.assertEqual(self.post('/api/register', p).status_code, 201)
        self.assertEqual(self.post('/api/login', {'username': ' @NOUR   HASSAN ', 'password': 'Sunrise8'}).status_code, 200)

    def test_health_advertises_simplified_registration(self):
        response = self.client.get('/health/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['registration_version'], 4)

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

    def test_reels_do_not_require_identity_but_publication_requires_creator_role(self):
        Profile.objects.filter(user=self.user).update(identity_verified=False)
        response = self.client.get('/api/reels', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(response.status_code, 200)
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

@override_settings(SECURE_SSL_REDIRECT=False)
class ReferenceInteractionTests(TestCase):
    post = ApiTests.post
    def setUp(self):
        ApiTests.setUp(self)
        self.service = Service.objects.create(owner=self.creator, title='Verified offer', description='Actual description', price_cents=200, approved=True)

    def test_reactions_are_persistent_idempotent_and_private(self):
        from .models import ServiceReaction
        path = f'/api/services/{self.service.pk}/reaction'
        for _ in range(2):
            self.assertEqual(self.post(path, {'kind': 'save', 'enabled': True}).status_code, 200)
        self.assertEqual(ServiceReaction.objects.count(), 1)
        result = self.client.get('/api/saved', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(result.json()['items'][0]['id'], self.service.pk)
        other = self.client.get('/api/saved', HTTP_AUTHORIZATION='Bearer ' + self.creator_token)
        self.assertEqual(other.json()['items'], [])
        self.post(path, {'kind': 'save', 'enabled': False})
        self.assertEqual(ServiceReaction.objects.count(), 0)

    def test_profile_frame_and_name_are_saved_without_privilege_changes(self):
        response = self.client.patch('/api/me', json.dumps({'display_name': 'New display name', 'selected_frame': 2, 'bio': 'New bio'}), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['selected_frame'], 2)
        self.assertEqual(response.json()['display_name'], 'New display name')
        self.assertFalse(response.json()['creator'])
        invalid = self.client.patch('/api/me', json.dumps({'selected_frame': 11}), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(invalid.status_code, 400)

    def test_only_creator_can_change_chat_fee(self):
        payload = json.dumps({'chat_price_cents': 200})
        self.assertEqual(self.client.patch('/api/me', payload, content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + self.token).status_code, 400)
        self.assertEqual(self.client.patch('/api/me', payload, content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + self.creator_token).status_code, 200)

    def test_follow_is_idempotent_and_counts_are_real(self):
        path = f'/api/creators/{self.creator.pk}/follow'
        for _ in range(2):
            response = self.post(path, {'enabled': True})
            self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['followers_count'], 1)
        self.assertTrue(response.json()['following'])
        response = self.post(path, {'enabled': False})
        self.assertEqual(response.json()['followers_count'], 0)

    def test_support_messages_are_private_and_admin_reply_is_readable(self):
        from .models import SupportTicket
        self.assertEqual(self.post('/api/support', {'body': 'Please help with my account'}).status_code, 201)
        SupportTicket.objects.update(reply='Your request was reviewed')
        mine = self.client.get('/api/support', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(mine.json()['items'][0]['reply'], 'Your request was reviewed')
        other = self.client.get('/api/support', HTTP_AUTHORIZATION='Bearer ' + self.creator_token)
        self.assertEqual(other.json()['items'], [])

    def test_service_image_and_category_validation(self):
        data = {'title': 'A service', 'description': 'A real description', 'price_cents': 200, 'category': 'منتج', 'image_url': 'http://example.com/image.jpg'}
        self.assertEqual(self.post('/api/services', data, self.creator_token).status_code, 400)
        data['image_url'] = 'https://example.com/image.jpg'
        self.assertEqual(self.post('/api/services', data, self.creator_token).status_code, 201)


@override_settings(SECURE_SSL_REDIRECT=False)
class RelaxiaRequirementsTests(TestCase):
    post = ApiTests.post
    def setUp(self):
        ApiTests.setUp(self)

    def test_approved_reels_are_visible_without_identity_and_still_require_login(self):
        from .models import Reel
        Reel.objects.create(owner=self.creator, video_url='https://example.com/approved.mp4', approved=True)
        result = self.client.get('/api/reels', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(result.status_code, 200)
        self.assertEqual(len(result.json()['items']), 1)
        self.assertEqual(self.client.get('/api/reels').status_code, 401)

    def test_creator_publication_and_withdrawals_do_not_require_identity(self):
        self.assertFalse(Profile.objects.get(user=self.creator).identity_verified)
        result = self.post('/api/services', {'title': 'جلسة صوتية', 'description': 'جلسة تواصل بصوت واضح', 'price_cents': 200, 'category': 'جلسة'}, self.creator_token)
        self.assertEqual(result.status_code, 201)
        self.assertEqual(result.json()['status'], 'pending_review')
        Wallet.objects.filter(user=self.creator).update(balance_cents=1000)
        result = self.post('/api/withdrawals', {'request_key': str(uuid.uuid4()), 'amount_cents': 500, 'destination': 'test destination'}, self.creator_token)
        self.assertEqual(result.status_code, 201)
        self.assertEqual(result.json()['status'], 'pending')

    def test_creator_directory_and_following_do_not_require_identity(self):
        result = self.client.get('/api/creators', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(result.json()['items'][0]['id'], self.creator.pk)
        self.assertEqual(self.post(f'/api/creators/{self.creator.pk}/follow', {'enabled': True}).status_code, 200)

    def test_eleven_frames_are_stored_and_avatar_urls_are_validated(self):
        for frame in range(11):
            response = self.client.patch('/api/me', json.dumps({'selected_frame': frame}), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + self.token)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.json()['selected_frame'], frame)
        response = self.client.patch('/api/me', json.dumps({'avatar_url': 'http://example.com/a.jpg'}), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(response.status_code, 400)
        response = self.client.patch('/api/me', json.dumps({'avatar_url': 'https://example.com/a.jpg'}), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(response.status_code, 200)

    def test_educational_category_is_not_accepted(self):
        result = self.post('/api/services', {'title': 'عنوان عرض', 'description': 'وصف واضح لهذا العرض', 'price_cents': 200, 'category': 'كورس'}, self.creator_token)
        self.assertEqual(result.status_code, 400)


    def test_legacy_educational_offers_are_hidden_without_deleting_data(self):
        from .models import ServiceReaction
        service = Service.objects.create(owner=self.creator, title='Legacy listing', description='Retained history', price_cents=200, approved=True, category='كورس')
        ServiceReaction.objects.create(user=self.user, service=service, kind='save')
        for path in ('/api/services', '/api/saved'):
            result = self.client.get(path, HTTP_AUTHORIZATION='Bearer ' + self.token)
            self.assertEqual(result.json()['items'], [])
        self.assertEqual(self.post('/api/bookings', {'service_id': service.pk}).status_code, 404)
        self.assertTrue(Service.objects.filter(pk=service.pk).exists())
