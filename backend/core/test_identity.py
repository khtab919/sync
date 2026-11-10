import json,uuid,hashlib
from unittest.mock import patch
from django.test import TestCase,TransactionTestCase,Client,override_settings
from django.contrib.auth import get_user_model
from django.db import IntegrityError,transaction,connection
from django.utils import timezone
from .models import Profile,Wallet,SessionToken,Message,UserBlock
from .views import issue_token

@override_settings(SECURE_SSL_REDIRECT=False)
class IdentityTests(TestCase):
    def setUp(self):
        self.user=get_user_model().objects.create_user('existing_handle','old@example.com','ExistingPass!875')
        self.other=get_user_model().objects.create_user('second_handle','other@example.com','SecondPass!875')
        for u in [self.user,self.other]:Profile.objects.create(user=u,display_name='اسم ظاهر');Wallet.objects.create(user=u,balance_cents=1234)
        self.token=issue_token(self.user)
    def post(self,path,p):return self.client.post(path,json.dumps(p),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+self.token)
    def patch(self,p):return self.client.patch('/api/me',json.dumps(p),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+self.token)
    def signup(self,email='new@example.com'):return self.post('/api/register',{'gender':'female','display_name':'نور علي','email':email,'password':'UniqueAccount!875','adult_confirmed':True,'accept_non_explicit_policy':True})
    def test_signup_separates_display_name_handle_and_internal_id(self):
        r=self.signup();self.assertEqual(r.status_code,201)
        u=get_user_model().objects.get(email='new@example.com');self.assertEqual(u.profile.display_name,'نور علي');self.assertNotEqual(u.username,u.profile.display_name);self.assertRegex(u.profile.public_id,r'^[1-9][0-9]{4}$')
        self.assertEqual(self.post('/api/login',{'email':' NEW@EXAMPLE.COM ','password':'UniqueAccount!875'}).status_code,200)
    def test_only_email_and_password_authenticate(self):
        self.assertEqual(self.post('/api/login',{'username':'existing_handle','password':'ExistingPass!875'}).status_code,400)
        self.assertEqual(self.post('/api/login',{'email':'old@example.com','password':'ExistingPass!875'}).status_code,200)
        self.assertEqual(self.post('/api/login',{'email':'old@example.com','password':'WrongPass!875'}).status_code,401)
    def test_new_duplicate_email_is_rejected_case_insensitively(self):
        self.assertEqual(self.signup(' OLD@EXAMPLE.COM ').status_code,409)
        self.assertEqual(get_user_model().objects.count(),2)
    def test_duplicate_legacy_emails_are_not_merged_or_selected_arbitrarily(self):
        self.other.email='OLD@example.com';self.other.save()
        self.assertEqual(self.post('/api/login',{'email':'old@example.com','password':'ExistingPass!875'}).status_code,401)
        self.assertEqual(get_user_model().objects.count(),2);self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,1234)
        self.assertEqual(self.patch({'bio':'Retained access','email':'old@example.com'}).status_code,200)
        self.assertEqual(self.patch({'email':'fixed@example.com'}).status_code,200)
        self.assertEqual(self.post('/api/login',{'email':'fixed@example.com','password':'ExistingPass!875'}).status_code,200)
    def test_handle_is_unique_and_does_not_change_display_name_or_ids(self):
        public=self.user.profile.public_id;internal=self.user.pk
        r=self.patch({'username':'@new_handle'});self.assertEqual(r.status_code,200);self.assertEqual(r.json()['username'],'new_handle');self.assertEqual(r.json()['display_name'],'اسم ظاهر');self.assertEqual(r.json()['public_id'],public);self.assertEqual(r.json()['id'],internal)
        self.assertEqual(self.patch({'username':'SECOND_HANDLE'}).status_code,409)
        self.assertEqual(self.patch({'username':'bad name'}).status_code,400)
        self.assertEqual(self.patch({'public_id':'12345'}).status_code,400)
    def test_legacy_name_fallback_is_frozen_before_handle_change(self):
        Profile.objects.filter(user=self.user).update(display_name='')
        r=self.patch({'username':'new_distinct_handle'})
        self.assertEqual(r.status_code,200);self.assertEqual(r.json()['display_name'],'existing_handle')

    def test_display_name_updates_never_replace_handle(self):
        r=self.patch({'display_name':'اسم آخر'});self.assertEqual(r.status_code,200);self.assertEqual(r.json()['username'],'existing_handle')
    def test_legacy_handle_can_be_retained_while_editing_profile(self):
        self.user.username='اسم قديم';self.user.save()
        self.assertEqual(self.patch({'username':'اسم قديم','bio':'Updated bio'}).status_code,200)
    def test_public_id_is_database_constrained_and_collision_retries(self):
        with self.assertRaises(IntegrityError),transaction.atomic():Profile.objects.filter(user=self.other).update(public_id=self.user.profile.public_id)
        with self.assertRaises(IntegrityError),transaction.atomic():Profile.objects.filter(user=self.other).update(public_id='1')
        with self.assertRaises(IntegrityError),transaction.atomic():Profile.objects.filter(user=self.other).update(public_id='abcde')
        from .identity import allocate_public_id
        used=int(self.user.profile.public_id)-10000
        free=next(n for n in range(10000,100000) if not Profile.objects.filter(public_id=str(n)).exists())
        with patch('core.identity.secrets.randbelow',side_effect=[used,free-10000]):self.assertEqual(allocate_public_id(),str(free))
    def test_own_public_number_is_searchable(self):
        r=self.client.get('/api/accounts/search',{'q':self.user.profile.public_id},HTTP_AUTHORIZATION='Bearer '+self.token)
        self.assertEqual(r.status_code,200);self.assertEqual(r.json()['items'][0]['id'],self.user.pk)

    def test_public_search_and_blocks_preserve_private_information(self):
        public=self.other.profile.public_id
        r=self.client.get('/api/accounts/search',{'q':public},HTTP_AUTHORIZATION='Bearer '+self.token)
        self.assertEqual(r.status_code,200);self.assertEqual(r.json()['items'][0]['id'],self.other.pk);self.assertEqual(r.json()['items'][0]['email'],'');self.assertEqual(r.json()['items'][0]['public_id'],public)
        UserBlock.objects.create(user=self.user,blocked=self.other)
        self.assertEqual(self.client.get('/api/accounts/search',{'q':public},HTTP_AUTHORIZATION='Bearer '+self.token).json()['items'],[])
    def test_email_change_is_unique_and_preserves_internal_relations(self):
        m=Message.objects.create(sender=self.user,recipient=self.other,body='Retained')
        self.assertEqual(self.patch({'email':'OTHER@EXAMPLE.COM','display_name':'Rejected name'}).status_code,409)
        self.assertEqual(self.patch({'email':'new-address@example.com'}).status_code,200)
        m.refresh_from_db();self.assertEqual(m.sender_id,self.user.pk);self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,1234)

@override_settings(SECURE_SSL_REDIRECT=False)
class IdentityConcurrencyTests(TransactionTestCase):
    def test_same_email_cannot_create_two_accounts(self):
        if connection.vendor!='postgresql':self.skipTest('Requires PostgreSQL row locking')
        from concurrent.futures import ThreadPoolExecutor
        from django.db import close_old_connections,connections
        def create(_):
            close_old_connections()
            try:return Client().post('/api/register',json.dumps({'gender':'male','display_name':'نفس الاسم','email':'Concurrent@example.com','password':'UniqueConcurrent!875','adult_confirmed':True,'accept_non_explicit_policy':True}),content_type='application/json').status_code
            finally:connections.close_all()
        with ThreadPoolExecutor(max_workers=2) as pool:result=list(pool.map(create,range(2)))
        self.assertEqual(sorted(result),[201,409]);self.assertEqual(get_user_model().objects.filter(email='concurrent@example.com').count(),1)

class PublicIdentityMigrationTests(TransactionTestCase):
    def test_backfill_keeps_passwords_internal_ids_wallets_and_messages(self):
        from django.db.migrations.executor import MigrationExecutor
        executor=MigrationExecutor(connection);executor.migrate([('core','0021_adminevent_details')]);old=executor.loader.project_state([('core','0021_adminevent_details')]).apps
        try:
            U=old.get_model('auth','User');P=old.get_model('core','Profile');W=old.get_model('core','Wallet');M=old.get_model('core','Message')
            one=U.objects.create(username='legacy-one',email='Legacy@example.com',password='retained-hash');two=U.objects.create(username='legacy-two',email='legacy@example.com',password='second-hash')
            P.objects.create(user_id=one.pk,display_name='قديم');P.objects.create(user_id=two.pk);W.objects.create(user_id=one.pk,balance_cents=5432,reserved_cents=100);m=M.objects.create(sender_id=one.pk,recipient_id=two.pk,body='Existing private message')
            ids=(one.pk,two.pk,m.pk)
            executor=MigrationExecutor(connection);executor.migrate(executor.loader.graph.leaf_nodes())
            self.assertIsNone(Profile.objects.get(user_id=ids[1]).gender);self.assertTrue(Profile.objects.get(user_id=ids[1]).publishing_allowed);self.assertEqual(Profile.objects.get(user_id=ids[1]).display_name,'legacy-two');self.assertEqual(Profile.objects.get(user_id=ids[0]).display_name,'قديم');self.assertEqual(get_user_model().objects.get(pk=ids[0]).password,'retained-hash');self.assertEqual(get_user_model().objects.get(pk=ids[1]).email,'legacy@example.com');self.assertEqual(Wallet.objects.get(user_id=ids[0]).balance_cents,5432);self.assertEqual(Message.objects.get(pk=ids[2]).sender_id,ids[0]);public=list(Profile.objects.filter(user_id__in=ids[:2]).values_list('public_id',flat=True));self.assertEqual(len(set(public)),2)
            for v in public:self.assertRegex(v,r'^[1-9][0-9]{4}$')
        finally:
            executor=MigrationExecutor(connection);executor.migrate(executor.loader.graph.leaf_nodes())
