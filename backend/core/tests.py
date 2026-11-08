import json
import uuid
from datetime import date
from django.test import TestCase, TransactionTestCase, override_settings
from django.contrib.auth import get_user_model
from .models import Profile, Wallet, Service, Withdrawal, Message
from .admin_test_support import issue_token,reviewed_payload

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
        p = {'gender':'female', 'username': 'newuser', 'email': 'new@example.com', 'password': 'some-unique-2026-password!', 'date_of_birth': date.today().isoformat(), 'accept_non_explicit_policy': True}
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
        p = {'gender':'male', 'username': 'adultnew', 'email': 'adult@example.com', 'password': 'unique-new-account-pass-2026!', 'accept_non_explicit_policy': True}
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
        p = {'gender':'female', 'username': '  لي  ', 'password': 'Sunrise8', 'adult_confirmed': True, 'accept_non_explicit_policy': True}
        self.assertEqual(self.post('/api/register', p).status_code, 400)
        p['email'] = ''
        self.assertEqual(self.post('/api/register', p).status_code, 400)
        p['email'] = 'member@example.com'
        response = self.post('/api/register', p)
        self.assertEqual(response.status_code, 201)
        user = get_user_model().objects.get(username='لي')
        self.assertEqual(user.email, 'member@example.com')
        self.assertTrue(user.check_password('Sunrise8'))
        self.assertEqual(self.post('/api/login', {'email': 'MEMBER@example.com', 'password': 'Sunrise8'}).status_code, 200)

    def test_simple_registration_errors_identify_the_field(self):
        p = {'gender':'male', 'username': 'newmember', 'email': 'newmember@example.com', 'password': 'short', 'adult_confirmed': True, 'accept_non_explicit_policy': True}
        self.assertEqual(self.post('/api/register', p).json()['error'], 'invalid_password')
        p['password'] = '12345678'
        self.assertEqual(self.post('/api/register', p).json()['error'], 'password_too_common')
        p['password'] = 'Sunrise8'
        p['email'] = 'not-an-email'
        self.assertEqual(self.post('/api/register', p).json()['error'], 'invalid_email')
        p['email'] = 'nour@example.com'
        p['username'] = 'Nour Hassan'
        self.assertEqual(self.post('/api/register', p).status_code, 201)
        self.assertEqual(self.post('/api/login', {'email': 'nour@example.com', 'password': 'Sunrise8'}).status_code, 200)

    def test_health_advertises_simplified_registration(self):
        response = self.client.get('/health/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['registration_version'], 6)

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

    def test_creator_detail_uses_real_counts_and_respects_blocks(self):
        from .models import Follow, UserBlock
        Follow.objects.create(user=self.user, creator=self.creator)
        response = self.client.get(
            f'/api/creators/{self.creator.pk}',
            HTTP_AUTHORIZATION='Bearer ' + self.token,
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['id'], self.creator.pk)
        self.assertEqual(response.json()['followers_count'], 1)
        self.assertEqual(response.json()['following_count'], 0)
        self.assertEqual(response.json()['reel_count'], 0)
        UserBlock.objects.create(user=self.user, blocked=self.creator)
        hidden = self.client.get(
            f'/api/creators/{self.creator.pk}',
            HTTP_AUTHORIZATION='Bearer ' + self.token,
        )
        self.assertEqual(hidden.status_code, 404)

    def test_reels_and_publication_are_available_to_regular_accounts(self):
        Profile.objects.filter(user=self.user).update(identity_verified=False)
        response = self.client.get('/api/reels', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.post('/api/services', {'title': 'test', 'description': 'test description', 'price_cents': 200}).status_code, 201)

    def test_real_xp_and_secure_luxury_frames_1_to_100(self):
        from datetime import timedelta
        from django.utils import timezone
        from .models import Reel, BenefitGrant
        auth = {'HTTP_AUTHORIZATION': 'Bearer ' + self.token}
        def equip(frame):
            return self.client.patch('/api/me', json.dumps({'selected_frame': frame}),
                content_type='application/json', **auth)
        me = self.client.get('/api/me', **auth).json()
        self.assertEqual(me['level'], 1)
        self.assertEqual(me['xp_points'], 0)
        self.assertIn(0, me['owned_frames'])
        self.assertIn(10, me['owned_frames'])  # Legacy frames remain available.
        self.assertNotIn(11, me['owned_frames'])
        self.assertEqual(equip(11).status_code, 403)
        self.assertEqual(equip(101).status_code, 400)
        self.assertEqual(equip(10).status_code, 200)

        Reel.objects.bulk_create([
            Reel(owner=self.user, approved=True,
                 video_url=f'https://example.com/real-{i}.mp4')
            for i in range(67)
        ])
        raised = self.client.get('/api/me', **auth).json()
        self.assertEqual(raised['xp_points'], 67 * 30)
        self.assertEqual(raised['level'], 11)
        self.assertIn(11, raised['owned_frames'])
        self.assertNotIn(12, raised['owned_frames'])
        self.assertEqual(equip(11).status_code, 200)
        self.assertEqual(equip(90).status_code, 403)

        BenefitGrant.objects.create(
            user=self.user, actor=self.creator, kind='frame', value=90,
            reason='Admin-approved special frame',
            expires_at=timezone.now()+timedelta(days=1),
        )
        self.assertEqual(equip(90).status_code, 200)
        self.assertIn(90, self.client.get('/api/me', **auth).json()['owned_frames'])

    def test_regular_user_publishes_service_and_reel_immediately(self):
        from .models import Reel, UploadedMedia
        offer = self.post('/api/services', {
            'title': 'Real service', 'description': 'A legitimate service listing',
            'price_cents': 200, 'category': 'خدمة'
        })
        self.assertEqual(offer.status_code, 201, offer.content)
        self.assertEqual(offer.json()['status'], 'published')
        service = Service.objects.get(pk=offer.json()['id'])
        self.assertTrue(service.approved)
        visible_offers = self.client.get('/api/services', HTTP_AUTHORIZATION='Bearer ' + self.creator_token).json()['items']
        self.assertIn(service.pk, [item['id'] for item in visible_offers])

        media = UploadedMedia.objects.create(owner=self.user, purpose='reel',
            file='uploads/tests/reel.mp4', mime='video/mp4', size=32)
        reel_response = self.post('/api/reels', {'media_id': str(media.pk)})
        self.assertEqual(reel_response.status_code, 201, reel_response.content)
        self.assertEqual(reel_response.json()['status'], 'published')
        reel = Reel.objects.get(pk=reel_response.json()['id'])
        self.assertTrue(reel.approved)
        visible_reels = self.client.get('/api/reels', HTTP_AUTHORIZATION='Bearer ' + self.creator_token).json()['items']
        self.assertIn(reel.pk, [item['id'] for item in visible_reels])
        reel_info = next(item for item in visible_reels if item['id'] == reel.pk)
        self.assertEqual(reel_info['owner_name'], self.user.profile.display_name or self.user.username)
        self.assertIn('avatar_url', reel_info)
        self.assertEqual(reel_info['owner_username'], self.user.username)
        self.assertEqual(reel_info['owner_public_id'], self.user.profile.public_id)
        self.assertFalse(reel_info['following'])
        self.assertFalse(reel_info['identity_verified'])
        self.assertIn('created_at', reel_info)
        self.assertNotIn('likes', reel_info)
        self.assertNotIn('comments', reel_info)

        # Follow and verification labels must reflect the database, not fake metrics.
        from .models import Follow
        Follow.objects.create(user=self.creator, creator=self.user)
        Profile.objects.filter(user=self.user).update(identity_verified=True)
        updated = self.client.get('/api/reels', HTTP_AUTHORIZATION='Bearer ' + self.creator_token).json()['items']
        publisher = next(item for item in updated if item['id'] == reel.pk)
        self.assertTrue(publisher['following'])
        self.assertTrue(publisher['identity_verified'])

        # Post-publication moderation must still remove reported content.
        Service.objects.filter(pk=service.pk).update(approved=False)
        Reel.objects.filter(pk=reel.pk).update(approved=False)
        offers = self.client.get('/api/services', HTTP_AUTHORIZATION='Bearer ' + self.creator_token).json()['items']
        reels = self.client.get('/api/reels', HTTP_AUTHORIZATION='Bearer ' + self.creator_token).json()['items']
        self.assertNotIn(service.pk, [item['id'] for item in offers])
        self.assertNotIn(reel.pk, [item['id'] for item in reels])

    def test_reel_media_supports_http_ranges_without_bypassing_access(self):
        import tempfile
        from django.core.files.base import ContentFile
        from .models import UploadedMedia, Reel
        payload = bytes(range(128))
        with tempfile.TemporaryDirectory() as folder, override_settings(MEDIA_ROOT=folder):
            media = UploadedMedia(owner=self.user, purpose='reel',
                                  mime='video/mp4', size=len(payload))
            media.file.save('range-test.mp4', ContentFile(payload), save=False)
            media.save()
            reel = Reel.objects.create(
                owner=self.user, media=media, approved=True,
                video_url='https://example.test/range-test.mp4'
            )
            url = f'/api/media/{media.pk}'
            first = self.client.get(url, HTTP_RANGE='bytes=10-24')
            self.assertEqual(first.status_code, 206)
            self.assertEqual(first['Content-Range'], 'bytes 10-24/128')
            self.assertEqual(first['Content-Length'], '15')
            self.assertEqual(first['Accept-Ranges'], 'bytes')
            self.assertEqual(first['Content-Type'], 'video/mp4')
            self.assertEqual(b''.join(first.streaming_content), payload[10:25])

            suffix = self.client.get(url, HTTP_RANGE='bytes=-8')
            self.assertEqual(suffix.status_code, 206)
            self.assertEqual(suffix['Content-Range'], 'bytes 120-127/128')
            self.assertEqual(b''.join(suffix.streaming_content), payload[-8:])

            invalid = self.client.get(url, HTTP_RANGE='bytes=200-')
            self.assertEqual(invalid.status_code, 416)
            self.assertEqual(invalid['Content-Range'], 'bytes */128')
            malformed = self.client.get(url, HTTP_RANGE='bytes=0-1,4-5')
            self.assertEqual(malformed.status_code, 416)
            head = self.client.head(url)
            self.assertEqual(head.status_code, 200)
            self.assertEqual(head['Content-Length'], '128')
            self.assertEqual(head['Accept-Ranges'], 'bytes')

            Reel.objects.filter(pk=reel.pk).update(approved=False)
            self.assertEqual(
                self.client.get(url, HTTP_RANGE='bytes=0-15').status_code, 403
            )

    def test_disabled_publishing_permission_still_restricts_services_and_reels(self):
        Profile.objects.filter(user=self.user).update(publishing_allowed=False)
        response = self.post('/api/services', {
            'title': 'Blocked service', 'description': 'Service must not appear',
            'price_cents': 200
        })
        self.assertEqual(response.status_code, 403)
        self.assertEqual(self.post('/api/reels', {'media_id': str(uuid.uuid4())}).status_code, 403)

    def test_login_rate_limit(self):
        for _ in range(20):
            self.assertEqual(self.post('/api/login', {'email': 'buyer@example.com', 'password': 'wrong'}).status_code, 401)
        self.assertEqual(self.post('/api/login', {'email': 'buyer@example.com', 'password': 'wrong'}).status_code, 429)


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
        locked = self.client.patch('/api/me', json.dumps({'selected_frame': 11}), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(locked.status_code, 403)
        invalid = self.client.patch('/api/me', json.dumps({'selected_frame': 101}), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertEqual(invalid.status_code, 400)

    def test_chat_fee_requires_publishing_permission(self):
        Profile.objects.filter(user=self.user).update(publishing_allowed=False)
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
        self.assertEqual(result.json()['status'], 'published')
        self.assertTrue(Service.objects.get(pk=result.json()['id']).approved)
        Wallet.objects.filter(user=self.creator).update(balance_cents=1000)
        result = self.post('/api/withdrawals', {'request_key': str(uuid.uuid4()), 'amount_cents': 500, 'destination': 'test destination'}, self.creator_token)
        self.assertEqual(result.status_code, 201)
        self.assertEqual(result.json()['status'], 'pending')

    def test_creator_directory_and_following_do_not_require_identity(self):
        result = self.client.get('/api/creators', HTTP_AUTHORIZATION='Bearer ' + self.token)
        self.assertIn(self.creator.pk, [x['id'] for x in result.json()['items']])
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

    def test_external_admin_cannot_credit_accounts(self):
        for user in (self.user,self.admin):
            self.client.force_login(user)
            self.assertEqual(self.client.post(f'/admin/core/wallet/{self.wallet.pk}/credit/',{'amount_cents':500,'reason':'legacy path'}).status_code,404)
        self.wallet.refresh_from_db();self.assertEqual(self.wallet.balance_cents,0)

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
        self.assertEqual(self.client.get(f'/admin/auth/user/{self.user.pk}/preview/').status_code,404)

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
        if data is not None and path.startswith('/api/admin/') and not data.get('reason') and not data.get('review_note'):data=dict(data,reason='Explicit legacy administrative fixture')
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
        self.assertIn(self.user.pk, [x['id'] for x in self.call('/api/creators', token=self.user_token).json()['items']])
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
        data=reviewed_payload(self.admin,url,data,self.user.pk)
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
        response = self.call('/api/login', {'email': 'root@example.com', 'password': 'unique-admin-pass'})
        self.assertEqual(response.status_code, 200)
        token = response.json()['token']
        self.assertTrue(self.call('/api/me', token=token).json()['is_admin'])
        self.assertEqual(self.call('/api/admin/accounts', token=token).json()['error'],'admin_mfa_required')

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
        if data is not None and path.startswith('/api/admin/') and not data.get('reason') and not data.get('review_note'):data=dict(data,reason='Explicit legacy administrative fixture')
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
        decision=reviewed_payload(self.admin,'/api/admin/requests',decision,self.user.pk)
        self.assertEqual(self.call('/api/admin/requests', decision, self.admin_token).status_code, 200)
        self.assertEqual(self.call('/api/admin/requests', decision, self.admin_token).status_code, 200)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 2500)
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
        Profile.objects.filter(user=self.user).update(publishing_allowed=False)
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
        config.enabled = True; config.first_reward_cents = 0; config.price_step_cents = 0; config.cost_cents = 100; config.prizes = [{'amount_cents': 50, 'weight': 1}]; config.bankroll_cents = 10000; config.save()
        info = self.call('/api/promotion/wheel').json()
        data = {'rules_version':2,'request_key': str(uuid.uuid4()), 'config_version': info['config_version']}
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
        from django.db import close_old_connections, connections
        from django.test import Client
        def send(_):
            close_old_connections()
            try: return Client().post(path, json.dumps(data), content_type='application/json', HTTP_AUTHORIZATION='Bearer ' + token).status_code
            finally: connections.close_all()
        with ThreadPoolExecutor(max_workers=2) as pool: return list(pool.map(send, range(2)))

    def test_concurrent_topup_approval_credits_once(self):
        from .models import TopUp, UploadedMedia
        receipt = UploadedMedia.objects.create(owner=self.user, purpose='receipt', file='test-only.jpg', mime='image/jpeg', size=100)
        row = TopUp.objects.create(user=self.user, amount_cents=1000, transaction_hash='0x'+'b'*64, receipt=receipt)
        data=reviewed_payload(self.admin,'/api/admin/requests',{'kind':'topup','id':str(row.pk),'decision':'approve','review_note':'Verified on chain','payment_verified':True},self.user.pk)
        codes = self.parallel_post('/api/admin/requests', data, self.admin_token)
        self.assertEqual(codes, [200, 200])
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents, 1000)

    def test_concurrent_spin_retry_charges_once(self):
        from .models import PaidWheelConfiguration, PaidSpin
        from .commerce import wheel_offer
        config = PaidWheelConfiguration.objects.update_or_create(pk=1, defaults=dict(enabled=True, cost_cents=100, first_reward_cents=0, price_step_cents=0, bankroll_cents=10000, prizes=[{'amount_cents':50,'weight':1}]))[0]
        wallet = Wallet.objects.get(user=self.user); wallet.balance_cents=500; wallet.save()
        codes = self.parallel_post('/api/promotion/wheel', {'rules_version':2,'request_key':str(uuid.uuid4()),'config_version':wheel_offer(config,self.user)['config_version']}, self.token)
        self.assertEqual(sorted(codes), [200,201])
        self.assertEqual(PaidSpin.objects.count(),1)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,450)

@override_settings(SECURE_SSL_REDIRECT=False)
class InternalCallingTests(TestCase):
    def setUp(self):
        from unittest.mock import patch
        self.environment = patch.dict('os.environ', {'LAYALI_TURN_SECRET':'test-relay-secret-not-production', 'LAYALI_TURN_HOST':'turn.example.com'})
        self.environment.start(); self.addCleanup(self.environment.stop)
        self.caller = get_user_model().objects.create_user('callingone', password='unique-pass')
        self.callee = get_user_model().objects.create_user('callingtwo', password='unique-pass')
        self.stranger = get_user_model().objects.create_user('outsider', password='unique-pass')
        for user in (self.caller,self.callee,self.stranger): Profile.objects.create(user=user); Wallet.objects.create(user=user)
        self.caller_token=issue_token(self.caller); self.callee_token=issue_token(self.callee); self.stranger_token=issue_token(self.stranger)

    def call(self,path,data=None,token=None):
        kwargs={'HTTP_AUTHORIZATION':'Bearer '+(token or self.caller_token)}
        if data is None: return self.client.get(path,**kwargs)
        return self.client.post(path,json.dumps(data),content_type='application/json',**kwargs)

    def create(self, mode='audio'):
        response=self.call('/api/calls',{'peer_id':self.callee.pk,'mode':mode})
        self.assertEqual(response.status_code,201)
        return response.json()['id']

    def room_token(self, call_id, token):
        import re
        bootstrap=self.call(f'/api/calls/{call_id}/ticket',{},token).json()
        room=self.client.post(f'/api/calls/{call_id}/room',{'ticket':bootstrap['ticket']})
        self.assertEqual(room.status_code,200)
        self.assertEqual(self.client.post(f'/api/calls/{call_id}/room',{'ticket':bootstrap['ticket']}).status_code,403)
        self.assertNotIn('test-relay-secret-not-production',room.content.decode())
        self.assertIn("frame-ancestors 'none'",room['Content-Security-Policy'])
        config=json.loads(re.search(r'<script id="config" type="application/json">(.*?)</script>',room.content.decode(),re.S).group(1))
        return config['token']

    def test_call_room_uses_verified_profile_and_real_media_controls(self):
        import re
        Profile.objects.filter(user=self.callee).update(identity_verified=False)
        call_id = self.create('video')
        bootstrap = self.call(f'/api/calls/{call_id}/ticket', {}, self.caller_token).json()
        page = self.client.post(f'/api/calls/{call_id}/room', {'ticket': bootstrap['ticket']})
        self.assertEqual(page.status_code, 200)
        config = json.loads(re.search(
            r'<script id="config" type="application/json">(.*?)</script>',
            page.content.decode(), re.S,
        ).group(1))
        self.assertFalse(config['peerVerified'])
        self.assertContains(page, 'id="pipContainer"')
        self.assertContains(page, 'id="pipSwitch"')
        self.assertContains(page, 'id="signalIndicator"')
        self.assertContains(page, 'pc.getStats()')

    def test_invite_accept_reject_permissions_and_busy_state(self):
        call_id=self.create('video')
        url=f'/api/calls/{call_id}'
        self.assertEqual(self.call('/api/calls',{'peer_id':self.callee.pk,'mode':'video'}).status_code,200)
        self.assertEqual(self.call(url,{'action':'accept'}).status_code,403)
        self.assertEqual(self.call(url,token=self.stranger_token).status_code,404)
        self.assertEqual(self.call(url+'/ticket',{},self.callee_token).status_code,409)
        self.assertEqual(self.call('/api/calls',token=self.callee_token).json()['incoming']['id'],call_id)
        self.assertEqual(self.call('/api/calls',{'peer_id':self.callee.pk,'mode':'audio'},self.stranger_token).status_code,409)
        self.assertEqual(self.call(url,{'action':'accept'},self.callee_token).json()['status'],'accepted')
        self.assertEqual(self.call(url,{'action':'end'}).json()['status'],'ended')
        self.assertEqual(self.call(url,{'action':'accept'},self.callee_token).status_code,409)
        self.assertEqual(self.call('/api/calls',token=self.callee_token).json()['incoming'],None)
        from .models import InternalCall
        self.assertEqual(InternalCall.objects.count(),1)
        self.assertEqual(Wallet.objects.get(user=self.caller).balance_cents,0)

    def test_one_use_tickets_and_scoped_signaling(self):
        call_id=self.create()
        self.call(f'/api/calls/{call_id}',{'action':'accept'},self.callee_token)
        caller=self.room_token(call_id,self.caller_token); callee=self.room_token(call_id,self.callee_token)
        path=f'/api/calls/{call_id}/signal'
        def send(token,kind,payload): return self.client.post(path,json.dumps({'kind':kind,'data':payload}),content_type='application/json',HTTP_X_CALL_TOKEN=token)
        self.assertEqual(send(callee,'offer',{'type':'offer','sdp':'fake-test-sdp'}).status_code,403)
        self.assertEqual(send(caller,'offer',{'type':'offer','sdp':'fake-test-sdp'}).status_code,201)
        self.assertEqual(send(caller,'offer',{'type':'offer','sdp':'fake-test-sdp'}).status_code,200)
        events=self.client.get(path,HTTP_X_CALL_TOKEN=callee).json()['events']
        self.assertEqual(events[0]['kind'],'offer')
        self.assertEqual(self.client.get(path,HTTP_X_CALL_TOKEN='forged').status_code,403)
        self.assertEqual(self.client.get(f'/api/calls/{uuid.uuid4()}/signal',HTTP_X_CALL_TOKEN=caller).status_code,403)
        self.assertEqual(self.call(f'/api/calls/{call_id}/ticket',{},self.stranger_token).status_code,404)
        self.call('/api/logout',{},self.callee_token)
        self.assertEqual(self.client.get(path,HTTP_X_CALL_TOKEN=callee).status_code,401)

    def test_ban_revokes_call_room_and_signaling(self):
        call_id=self.create(); caller=self.room_token(call_id,self.caller_token)
        self.callee.is_active=False; self.callee.save()
        self.assertEqual(self.client.get(f'/api/calls/{call_id}/signal',HTTP_X_CALL_TOKEN=caller).status_code,403)
        self.assertEqual(self.call(f'/api/calls/{call_id}').status_code,403)

    def test_no_reply_and_stale_media_sessions_expire(self):
        from .models import InternalCall
        from django.utils import timezone
        from datetime import timedelta
        call_id=self.create()
        InternalCall.objects.filter(pk=call_id).update(created_at=timezone.now()-timedelta(seconds=61))
        self.assertEqual(self.call(f'/api/calls/{call_id}').json()['status'],'missed')
        second=self.create(); self.call(f'/api/calls/{second}',{'action':'accept'},self.callee_token)
        InternalCall.objects.filter(pk=second).update(callee_seen_at=timezone.now()-timedelta(seconds=46))
        self.assertEqual(self.call(f'/api/calls/{second}').json()['status'],'ended')

    def test_setup_and_paid_chat_gate(self):
        from unittest.mock import patch
        with patch.dict('os.environ',{'LAYALI_TURN_SECRET':''}):
            self.assertEqual(self.call('/api/calls',{'peer_id':self.callee.pk}).status_code,503)
        profile=self.callee.profile; profile.chat_price_cents=100; profile.save()
        self.assertEqual(self.call('/api/calls',{'peer_id':self.callee.pk}).status_code,409)
        self.assertEqual(self.call(f'/api/messages/{self.callee.pk}',{'start_call':True}).status_code,409)

@override_settings(SECURE_SSL_REDIRECT=False)
class ReliableChatTests(TestCase):
    def setUp(self):
        self.sender=get_user_model().objects.create_user('actual_sender',password='unique-pass')
        self.receiver=get_user_model().objects.create_user('actual_receiver',password='unique-pass')
        self.stranger=get_user_model().objects.create_user('other_receiver',password='unique-pass')
        for user in (self.sender,self.receiver,self.stranger):Profile.objects.create(user=user);Wallet.objects.create(user=user)
        self.tokens={u.pk:issue_token(u) for u in (self.sender,self.receiver,self.stranger)}
    def post(self,path,body,user):return self.client.post(path,json.dumps(body),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+self.tokens[user.pk])
    def test_retry_and_recipient_acknowledgement_are_real(self):
        key=str(uuid.uuid4());path=f'/api/messages/{self.receiver.pk}';payload={'body':'A real queued message','client_key':key}
        first=self.post(path,payload,self.sender);self.assertEqual(first.status_code,201)
        self.assertEqual(self.post(path,payload,self.sender).status_code,200)
        self.assertEqual(Message.objects.count(),1)
        self.assertEqual(self.post(path,dict(payload,body='changed'),self.sender).status_code,409)
        self.assertEqual(self.post(path,dict(payload,body=5),self.sender).status_code,400)
        row=Message.objects.get();self.assertIsNone(row.delivered_at)
        received=self.client.get(f'/api/messages/{self.sender.pk}',HTTP_AUTHORIZATION='Bearer '+self.tokens[self.receiver.pk]).json()
        self.assertEqual(received['peer']['username'],'actual_sender')
        row.refresh_from_db();self.assertIsNone(row.delivered_at)
        ack=f'/api/messages/{self.sender.pk}/delivered'
        self.assertEqual(self.post(ack,{'ids':[row.pk]},self.stranger).status_code,200)
        row.refresh_from_db();self.assertIsNone(row.delivered_at)
        self.assertEqual(self.post(ack,{'ids':[row.pk]},self.receiver).status_code,200)
        row.refresh_from_db();self.assertIsNotNone(row.delivered_at)
        stamp=row.delivered_at;self.post(ack,{'ids':[row.pk]},self.receiver);row.refresh_from_db();self.assertEqual(row.delivered_at,stamp)

@override_settings(SECURE_SSL_REDIRECT=False)
class NewFundingAndWheelTests(CommerceTests):
    def test_quote_has_real_thirty_minute_expiration_and_snapshot_destination(self):
        from .models import PaymentSettings,TopUpQuote
        from django.utils import timezone
        import qrcode
        from unittest.mock import patch
        settings=PaymentSettings.objects.get(pk=1)
        self.assertEqual(settings.shamcash_account,'42c9f2bd84250c5ab6c444616b8ef0c2')
        first=self.call('/api/topup-quotes',{'method':'shamcash','amount_cents':1000});self.assertEqual(first.status_code,201)
        quote=first.json();again=self.call('/api/topup-quotes',{'method':'shamcash','amount_cents':1000}).json();self.assertEqual(quote['id'],again['id']);self.assertEqual(quote['expires_at'],again['expires_at'])
        row=TopUpQuote.objects.get(pk=quote['id']);self.assertAlmostEqual((row.expires_at-row.created_at).total_seconds(),1800,delta=1)
        settings.shamcash_account='new-operator-destination';settings.save()
        with patch('qrcode.make',wraps=qrcode.make) as make:
            self.assertEqual(self.client.get(quote['qr_url']).status_code,200)
            make.assert_called_once_with('42c9f2bd84250c5ab6c444616b8ef0c2')
        row.expires_at=timezone.now()-__import__('datetime').timedelta(seconds=1);row.save()
        receipt=self.upload()
        paid=self.call('/api/topups',{'quote_id':str(row.pk),'receipt_id':receipt['id'],'transaction_hash':'SHAM-123456'})
        self.assertEqual(paid.status_code,201);self.assertTrue(paid.json()['late_submission'])
        from .models import TopUp
        self.assertEqual(TopUp.objects.get(pk=paid.json()['id']).method,'shamcash')
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,0)

    def test_first_win_increasing_cost_and_explicit_grand_prize_gate(self):
        from .models import PaidWheelConfiguration,PaidSpin
        config=PaidWheelConfiguration.objects.get(pk=1);config.enabled=True;config.bankroll_cents=100000;config.save()
        wallet=Wallet.objects.get(user=self.user);wallet.balance_cents=10000;wallet.save()
        offer=self.call('/api/promotion/wheel').json()
        self.assertTrue(offer['first_spin_guaranteed']);self.assertEqual(offer['cost_cents'],350)
        paid=self.call('/api/promotion/wheel',{'rules_version':2,'request_key':str(uuid.uuid4()),'config_version':offer['config_version']})
        self.assertEqual(paid.status_code,201);self.assertEqual(paid.json()['prize_cents'],500)
        wallet.refresh_from_db();self.assertEqual(wallet.balance_cents,10150)
        second=self.call('/api/promotion/wheel').json();self.assertEqual(second['cost_cents'],400);self.assertFalse(second['first_spin_guaranteed']);self.assertFalse(second['grand_unlocked'])
        self.assertNotIn(10000,[p['amount_cents'] for p in second['prizes']])
        PaidSpin.objects.create(user=self.user,request_key=uuid.uuid4(),cost_cents=19650,prize_cents=0,prizes_snapshot=[])
        unlocked=self.call('/api/promotion/wheel').json();self.assertTrue(unlocked['grand_unlocked']);self.assertIn(10000,[p['amount_cents'] for p in unlocked['prizes']])
        self.assertEqual(self.call('/api/promotion/wheel',{'request_key':str(uuid.uuid4()),'config_version':unlocked['config_version']}).status_code,409)

@override_settings(SECURE_SSL_REDIRECT=False)
class NOWPaymentsTests(TestCase):
    def setUp(self):
        from unittest.mock import patch
        self.environment=patch.dict('os.environ',{'NOWPAYMENTS_API_KEY':'test-api-not-real','NOWPAYMENTS_IPN_SECRET':'test-ipn-not-real'})
        self.environment.start();self.addCleanup(self.environment.stop)
        self.user=get_user_model().objects.create_user('gatewaybuyer',password='unique-pass')
        Profile.objects.create(user=self.user);Wallet.objects.create(user=self.user);self.token=issue_token(self.user)
    def post(self,path,data):return self.client.post(path,json.dumps(data),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+self.token)
    def invoice(self):
        from unittest.mock import patch
        quote=self.post('/api/topup-quotes',{'method':'nowpayments','amount_cents':2500}).json()
        with patch('core.nowpayments.provider_request',return_value={'id':12345,'invoice_url':'https://nowpayments.io/payment/?iid=12345'}) as provider:
            response=self.post('/api/nowpayments/invoices',{'quote_id':quote['id']});self.assertEqual(response.status_code,201)
            self.assertEqual(provider.call_args.args[1]['price_amount'],'25')
        return quote
    def notify(self,data,valid=True):
        import hashlib,hmac
        body=json.dumps(data,sort_keys=True,separators=(',',':'),ensure_ascii=False)
        signature=hmac.new(b'test-ipn-not-real',body.encode(),hashlib.sha512).hexdigest() if valid else '0'*128
        return self.client.post('/api/nowpayments/ipn',body,content_type='application/json',HTTP_X_NOWPAYMENTS_SIG=signature)
    def payload(self,quote,status='finished'):
        return {'order_id':quote['id'],'invoice_id':12345,'payment_id':98765,'price_amount':25,'price_currency':'usd','payment_status':status,'pay_amount':25,'actually_paid':25,'pay_currency':'usdtbsc'}
    def test_signed_finished_callback_credits_once_and_return_url_never_credits(self):
        quote=self.invoice();data=self.payload(quote)
        self.client.get('/api/nowpayments/return?status=finished')
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,0)
        self.assertEqual(self.notify(data,False).status_code,403)
        self.assertEqual(self.notify(self.payload(quote,'confirming')).status_code,200)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,0)
        self.assertEqual(self.notify(data).status_code,200)
        self.assertEqual(self.notify(data).status_code,200)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,2500)
        from .promotions import balance
        self.assertEqual(balance(self.user),100)
        status=self.client.get('/api/nowpayments/status/'+quote['id'],HTTP_AUTHORIZATION='Bearer '+self.token).json()
        self.assertTrue(status['credited'])
        another=self.post('/api/topup-quotes',{'method':'nowpayments','amount_cents':2500}).json()
        self.assertNotEqual(quote['id'],another['id'])
    def test_wrong_order_amount_or_underpayment_never_credits(self):
        quote=self.invoice();base=self.payload(quote)
        for data in [dict(base,price_amount=20),dict(base,actually_paid=24),dict(base,invoice_id=999),dict(base,order_id=str(uuid.uuid4()))]:
            self.assertEqual(self.notify(data).status_code,400)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,0)
    def test_gateway_requires_credentials_and_reuses_created_invoice(self):
        from unittest.mock import patch
        with patch.dict('os.environ',{'NOWPAYMENTS_API_KEY':''}):
            self.assertEqual(self.post('/api/topup-quotes',{'method':'nowpayments','amount_cents':1000}).status_code,409)
        quote=self.invoice()
        with patch('core.nowpayments.provider_request') as provider:
            self.assertEqual(self.post('/api/nowpayments/invoices',{'quote_id':quote['id']}).status_code,200)
            provider.assert_not_called()

@override_settings(SECURE_SSL_REDIRECT=False)
class ForegroundDeliveryTests(TestCase):
    def test_inbox_requires_real_recipient_acknowledgement(self):
        sender=get_user_model().objects.create_user('foregroundsender',password='unique-pass')
        recipient=get_user_model().objects.create_user('foregroundrecipient',password='unique-pass')
        stranger=get_user_model().objects.create_user('foregroundstranger',password='unique-pass')
        for user in (sender,recipient,stranger):Profile.objects.create(user=user);Wallet.objects.create(user=user)
        message=Message.objects.create(sender=sender,recipient=recipient,body='Actual downloaded content')
        receiver_token=issue_token(recipient);stranger_token=issue_token(stranger)
        data=self.client.get('/api/delivery/incoming',HTTP_AUTHORIZATION='Bearer '+receiver_token).json()['items']
        self.assertEqual(data[0]['body'],'Actual downloaded content')
        message.refresh_from_db();self.assertIsNone(message.delivered_at)
        self.client.post('/api/delivery/acknowledge',json.dumps({'ids':[message.pk]}),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+stranger_token)
        message.refresh_from_db();self.assertIsNone(message.delivered_at)
        self.client.post('/api/delivery/acknowledge',json.dumps({'ids':[message.pk]}),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+receiver_token)
        message.refresh_from_db();self.assertIsNotNone(message.delivered_at)
        self.assertEqual(self.client.get('/api/delivery/incoming',HTTP_AUTHORIZATION='Bearer '+receiver_token).json()['items'],[])

@override_settings(SECURE_SSL_REDIRECT=False)
class PresenceAndReadTests(ReliableChatTests):
    def test_read_requires_recipient_and_does_not_follow_get_or_delivery(self):
        from django.utils import timezone
        from datetime import timedelta
        response=self.post(f'/api/messages/{self.receiver.pk}',{'body':'Visible only when opened'},self.sender)
        row=Message.objects.get(pk=response.json()['id'])
        endpoint=f'/api/messages/{self.sender.pk}/read'
        self.post(endpoint,{'ids':[row.pk]},self.stranger);row.refresh_from_db();self.assertIsNone(row.read_at)
        self.post(f'/api/messages/{self.sender.pk}/delivered',{'ids':[row.pk]},self.receiver)
        row.refresh_from_db();self.assertIsNone(row.read_at);self.assertIsNotNone(row.delivered_at)
        self.assertEqual(self.post(endpoint,{'ids':[row.pk]},self.receiver).status_code,200)
        row.refresh_from_db();self.assertIsNotNone(row.read_at);stamp=row.read_at
        self.post(endpoint,{'ids':[row.pk]},self.receiver);row.refresh_from_db();self.assertEqual(stamp,row.read_at)
        peer=f'/api/messages/{self.receiver.pk}'
        def status():return self.client.get(peer,HTTP_AUTHORIZATION='Bearer '+self.tokens[self.sender.pk]).json()['peer']['online']
        self.assertFalse(status());self.post('/api/presence',{},self.receiver);self.assertTrue(status())
        Profile.objects.filter(user=self.receiver).update(last_active_at=timezone.now()-timedelta(seconds=26));self.assertFalse(status())
    def test_read_rejects_invalid_ids(self):
        self.assertEqual(self.post(f'/api/messages/{self.sender.pk}/read',{'ids':['1']},self.receiver).status_code,400)

@override_settings(SECURE_SSL_REDIRECT=False)
class RecoverySignalingTests(InternalCallingTests):
    def test_restart_revisions_and_hangup_are_participant_bound(self):
        call=self.create();self.call(f'/api/calls/{call}',{'action':'accept'},self.callee_token)
        caller=self.room_token(call,self.caller_token);callee=self.room_token(call,self.callee_token)
        path=f'/api/calls/{call}/signal'
        def send(token,kind,data):return self.client.post(path,json.dumps({'kind':kind,'data':data}),content_type='application/json',HTTP_X_CALL_TOKEN=token)
        for revision in (0,1):
            data={'type':'offer','sdp':f'test-sdp-{revision}','revision':revision}
            self.assertEqual(send(caller,'offer',data).status_code,201)
            self.assertEqual(send(caller,'offer',data).status_code,200)
            self.assertEqual(send(caller,'offer',dict(data,sdp='different')).status_code,409)
        self.assertEqual(send(callee,'offer',{'type':'offer','sdp':'forbidden','revision':2}).status_code,403)
        self.assertEqual(send(caller,'offer',{'type':'offer','sdp':'too-many','revision':9}).status_code,400)
        self.assertEqual(send(callee,'hangup',{}).status_code,200)
        self.assertEqual(self.call(f'/api/calls/{call}').json()['status'],'ended')

@override_settings(SECURE_SSL_REDIRECT=False)
class PaidChatTests(CommerceTests):
    def test_paid_message_is_explicit_atomic_and_idempotent(self):
        from .models import ChatCharge
        Profile.objects.filter(user=self.peer).update(creator=True,chat_price_cents=100)
        wallet=Wallet.objects.get(user=self.user);wallet.balance_cents=150;wallet.save()
        body={'body':'Paid message','client_key':str(uuid.uuid4())}
        path=f'/api/messages/{self.peer.pk}'
        self.assertEqual(self.call(path,body).status_code,409)
        self.assertEqual(self.call(path,dict(body,price_consent=99)).status_code,409)
        body['price_consent']=100
        self.assertEqual(self.call(path,body).status_code,201)
        self.assertEqual(self.call(path,body).status_code,200)
        self.assertEqual(ChatCharge.objects.count(),1)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,50)
        self.assertEqual(Wallet.objects.get(user=self.peer).balance_cents,80)
        self.assertEqual(ChatCharge.objects.get().platform_cents,20)
        body['client_key']=str(uuid.uuid4())
        self.assertEqual(self.call(path,body).status_code,409)
        self.assertEqual(Message.objects.count(),1)

    def test_private_paid_media_is_hidden_until_recipient_buys_once(self):
        from .models import ChatCharge
        Profile.objects.filter(user=self.user).update(creator=True)
        media=self.upload('chat_image')
        sent=self.call(f'/api/messages/{self.peer.pk}',{'attachment_id':media['id'],'client_key':str(uuid.uuid4()),'media_price_cents':100})
        self.assertEqual(sent.status_code,201)
        self.assertFalse(sent.json()['media_locked'])
        received=self.call(f'/api/messages/{self.user.pk}',token=self.peer_token).json()['items'][0]
        self.assertTrue(received['media_locked']);self.assertEqual(received['attachment_url'],'')
        # Even obtaining the creator's URL does not permit a recipient grant before buying.
        from .commerce import grant_url
        from django.test import RequestFactory
        from .models import UploadedMedia
        req=RequestFactory().get('/');req.actor=self.peer
        with override_settings(ALLOWED_HOSTS=['testserver']):url=grant_url(req,UploadedMedia.objects.get(pk=media['id']))
        self.assertEqual(self.client.get(url).status_code,403)
        endpoint=f'/api/messages/{sent.json()["id"]}/unlock'
        self.assertEqual(self.call(endpoint,{'price_cents':100}).status_code,404)
        self.assertEqual(self.call(endpoint,{'price_cents':99},self.peer_token).status_code,409)
        self.assertEqual(self.call(endpoint,{'price_cents':100},self.peer_token).status_code,409)
        wallet=Wallet.objects.get(user=self.peer);wallet.balance_cents=100;wallet.save()
        result=self.call(endpoint,{'price_cents':100},self.peer_token)
        self.assertEqual(result.status_code,200);self.assertFalse(result.json()['media_locked'])
        self.assertEqual(self.client.get(result.json()['attachment_url']).status_code,200)
        self.assertEqual(self.call(endpoint,{'price_cents':100},self.peer_token).status_code,200)
        self.assertEqual(ChatCharge.objects.filter(kind='unlock').count(),1)
        self.assertEqual(Wallet.objects.get(user=self.peer).balance_cents,0)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,80)

@override_settings(SECURE_SSL_REDIRECT=False)
class ChatChargeConcurrencyTests(TransactionTestCase):
    def test_parallel_paid_sends_and_unlocks_do_not_duplicate_or_overdraw(self):
        from concurrent.futures import ThreadPoolExecutor
        from django.db import connection,close_old_connections,connections
        from django.test import Client
        from .models import ChatCharge,UploadedMedia
        if connection.vendor!='postgresql':self.skipTest('Row locking requires PostgreSQL')
        buyer=get_user_model().objects.create_user('chat_concurrent_buyer',password='test-only')
        creator=get_user_model().objects.create_user('chat_concurrent_creator',password='test-only')
        Profile.objects.create(user=buyer);Profile.objects.create(user=creator,creator=True,chat_price_cents=100)
        Wallet.objects.create(user=buyer,balance_cents=100);Wallet.objects.create(user=creator)
        token=issue_token(buyer)
        def parallel(path,payloads):
            def post(payload):
                close_old_connections()
                try:return Client().post(path,json.dumps(payload),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+token).status_code
                finally:connections.close_all()
            with ThreadPoolExecutor(max_workers=2) as pool:return list(pool.map(post,payloads))
        results=parallel(f'/api/messages/{creator.pk}',[{'body':'paid','client_key':str(uuid.uuid4()),'price_consent':100} for _ in range(2)])
        self.assertEqual(sorted(results),[201,409]);self.assertEqual(ChatCharge.objects.count(),1)
        media=UploadedMedia.objects.create(owner=creator,purpose='chat_image',file='test-only-image.jpg',mime='image/jpeg',size=10)
        message=Message.objects.create(sender=creator,recipient=buyer,attachment=media,media_price_cents=100)
        Wallet.objects.filter(user=buyer).update(balance_cents=100)
        results=parallel(f'/api/messages/{message.pk}/unlock',[{'price_cents':100}]*2)
        self.assertEqual(results,[200,200]);self.assertEqual(ChatCharge.objects.filter(kind='unlock').count(),1)
        self.assertEqual(Wallet.objects.get(user=buyer).balance_cents,0)
        self.assertEqual(Wallet.objects.get(user=creator).balance_cents,160)
