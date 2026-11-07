"""Real API regression checks for universal publishing and optional legacy gender."""
import json
from django.test import TestCase, override_settings
from django.contrib.auth import get_user_model
from .models import Profile, Wallet, Service
from .views import issue_token, ApiError
from .live import eligible

@override_settings(SECURE_SSL_REDIRECT=False)
class GenderAccessTests(TestCase):
    def setUp(self):
        self.user=get_user_model().objects.create_user('legacy','legacy@example.com','LegacyPass!875')
        Profile.objects.create(user=self.user,display_name='قديم')
        Wallet.objects.create(user=self.user,balance_cents=1200)
        self.token=issue_token(self.user)
    def call(self,path,p=None,method='post'):
        auth={'HTTP_AUTHORIZATION':'Bearer '+self.token}
        if p is None:return self.client.get(path,**auth)
        return getattr(self.client,method)(path,json.dumps(p),content_type='application/json',**auth)
    def test_gender_required_only_for_new_registration(self):
        p={'display_name':'حساب جديد','email':'new@example.com','password':'NewAccount!875','adult_confirmed':True,'accept_non_explicit_policy':True}
        for invalid in [None,'','other',1,True]:
            p['gender']=invalid
            self.assertEqual(self.call('/api/register',p).json()['error'],'invalid_gender')
        for gender in ['male','female']:
            p.update(gender=gender,email=gender+'@example.com')
            self.assertEqual(self.call('/api/register',p).status_code,201)
            profile=Profile.objects.get(user__email=p['email'])
            self.assertEqual(profile.gender,gender);self.assertTrue(profile.publishing_allowed);self.assertFalse(profile.creator)
        self.assertEqual(self.call('/api/login',{'email':'legacy@example.com','password':'LegacyPass!875'}).status_code,200)
        self.assertIsNone(self.call('/api/me').json()['gender'])
    def test_legacy_profile_gender_update_preserves_identity_and_money(self):
        before=self.user.profile.public_id
        for gender in ['male','female']:
            r=self.call('/api/me',{'gender':gender},'patch');self.assertEqual(r.status_code,200);self.assertEqual(r.json()['gender'],gender)
        self.assertEqual(self.call('/api/me',{'gender':None},'patch').status_code,400)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,1200)
        self.assertEqual(Profile.objects.get(user=self.user).public_id,before)
    def test_all_genders_and_legacy_accounts_can_publish_and_host_without_badge(self):
        for gender in [None,'male','female']:
            Profile.objects.filter(user=self.user).update(gender=gender)
            self.user.refresh_from_db()
            r=self.call('/api/services',{'title':'خدمة حقيقية','description':'تفاصيل الخدمة وشروطها','price_cents':100})
            self.assertEqual(r.status_code,201,r.content)
            self.assertFalse(Service.objects.get(pk=r.json()['id']).approved)
            self.assertEqual(eligible(self.user,host=True).gender,gender)
            self.assertFalse(Profile.objects.get(user=self.user).creator)
        Profile.objects.filter(user=self.user).update(publishing_allowed=False,broadcast_allowed=False)
        self.assertEqual(self.call('/api/services',{'title':'خدمة ممنوعة','description':'تفاصيل الخدمة وشروطها','price_cents':100}).status_code,403)
        self.user.refresh_from_db()
        with self.assertRaises(ApiError) as denied:eligible(self.user,host=True)
        self.assertEqual(denied.exception.message,'broadcast_permission_required')
    def test_directory_and_follow_include_regular_accounts(self):
        peer=get_user_model().objects.create_user('regular','regular@example.com','PeerPassword!875')
        Profile.objects.create(user=peer,gender='male',display_name='حساب عادي');Wallet.objects.create(user=peer)
        self.assertIn(peer.pk,[x['id'] for x in self.call('/api/creators').json()['items']])
        self.assertEqual(self.call(f'/api/creators/{peer.pk}/follow',{'enabled':True}).status_code,200)
