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

@override_settings(SECURE_SSL_REDIRECT=False)
class AdministrationTests(TestCase):
    def setUp(self):
        self.admin = get_user_model().objects.create_superuser('administrator', 'admin@example.com', 'unique-admin-pass')
        self.user = get_user_model().objects.create_user('member', 'macitcomah@gmail.com', 'unique-member-pass')
        Profile.objects.create(user=self.user)
        self.wallet = Wallet.objects.create(user=self.user)

    def test_authorize_creator_and_ambiguous_email(self):
        from django.core.management import call_command, CommandError
        call_command('authorize_creator', 'MACITCOMAH@gmail.com')
        self.user.profile.refresh_from_db()
        self.assertTrue(self.user.profile.creator)
        get_user_model().objects.create_user('duplicate', 'macitcomah@gmail.com')
        with self.assertRaises(CommandError):
            call_command('authorize_creator', 'macitcomah@gmail.com')

    def test_credit_requires_superuser_and_records_audit(self):
        from .models import BalanceAdjustment
        url = f'/admin/core/wallet/{self.wallet.pk}/credit/'
        self.client.force_login(self.user)
        self.assertEqual(self.client.post(url, {'amount_cents': 500, 'reason': 'support credit'}).status_code, 302)
        self.user.is_staff = True
        self.user.save()
        self.assertEqual(self.client.post(url, {'amount_cents': 500, 'reason': 'support credit'}).status_code, 403)
        self.client.force_login(self.admin)
        self.assertEqual(self.client.post(url, {'amount_cents': -1, 'reason': 'invalid'}).status_code, 200)
        self.assertEqual(self.client.post(url, {'amount_cents': 500, 'reason': 'support credit'}).status_code, 302)
        self.wallet.refresh_from_db()
        self.assertEqual(self.wallet.balance_cents, 500)
        entry = BalanceAdjustment.objects.get()
        self.assertEqual(entry.administrator, self.admin)
        self.assertEqual(entry.amount_cents, 500)

    def test_ban_invalidates_sessions_and_preview_is_available(self):
        from .models import SessionToken
        from .admin import AccountAdmin
        from django.contrib import admin
        from django.test import RequestFactory
        issue_token(self.user)
        request = RequestFactory().post('/admin/auth/user/')
        request.user = self.admin
        AccountAdmin(get_user_model(), admin.site).ban_accounts(request, get_user_model().objects.filter(pk=self.user.pk))
        self.user.refresh_from_db()
        self.assertFalse(self.user.is_active)
        self.assertFalse(SessionToken.objects.filter(user=self.user).exists())
        self.client.force_login(self.admin)
        self.assertContains(self.client.get(f'/admin/auth/user/{self.user.pk}/preview/'), 'macitcomah@gmail.com')

@override_settings(SECURE_SSL_REDIRECT=False)
class MobileAdministrationTests(TestCase):
    def setUp(self):
        self.admin = get_user_model().objects.create_superuser('mobileadmin', 'root@example.com', 'unique-admin-pass')
        self.user = get_user_model().objects.create_user('person', 'person@example.com', 'unique-member-pass')
        for user in (self.admin, self.user):
            Profile.objects.create(user=user)
            Wallet.objects.create(user=user)
        self.admin_token = issue_token(self.admin)
        self.user_token = issue_token(self.user)

    def call(self, path, data=None, token=None):
        kwargs = {'HTTP_AUTHORIZATION': 'Bearer ' + (token or self.admin_token)}
        if data is None:
            return self.client.get(path, **kwargs)
        return self.client.post(path, json.dumps(data), content_type='application/json', **kwargs)

    def test_only_full_administrator_can_access(self):
        for path in ('/api/admin/accounts', f'/api/admin/accounts/{self.user.pk}', '/api/admin/moderation'):
            self.assertEqual(self.call(path, token=self.user_token).status_code, 403)
            self.assertEqual(self.client.get(path).status_code, 401)
        self.user.is_staff = True
        self.user.save()
        self.assertEqual(self.call('/api/admin/accounts', token=self.user_token).status_code, 403)
        self.assertFalse(self.call('/api/me', token=self.user_token).json()['is_admin'])
        self.assertTrue(self.call('/api/me').json()['is_admin'])
        self.assertNotIn('password', self.call('/api/admin/accounts').content.decode())

    def test_ban_creator_preview_and_protection(self):
        url = f'/api/admin/accounts/{self.user.pk}'
        self.assertEqual(self.call(url, {'action': 'creator_on'}).status_code, 200)
        self.user.profile.refresh_from_db()
        self.assertTrue(self.user.profile.creator)
        self.assertEqual(self.call('/api/creators', token=self.user_token).json()['items'][0]['id'], self.user.pk)
        self.assertEqual(self.call(url).json()['email'], self.user.email)
        self.assertEqual(self.call(url, {'action': 'ban'}).status_code, 200)
        self.assertEqual(self.call('/api/me', token=self.user_token).status_code, 401)
        self.assertEqual(self.call(url, {'action': 'unban'}).status_code, 200)
        self.assertEqual(self.call(f'/api/admin/accounts/{self.admin.pk}', {'action': 'ban'}).status_code, 403)
        self.assertEqual(self.call(url, {'action': 'make_admin'}).status_code, 400)

    def test_credit_is_validated_audited_and_idempotent(self):
        from .models import BalanceAdjustment
        from django.contrib.admin.models import LogEntry
        url = f'/api/admin/accounts/{self.user.pk}'
        data = {'action': 'credit', 'amount_cents': 2345, 'reason': 'Support credit', 'request_key': str(uuid.uuid4())}
        self.assertEqual(self.call(url, data, token=self.user_token).status_code, 403)
        self.assertEqual(self.call(url, dict(data, amount_cents=-1)).status_code, 400)
        self.assertEqual(self.call(url, dict(data, request_key='invalid')).status_code, 400)
        self.assertEqual(self.call(url, data).status_code, 200)
        self.assertEqual(self.call(url, data).status_code, 200)
        self.assertEqual(self.call(url, dict(data, amount_cents=2500)).status_code, 409)
        wallet = Wallet.objects.get(user=self.user)
        self.assertEqual(wallet.balance_cents, 2345)
        self.assertEqual(BalanceAdjustment.objects.count(), 1)
        self.assertEqual(LogEntry.objects.filter(user=self.admin).count(), 1)

    def test_moderation_requires_admin_and_boolean_approval(self):
        from .models import Reel
        service = Service.objects.create(owner=self.user, title='Session', price_cents=100)
        reel = Reel.objects.create(owner=self.user, video_url='https://example.com/reel.mp4')
        for kind, obj in (('service', service), ('reel', reel)):
            data = {'kind': kind, 'id': obj.pk, 'approved': True}
            self.assertEqual(self.call('/api/admin/moderation', data, token=self.user_token).status_code, 403)
            self.assertEqual(self.call('/api/admin/moderation', dict(data, approved='true')).status_code, 400)
            self.assertEqual(self.call('/api/admin/moderation', data).status_code, 200)
            obj.refresh_from_db()
            self.assertTrue(obj.approved)
            self.assertEqual(self.call('/api/admin/moderation', dict(data, approved=False)).status_code, 200)
            obj.refresh_from_db()
            self.assertFalse(obj.approved)

    def test_console_created_admin_can_login_without_existing_profile(self):
        Profile.objects.filter(user=self.admin).delete()
        Wallet.objects.filter(user=self.admin).delete()
        response = self.call('/api/login', {'username': 'mobileadmin', 'password': 'unique-admin-pass'})
        self.assertEqual(response.status_code, 200)
        token = response.json()['token']
        self.assertTrue(self.call('/api/me', token=token).json()['is_admin'])
        self.assertEqual(self.call('/api/admin/accounts', token=token).status_code, 200)

@override_settings(SECURE_SSL_REDIRECT=False)
class CommerceTests(TestCase):
    def setUp(self):
        import tempfile
        self.directory = tempfile.TemporaryDirectory()
        self.override = override_settings(MEDIA_ROOT=self.directory.name)
        self.override.enable()
        self.addCleanup(self.override.disable)
        self.addCleanup(self.directory.cleanup)
        self.user = get_user_model().objects.create_user('payer', 'payer@example.com', 'unique-pass')
        self.peer = get_user_model().objects.create_user('peer', 'peer@example.com', 'unique-pass')
        self.admin = get_user_model().objects.create_superuser('boss', 'boss@example.com', 'unique-pass')
        for user in (self.user, self.peer, self.admin):
            Profile.objects.create(user=user)
            Wallet.objects.create(user=user)
        self.token = issue_token(self.user)
        self.peer_token = issue_token(self.peer)
        self.admin_token = issue_token(self.admin)

    def call(self, path, data=None, token=None):
        kwargs = {'HTTP_AUTHORIZATION': 'Bearer ' + (token or self.token)}
        if data is None: return self.client.get(path, **kwargs)
        return self.client.post(path, json.dumps(data), content_type='application/json', **kwargs)

    def upload(self, purpose='receipt'):
        import io, base64
        from PIL import Image
        stream = io.BytesIO()
        Image.new('RGB', (32,32), 'red').save(stream, 'PNG')
        response = self.call('/api/upload', {'purpose': purpose, 'data': base64.b64encode(stream.getvalue()).decode()})
        self.assertEqual(response.status_code, 201)
        return response.json()

    def test_receipts_are_private_and_transaction_cannot_double_credit(self):
        from .models import TopUp
        media = self.upload()
        self.assertEqual(self.client.get('/api/media/' + media['id']).status_code, 403)
        self.assertEqual(self.client.get(media['url']).status_code, 200)
        body = {'amount_cents': 2500, 'transaction_hash': '0x' + 'a'*64, 'receipt_id': media['id']}
        result = self.call('/api/topups', body)
        self.assertEqual(result.status_code, 201)
        self.assertEqual(self.call('/api/topups', body).status_code, 200)
        self.assertEqual(TopUp.objects.count(), 1)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 0)
        decision = {'kind': 'topup', 'id': result.json()['id'], 'decision': 'approve', 'review_note': 'Verified on chain'}
        self.assertEqual(self.call('/api/admin/requests', decision).status_code, 403)
        self.assertEqual(self.call('/api/admin/requests', decision, self.admin_token).status_code, 400)
        decision['payment_verified'] = True
        self.assertEqual(self.call('/api/admin/requests', decision, self.admin_token).status_code, 200)
        self.assertEqual(self.call('/api/admin/requests', decision, self.admin_token).status_code, 200)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 2600)
        self.assertEqual(self.call('/api/admin/requests', dict(decision, decision='reject'), self.admin_token).status_code, 409)

    def test_seller_needs_approval_and_private_chat_files_restrict_recipients(self):
        self.assertEqual(self.call('/api/seller-requests', {'description': 'Non-explicit adult services'}).status_code, 200)
        self.assertEqual(self.call('/api/seller-requests', {'description': 'Non-explicit adult services'}).status_code, 409)
        from .models import SellerApplication
        row = SellerApplication.objects.get()
        self.assertEqual(self.call('/api/admin/requests', {'kind': 'seller', 'id': row.pk, 'decision': 'approve', 'review_note': 'Content reviewed'}, self.admin_token).status_code, 200)
        self.user.profile.refresh_from_db()
        self.assertTrue(self.user.profile.creator)
        media = self.upload('chat_image')
        result = self.call(f'/api/messages/{self.peer.pk}', {'attachment_id': media['id']})
        self.assertEqual(result.status_code, 201)
        received = self.call(f'/api/messages/{self.user.pk}', token=self.peer_token).json()['items'][0]
        self.assertEqual(self.client.get(received['attachment_url']).status_code, 200)
        self.assertEqual(self.call(f'/api/messages/{self.peer.pk}', {'attachment_id': media['id']}).status_code, 200)
        from .models import Message
        self.assertEqual(Message.objects.count(), 1)
        self.user.is_active = False
        self.user.save()
        self.assertEqual(self.client.get(received['attachment_url']).status_code, 403)

    def test_upload_validation_and_reel_publishing_gate(self):
        import base64
        self.assertEqual(self.call('/api/upload', {'purpose':'receipt','data':base64.b64encode(b'<html>not an image</html>').decode()}).status_code, 400)
        self.assertEqual(self.call('/api/upload', {'purpose':'offer','data':'a'*40}).status_code, 403)
        self.assertEqual(self.call('/api/reels', {'media_id':str(uuid.uuid4())}).status_code, 403)
        self.assertEqual(self.call('/api/admin/requests').status_code, 403)
        info = self.call('/api/payment-info').json()
        self.assertEqual(info['address'], '0xc35797768180752b67743d791f79f812bb0972d1')
        self.assertEqual(self.client.get('/api/payment-qr').status_code, 200)

    def test_paid_wheel_disabled_until_configured_and_retry_does_not_charge_twice(self):
        from .models import PaidWheelConfiguration, PaidSpin
        self.assertFalse(self.call('/api/promotion/wheel').json()['enabled'])
        self.assertEqual(self.call('/api/promotion/wheel', {'request_key': str(uuid.uuid4())}).status_code, 409)
        config = PaidWheelConfiguration.objects.get(pk=1)
        config.enabled = True; config.cost_cents = 100; config.prizes = [{'amount_cents': 50, 'weight': 1}]; config.bankroll_cents = 10000; config.save()
        info = self.call('/api/promotion/wheel').json()
        data = {'request_key': str(uuid.uuid4()), 'config_version': info['config_version']}
        self.assertEqual(self.call('/api/promotion/wheel', data).status_code, 409)
        wallet = Wallet.objects.get(user=self.user); wallet.balance_cents = 500; wallet.save()
        self.assertEqual(self.call('/api/promotion/wheel', data).status_code, 201)
        self.assertEqual(self.call('/api/promotion/wheel', data).status_code, 200)
        self.assertEqual(PaidSpin.objects.count(), 1)
        wallet.refresh_from_db(); config.refresh_from_db()
        self.assertEqual(wallet.balance_cents, 450)
        self.assertEqual(config.bankroll_cents, 10050)
        self.assertEqual(self.call('/api/admin/wheel').status_code, 403)

@override_settings(SECURE_SSL_REDIRECT=False)
class CommerceConcurrencyTests(TransactionTestCase):
    def setUp(self):
        from django.db import connection
        if connection.vendor != 'postgresql': self.skipTest('Row locking requires PostgreSQL')
        self.user = get_user_model().objects.create_user('concurrentpayer', password='unique-pass')
        self.admin = get_user_model().objects.create_superuser('concurrentboss', 'admin@example.com', 'unique-pass')
        for user in (self.user, self.admin):
            Profile.objects.create(user=user); Wallet.objects.create(user=user)
        self.token = issue_token(self.user); self.admin_token = issue_token(self.admin)

    def parallel_post(self, path, data, token):
        from concurrent.futures import ThreadPoolExecutor
        from django.db import close_old_connections
        from django.test import Client
        def send(_):
            close_old_connections()
            try: return Client().post(path, json.dumps(data), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + token).status_code
            finally: close_old_connections()
        with ThreadPoolExecutor(max_workers=2) as pool: return list(pool.map(send, range(2)))

    def test_concurrent_topup_approval_credits_once(self):
        from .models import TopUp, UploadedMedia
        receipt = UploadedMedia.objects.create(owner=self.user, purpose='receipt', file='test-only.jpg', mime='image/jpeg', size=100)
        row = TopUp.objects.create(user=self.user, amount_cents=1000, transaction_hash='0x'+'b'*64, receipt=receipt)
        codes = self.parallel_post('/api/admin/requests', {'kind':'topup','id':str(row.pk),'decision':'approve','review_note':'Verified on chain','payment_verified':True}, self.admin_token)
        self.assertEqual(codes, [200, 200])
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 1000)

    def test_concurrent_spin_retry_charges_once(self):
        from .models import PaidWheelConfiguration, PaidSpin
        from .commerce import wheel_settings
        config = PaidWheelConfiguration.objects.create(pk=1, enabled=True, cost_cents=100, bankroll_cents=10000, prizes=[{'amount_cents':50,'weight':1}])
        wallet = Wallet.objects.get(user=self.user); wallet.balance_cents=500; wallet.save()
        codes = self.parallel_post('/api/promotion/wheel', {'request_key':str(uuid.uuid4()),'config_version':wheel_settings(config)[2]}, self.token)
        self.assertEqual(sorted(codes), [200,201])
        self.assertEqual(PaidSpin.objects.count(),1)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,450)
