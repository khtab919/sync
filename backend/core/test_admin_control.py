import hashlib,json
from datetime import timedelta
from unittest.mock import patch
import pyotp
from django.test import TestCase,override_settings
from django.contrib.auth import get_user_model
from django.utils import timezone
from .models import *
from .views import issue_token
from .admin_security import cipher,SCOPES

@override_settings(SECURE_SSL_REDIRECT=False)
class NativeAdminSecurityTests(TestCase):
    def setUp(self):
        U=get_user_model();self.owner=U.objects.create_superuser('super_admin','owner@example.com','strong-owner-password');self.staff=U.objects.create_user('staff',password='strong-staff-password',is_staff=True);self.user=U.objects.create_user('customer',password='strong-user-password')
        self.tokens={}
        for u in [self.owner,self.staff,self.user]:Profile.objects.create(user=u);Wallet.objects.create(user=u);self.tokens[u.pk]=issue_token(u)
        AdminAccess.objects.create(user=self.staff,role='support')
    def call(self,path,data=None,user=None):
        u=user or self.owner;kw={'HTTP_AUTHORIZATION':'Bearer '+self.tokens[u.pk]}
        return self.client.get(path,**kw) if data is None else self.client.post(path,json.dumps(data),content_type='application/json',**kw)
    def verify(self,u):
        secret=pyotp.random_base32();AdminAccess.objects.update_or_create(user=u,defaults={'totp_secret':cipher().encrypt(secret.encode()).decode(),'last_counter':-1})
        result=self.call('/api/admin/auth',{'action':'verify','code':pyotp.TOTP(secret).now()},u);self.assertEqual(result.status_code,200);return secret
    def test_owner_requires_real_mfa_and_replay_is_rejected(self):
        self.assertEqual(self.call('/api/admin/control/dashboard').json()['error'],'admin_mfa_required')
        secret=self.verify(self.owner)
        self.assertEqual(self.call('/api/admin/control/dashboard').status_code,200)
        self.assertEqual(self.call('/api/admin/auth',{'action':'verify','code':pyotp.TOTP(secret).now()}).json()['error'],'invalid_totp')
        a=AdminAccess.objects.get(user=self.owner);self.assertNotIn(secret,a.totp_secret)
    def test_mfa_rate_limit_and_short_admin_session(self):
        self.assertLessEqual((SessionToken.objects.get(user=self.owner).expires_at-timezone.now()).total_seconds(),3600)
        for _ in range(5):self.call('/api/admin/auth',{'action':'setup','password':'wrong'})
        self.assertEqual(self.call('/api/admin/auth',{'action':'setup','password':'strong-owner-password'}).status_code,429)
    def test_registration_cannot_create_reserved_owner(self):
        r=self.call('/api/register',{'username':'super_admin','password':'safe-long-pass','email':'test@example.com','adult_confirmed':True,'accept_non_explicit_policy':True,'is_superuser':True},self.user)
        self.assertEqual(r.status_code,409);self.user.refresh_from_db();self.assertFalse(self.user.is_staff)
    def test_support_preview_is_readonly_audited_and_has_no_private_content(self):
        self.verify(self.staff);Message.objects.create(sender=self.user,recipient=self.owner,body='PRIVATE_SECRET')
        r=self.call(f'/api/admin/control/preview/{self.user.pk}',{'reason':'Customer requested support'},self.staff)
        self.assertEqual(r.status_code,200);self.assertTrue(r.json()['read_only']);self.assertNotIn('PRIVATE_SECRET',r.content.decode());self.assertNotIn('wallet',r.json());self.assertNotIn('token',r.json());self.assertTrue(AdminEvent.objects.filter(action='account_preview',actor=self.staff).exists())
        self.assertEqual(self.call('/api/admin/accounts/'+str(self.user.pk),{'action':'credit','amount_cents':100},self.staff).status_code,403)
    def test_no_self_escalation_or_owner_changes(self):
        self.verify(self.staff);self.verify(self.owner)
        self.assertEqual(self.call('/api/admin/control/roles',{'user_id':self.staff.pk,'role':'general','permissions':SCOPES,'reason':'Attempt escalation'},self.staff).status_code,403)
        self.assertEqual(self.call('/api/admin/control/roles',{'user_id':self.owner.pk,'role':'support','reason':'Attempt owner change'}).status_code,403)
    def test_custom_permissions_can_remove_role_defaults_and_revoke_sessions(self):
        self.verify(self.owner)
        r=self.call('/api/admin/control/roles',{'user_id':self.staff.pk,'role':'finance','permissions':['dashboard.view'],'reason':'Least privilege assignment'})
        self.assertEqual(r.status_code,200);self.assertFalse(SessionToken.objects.filter(user=self.staff).exists())
        from .admin_security import scopes
        self.assertEqual(scopes(self.staff),['dashboard.view'])
    def test_expired_or_partial_restrictions_do_not_ban_other_features(self):
        self.verify(self.owner)
        r=self.call('/api/admin/control/sanctions',{'user_id':self.user.pk,'kind':'messages','hours':1,'reason':'Repeated harassment'})
        self.assertEqual(r.status_code,200)
        self.assertEqual(self.call('/api/messages/'+str(self.owner.pk),{'body':'Blocked'},self.user).json()['error'],'feature_restricted')
        self.assertEqual(self.call('/api/me',user=self.user).status_code,200)
        sanction=AccountSanction.objects.get(user=self.user);sanction.expires_at=timezone.now()-timedelta(seconds=1);sanction.save()
        from .admin_security import blocked
        self.assertFalse(blocked(self.user,'messages'))
    def test_financial_adjustment_requires_independent_exact_approval(self):
        self.verify(self.owner);AdminAccess.objects.filter(user=self.staff).update(role='finance');self.verify(self.staff)
        import uuid
        path=f'/api/admin/accounts/{self.user.pk}';p={'action':'credit','amount_cents':500,'request_key':str(uuid.uuid4()),'reason':'Reviewed balance correction'}
        r=self.call(path,p);self.assertEqual(r.status_code,409);self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,0);pk=r.json()['approval_id']
        self.assertEqual(self.call('/api/admin/control/approvals',{'id':pk,'action':'approve','reason':'Self review attempt'}).status_code,403)
        self.assertEqual(self.call('/api/admin/control/approvals',{'id':pk,'action':'approve','reason':'Independent evidence checked'},self.staff).status_code,200)
        p['approval_id']=pk
        self.assertEqual(self.call(path,dict(p,amount_cents=600)).status_code,409)
        self.assertEqual(self.call(path,p).status_code,200);self.assertEqual(self.call(path,p).status_code,200)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,500);self.assertEqual(BalanceAdjustment.objects.count(),1)
    def test_emergency_stop_and_appeals(self):
        self.verify(self.owner)
        self.assertEqual(self.call('/api/admin/control/settings',{'key':'withdrawals','enabled':False,'reason':'Investigate payment incident'}).status_code,200)
        self.assertEqual(self.call('/api/withdrawals',{'amount_cents':100,'destination':'test','request_key':'test'},self.user).status_code,503)
        self.assertEqual(self.call('/api/my-cases',{'body':'Please review restriction'},self.user).status_code,200)
    def test_stale_mfa_cannot_change_settings_and_external_admin_disabled(self):
        self.verify(self.owner);SessionToken.objects.filter(user=self.owner).update(admin_verified_at=timezone.now()-timedelta(minutes=11))
        self.assertEqual(self.call('/api/admin/control/settings',{'key':'live','enabled':False,'reason':'Incident response'}).json()['error'],'admin_mfa_required')
        self.assertEqual(self.client.get('/admin/').status_code,404)
    def test_financial_report_and_preview_never_include_tokens_or_chat_bodies(self):
        self.verify(self.owner)
        r=self.call('/api/admin/control/dashboard?period=week');self.assertIn('commission_cents',r.json()['stats'])
        self.assertEqual(self.call('/api/admin/control/dashboard?period=invalid').status_code,400)
    def test_notice_delivery_is_scoped_to_recipient(self):
        self.verify(self.owner)
        self.assertEqual(self.call('/api/admin/control/notices',{'user_id':self.user.pk,'title':'Review result','body':'Your request was reviewed','reason':'Customer support response'}).status_code,200)
        self.assertEqual(len(self.call('/api/notices',user=self.user).json()['items']),1);self.assertEqual(self.call('/api/notices',user=self.staff).json()['items'],[])
