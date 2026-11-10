import threading,uuid
from concurrent.futures import ThreadPoolExecutor
from unittest import skipUnless
from django.test import TransactionTestCase,override_settings
from django.contrib.auth import get_user_model
from django.db import connection,close_old_connections
from .models import Profile,Wallet,ExperienceAward,PushDevice,PushDelivery
from .activity_events import award
from .push import enqueue

@skipUnless(connection.vendor=='postgresql','Requires PostgreSQL row locks')
@override_settings(PASSWORD_HASHERS=['django.contrib.auth.hashers.MD5PasswordHasher'])
class ReleaseConcurrencyTests(TransactionTestCase):
    def setUp(self):
        self.user=get_user_model().objects.create_user('concurrent','c@example.org','pass')
        Profile.objects.create(user=self.user);Wallet.objects.create(user=self.user)
    def test_award_retries_are_once_only(self):
        uid=self.user.pk
        def run(_):
            close_old_connections()
            try:award(uid,'same-event',80)
            finally:close_old_connections()
        with ThreadPoolExecutor(max_workers=4) as pool:list(pool.map(run,range(8)))
        self.assertEqual(ExperienceAward.objects.filter(user=self.user).count(),1)
        self.assertEqual(ExperienceAward.objects.get().points,80)
    def test_push_enqueue_concurrent_retry_is_unique(self):
        PushDevice.objects.create(user=self.user,token='c'*64);uid=self.user.pk
        def run(_):
            close_old_connections()
            try:enqueue(uid,'same-event','wallet','1','update')
            finally:close_old_connections()
        with ThreadPoolExecutor(max_workers=4) as pool:list(pool.map(run,range(8)))
        self.assertEqual(PushDelivery.objects.count(),1)
