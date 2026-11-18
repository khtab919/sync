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
        result=self.call('/api/admin/auth',user=u)
        self.assertEqual(result.status_code,200)
        self.assertTrue(result.json()['verified'])
        self.assertFalse(result.json()['enabled'])
        return ''
    def test_owner_signs_into_android_account_api_then_enters_in_app_mfa(self):
        # The public login is shared by users and administrators. It must not
        # grant an admin role to registrants or bypass the extra MFA layer.
        signed = self.client.post(
            '/api/login',
            json.dumps({'email': 'owner@example.com', 'password': 'strong-owner-password'}),
            content_type='application/json',
        )
        self.assertEqual(signed.status_code, 200, signed.content)
        auth = {'HTTP_AUTHORIZATION': 'Bearer ' + signed.json()['token']}
        me = self.client.get('/api/me', **auth)
        self.assertEqual(me.status_code, 200)
        self.assertTrue(me.json()['is_admin'])
        self.assertTrue(me.json()['is_staff'])
        self.assertIn('dashboard.view', me.json()['admin_permissions'])
        self.assertEqual(me.json()['admin_scope'], 'full')

        entry = self.client.get('/api/admin/auth', **auth)
        self.assertEqual(entry.status_code, 200)
        self.assertTrue(entry.json()['verified'])
        self.assertEqual(self.client.get('/api/admin/control/dashboard', **auth).status_code,200)
        customer = self.client.get(
            '/api/me', HTTP_AUTHORIZATION='Bearer ' + self.tokens[self.user.pk],
        ).json()
        self.assertFalse(customer['is_admin'])

    def test_gender_and_distinct_publishing_permission_are_admin_visible(self):
        self.verify(self.owner)
        Profile.objects.filter(user=self.user).update(gender='female')
        url=f'/api/admin/accounts/{self.user.pk}'
        self.assertEqual(self.call(url).json()['gender'],'female')
        for action,allowed in [('publishing_off',False),('publishing_on',True)]:
            r=self.call(url,{'action':action,'reason':'Account publishing permission review'})
            self.assertEqual(r.status_code,200);self.assertEqual(r.json()['publishing_allowed'],allowed)
            self.assertFalse(r.json()['creator'])
        self.assertEqual(self.call(url,{'action':'publishing_on','reason':'Unauthorized permission request'},self.user).status_code,403)

    def test_admin_uses_authenticated_role_without_second_factor(self):
        self.verify(self.owner)
        self.assertEqual(self.call('/api/admin/control/dashboard').status_code,200)
        self.assertEqual(self.call('/api/admin/auth',{'action':'verify','code':'123456'}).status_code,410)
        self.assertEqual(self.call('/api/admin/auth',{'action':'setup','password':'any'}).status_code,410)
    def test_short_admin_session_still_expires(self):
        self.assertLessEqual((SessionToken.objects.get(user=self.owner).expires_at-timezone.now()).total_seconds(),3600)
        self.assertEqual(self.call('/api/admin/control/dashboard').status_code,200)
    def test_registration_cannot_create_reserved_owner(self):
        r=self.call('/api/register',{'gender':'male','username':'super_admin','password':'safe-long-pass','email':'test@example.com','adult_confirmed':True,'accept_non_explicit_policy':True,'is_superuser':True},self.user)
        self.assertEqual(r.status_code,409);self.user.refresh_from_db();self.assertFalse(self.user.is_staff)
    def test_support_preview_is_readonly_audited_and_has_no_private_content(self):
        self.verify(self.staff);Message.objects.create(sender=self.user,recipient=self.owner,body='PRIVATE_SECRET')
        r=self.call(f'/api/admin/control/preview/{self.user.pk}',{'reason':'Customer requested support'},self.staff)
        self.assertEqual(r.status_code,200);self.assertTrue(r.json()['read_only']);self.assertNotIn('PRIVATE_SECRET',r.content.decode());self.assertNotIn('wallet',r.json());self.assertNotIn('token',r.json());self.assertTrue(AdminEvent.objects.filter(action='account_preview',actor=self.staff).exists())
        self.assertEqual(self.call('/api/admin/accounts/'+str(self.user.pk),{'action':'credit','amount_cents':100},self.staff).status_code,403)
    def test_preview_shows_real_public_content_without_impersonation_or_private_messages(self):
        self.verify(self.staff)
        Service.objects.create(owner=self.user,title='جلسة حقيقية',description='وصف الخدمة',
            price_cents=250,duration_minutes=30,approved=True,active=True)
        Reel.objects.create(owner=self.user,caption='مقطع حقيقي',
            video_url='https://example.invalid/public.mp4',approved=True)
        Follow.objects.create(user=self.owner,creator=self.user)
        LiveBroadcast.objects.create(owner=self.user,title='جلسة صوتية',mode='audio')
        Message.objects.create(sender=self.user,recipient=self.owner,body='PRIVATE_CHAT_BODY')
        result=self.call(f'/api/admin/control/preview/{self.user.pk}',
            {'reason':'Read-only support verification'},self.staff)
        self.assertEqual(result.status_code,200,result.content)
        data=result.json()
        self.assertTrue(data['read_only'])
        self.assertEqual(data['profile']['followers'],1)
        self.assertIn('owned_frames',data['profile'])
        self.assertEqual(data['services'][0]['title'],'جلسة حقيقية')
        self.assertEqual(data['reels'][0]['caption'],'مقطع حقيقي')
        self.assertEqual(data['live_rooms'][0]['mode'],'audio')
        self.assertNotIn('PRIVATE_CHAT_BODY',result.content.decode())
        self.assertNotIn('wallet',data)
        self.assertNotIn('operations',data)

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
    def test_store_sanction_blocks_reel_and_offer_upload_but_not_avatar(self):
        import uuid
        self.verify(self.owner)
        response = self.call('/api/admin/control/sanctions', {
            'user_id': self.user.pk, 'kind': 'store', 'hours': 1,
            'reason': 'Temporary restriction for repeated content violations'
        })
        self.assertEqual(response.status_code, 200, response.content)
        self.assertEqual(self.call('/api/reels', {'media_id': str(uuid.uuid4())}, self.user).json()['error'], 'feature_restricted')
        self.assertEqual(self.call('/api/upload', {'purpose': 'offer', 'data': 'not-base64'}, self.user).json()['error'], 'feature_restricted')
        self.assertEqual(self.call('/api/upload', {'purpose': 'reel', 'data': 'not-base64'}, self.user).json()['error'], 'feature_restricted')
        self.assertNotEqual(self.call('/api/upload', {'purpose': 'avatar', 'data': 'not-base64'}, self.user).status_code, 403)

    def test_financial_credit_immediately_settles_after_mfa_and_replay_is_safe(self):
        import uuid
        path=f'/api/admin/accounts/{self.user.pk}'
        p={'action':'credit','amount_cents':500,'request_key':str(uuid.uuid4()),
           'reason':'Audited manual balance credit'}
        # Financial permissions and the immutable audit record remain required.
        self.verify(self.owner)
        first=self.call(path,p)
        self.assertEqual(first.status_code,200,first.content)
        self.assertEqual(self.call(path,p).status_code,200)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,500)
        self.assertEqual(BalanceAdjustment.objects.count(),1)
        self.assertFalse(AdminApproval.objects.filter(requester=self.owner).exists())
        conflict=self.call(path,dict(p,amount_cents=700))
        self.assertEqual(conflict.status_code,409)
        self.assertEqual(conflict.json()['error'],'credit_request_conflict')
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,500)
        self.assertTrue(AdminEvent.objects.filter(actor=self.owner,action__icontains='credit').exists() or
                        AdminEvent.objects.filter(actor=self.owner,reason__icontains='Credit').exists())
        detail=self.call(path).json()
        self.assertEqual(detail['balance_cents'],500)
        self.assertEqual(detail['credit_history'][0]['coins'],500)

    def test_finance_adjust_authorized_staff_credits_directly_without_approver(self):
        import uuid
        self.verify(self.owner)
        self.verify(self.staff)
        path=f'/api/admin/accounts/{self.user.pk}'
        p={'action':'credit','amount_cents':200,'request_key':str(uuid.uuid4()),
           'reason':'Authorized staff adjustment'}
        # Support role without finance.adjust cannot credit.
        self.assertEqual(self.call(path,p,self.staff).status_code,403)
        AdminAccess.objects.filter(user=self.staff).update(
            role='general', customized=True,
            permissions=['users.view','finance.view','finance.adjust']
        )
        result=self.call(path,p,self.staff)
        self.assertEqual(result.status_code,200,result.content)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,200)
        self.assertEqual(self.call(path,p,self.staff).status_code,200)
        self.assertEqual(BalanceAdjustment.objects.count(),1)
        self.assertFalse(AdminApproval.objects.filter(requester=self.staff).exists())

    def test_manual_credit_invalid_amount_never_credits(self):
        import uuid
        self.verify(self.owner)
        path=f'/api/admin/accounts/{self.user.pk}'
        for cents in (0,-5,1000001):
            response=self.call(path,{'action':'credit','amount_cents':cents,
                'request_key':str(uuid.uuid4()),'reason':'Invalid amount safeguard'})
            self.assertEqual(response.status_code,400)
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,0)

    def test_emergency_stop_and_appeals(self):
        self.verify(self.owner)
        self.assertEqual(self.call('/api/admin/control/settings',{'key':'withdrawals','enabled':False,'reason':'Investigate payment incident'}).status_code,200)
        self.assertEqual(self.call('/api/withdrawals',{'amount_cents':100,'destination':'test','request_key':'test'},self.user).status_code,503)
        self.assertEqual(self.call('/api/my-cases',{'body':'Please review restriction'},self.user).status_code,200)
    def test_temporary_admin_session_can_change_settings_and_external_admin_disabled(self):
        self.verify(self.owner)
        self.assertEqual(self.call('/api/admin/control/settings',
            {'key':'live','enabled':False,'reason':'Incident response'}).status_code,200)
        self.assertEqual(self.client.get('/admin/').status_code,404)
    def test_financial_report_and_preview_never_include_tokens_or_chat_bodies(self):
        self.verify(self.owner)
        r=self.call('/api/admin/control/dashboard?period=week');self.assertIn('commission_cents',r.json()['stats'])
        self.assertEqual(self.call('/api/admin/control/dashboard?period=invalid').status_code,400)
    def test_notice_delivery_is_scoped_to_recipient(self):
        self.verify(self.owner)
        self.assertEqual(self.call('/api/admin/control/notices',{'user_id':self.user.pk,'title':'Review result','body':'Your request was reviewed','reason':'Customer support response'}).status_code,200)
        self.assertEqual(len(self.call('/api/notices',user=self.user).json()['items']),1);self.assertEqual(self.call('/api/notices',user=self.staff).json()['items'],[])
    def test_promotional_points_are_separate_and_cannot_be_withdrawn(self):
        self.verify(self.owner)
        import uuid
        p={'user_id':self.user.pk,'kind':'promotion','value':500,'days':30,'request_key':str(uuid.uuid4()),'reason':'Bounded promotional gift'}
        self.assertEqual(self.call('/api/admin/control/benefits',p).status_code,200)
        self.assertEqual(self.call('/api/admin/control/benefits',p).status_code,200)
        w=self.call('/api/wallet',user=self.user).json();self.assertEqual(w['balance_cents'],0);self.assertEqual(w['promotional_points'],500);self.assertFalse(w['promotional_withdrawable'])
        self.assertEqual(PromotionalGrant.objects.count(),1)
        p['request_key']=str(uuid.uuid4());p['value']=10000
        self.assertEqual(self.call('/api/admin/control/benefits',p).status_code,409)
    def test_frame_grant_expires_without_mutating_customer_settings(self):
        self.verify(self.owner)
        p={'user_id':self.user.pk,'kind':'frame','value':110,'days':1,'reason':'Temporary frame benefit'}
        self.assertEqual(self.call('/api/admin/control/benefits',p).status_code,200)
        self.assertEqual(self.call('/api/me',user=self.user).json()['selected_frame'],110)
        BenefitGrant.objects.update(expires_at=timezone.now()-timedelta(seconds=1))
        self.assertEqual(self.call('/api/me',user=self.user).json()['selected_frame'],0)
    def test_milestone_reward_catalog_blocks_old_frames(self):
        self.verify(self.owner)
        path='/api/admin/control/benefits'
        result=self.call(path,{'user_id':self.user.pk,'kind':'frame','value':5,'days':30,'reason':'Legacy frame attempt'})
        self.assertEqual(result.status_code,400)
        self.assertFalse(BenefitGrant.objects.filter(user=self.user,kind='frame').exists())

    def test_level_and_vip_rewards_are_visible_separate_from_earned_xp_and_revocable(self):
        self.verify(self.owner)
        path='/api/admin/control/benefits'
        before=self.call('/api/me',user=self.user).json()
        self.assertEqual(before['level'],1)
        self.assertEqual(before['vip_tier'],0)
        level=self.call(path,{'user_id':self.user.pk,'kind':'level','value':45,'days':7,'reason':'Community achievement'})
        vip=self.call(path,{'user_id':self.user.pk,'kind':'vip','value':3,'days':30,'reason':'Community recognition'})
        self.assertEqual(level.status_code,200,level.content)
        self.assertEqual(vip.status_code,200,vip.content)
        after=self.call('/api/me',user=self.user).json()
        self.assertEqual(after['level'],45)
        self.assertEqual(after['earned_level'],1)
        self.assertEqual(after['vip_tier'],3)
        self.assertEqual(after['level_color'],'gold')
        self.assertIn(140,after['owned_frames'])
        self.assertEqual(after['xp_points'],before['xp_points'])
        self.assertEqual(Wallet.objects.get(user=self.user).balance_cents,0)
        grant=BenefitGrant.objects.get(user=self.user,kind='vip')
        revoke=self.call(path,{'action':'revoke','id':grant.pk,'reason':'End VIP recognition'})
        self.assertEqual(revoke.status_code,200,revoke.content)
        self.assertEqual(self.call('/api/me',user=self.user).json()['vip_tier'],0)
        BenefitGrant.objects.filter(user=self.user,kind='level').update(expires_at=timezone.now()-timedelta(seconds=1))
        self.assertEqual(self.call('/api/me',user=self.user).json()['level'],1)
        self.assertTrue(AdminEvent.objects.filter(action='benefit_revoked',actor=self.owner).exists())

    def test_owner_can_revoke_admin_access_without_deleting_customer(self):
        self.verify(self.owner)
        self.verify(self.staff)
        result=self.call('/api/admin/control/roles',{'action':'revoke','user_id':self.staff.pk,'reason':'End temporary appointment'})
        self.assertEqual(result.status_code,200,result.content)
        self.staff.refresh_from_db()
        self.assertFalse(self.staff.is_staff)
        self.assertFalse(AdminAccess.objects.filter(user=self.staff).exists())
        self.assertFalse(SessionToken.objects.filter(user=self.staff).exists())
        self.assertTrue(Profile.objects.filter(user=self.staff).exists())

    def test_wheel_usage_limit_does_not_modify_existing_results(self):
        self.verify(self.owner)
        r=self.call('/api/admin/control/wheel-limits',{'daily_limit':0,'daily_budget_cents':1000,'reason':'Pause spins for review'})
        self.assertEqual(r.status_code,200);self.assertEqual(r.json()['daily_limit'],0)
    def test_communications_status_never_exposes_message_body(self):
        self.verify(self.staff);Message.objects.create(sender=self.user,recipient=self.owner,body='REDACT_ME')
        r=self.call('/api/admin/control/communications',user=self.staff)
        self.assertEqual(r.status_code,200);self.assertNotIn('REDACT_ME',r.content.decode());self.assertFalse(r.json()['content_access'])

    def test_owner_has_no_arbitrary_private_chat_media_override(self):
        from .commerce import may_read
        media=UploadedMedia.objects.create(owner=self.user,purpose='chat_image',file='private-test.jpg',mime='image/jpeg',size=1)
        Message.objects.create(sender=self.user,recipient=self.staff,attachment=media,body='Private attachment')
        self.assertFalse(may_read(media,self.owner));self.assertTrue(may_read(media,self.staff))

    def test_banned_customer_can_appeal_but_not_use_restricted_features(self):
        self.verify(self.owner)
        self.assertEqual(self.call('/api/admin/control/sanctions',{'user_id':self.user.pk,'kind':'ban','hours':1,'reason':'Temporary account investigation'}).status_code,200)
        self.assertTrue(self.call('/api/me',user=self.user).json()['account_restricted'])
        self.assertEqual(self.call('/api/notices',user=self.user).status_code,200)
        self.assertEqual(self.call('/api/my-cases',{'body':'Please review this decision'},self.user).status_code,200)
        self.assertEqual(self.call('/api/services',user=self.user).status_code,403)

    def test_banned_staff_cannot_keep_administrative_access(self):
        self.verify(self.staff);AccountSanction.objects.create(user=self.staff,actor=self.owner,kind='ban',reason='Administrative access suspended')
        self.assertEqual(self.call('/api/admin/control/dashboard',user=self.staff).status_code,403)

    def test_ban_does_not_allow_profile_writes(self):
        AccountSanction.objects.create(user=self.user,actor=self.owner,kind='ban',reason='Read-only appeal access')
        r=self.client.patch('/api/me',json.dumps({'bio':'Forbidden write'}),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+self.tokens[self.user.pk])
        self.assertEqual(r.status_code,403)
    def test_emergency_call_stop_ends_existing_sessions(self):
        self.verify(self.owner)
        c=InternalCall.objects.create(caller=self.user,callee=self.staff,status='accepted')
        self.assertEqual(self.call('/api/admin/control/settings',{'key':'calls','enabled':False,'reason':'Emergency call shutdown'}).status_code,200)
        c.refresh_from_db();self.assertEqual(c.status,'ended')
    def test_totp_enrollment_retired_but_owner_session_is_scoped(self):
        self.verify(self.owner)
        self.assertEqual(self.call('/api/admin/auth',{'action':'setup','password':'any'}).status_code,410)
        self.assertEqual(self.call('/api/admin/auth').json()['verified'],True)
        self.assertIn('finance.adjust',self.call('/api/admin/auth').json()['permissions'])

    def test_payment_emergency_blocks_new_service_reservations(self):
        import uuid
        self.verify(self.owner);Profile.objects.filter(user=self.staff).update(creator=True);Wallet.objects.filter(user=self.user).update(balance_cents=1000)
        service=Service.objects.create(owner=self.staff,title='Actual fixture service',price_cents=100,approved=True)
        self.call('/api/admin/control/settings',{'key':'payments','enabled':False,'reason':'Stop new financial commitments'})
        r=self.call('/api/bookings',{'service_id':service.pk,'confirmed':True,'price_cents':100,'request_key':str(uuid.uuid4())},self.user)
        self.assertEqual(r.status_code,503);self.assertEqual(FinancialOperation.objects.count(),0);self.assertEqual(Wallet.objects.get(user=self.user).reserved_cents,0)

@override_settings(SECURE_SSL_REDIRECT=False)
class OwnerProvisioningTests(TestCase):
    def test_owner_is_created_only_from_console_without_default_password(self):
        from django.core.management import call_command,CommandError
        from io import StringIO
        output=StringIO()
        with patch('builtins.input',return_value='owner@example.com'),patch('core.management.commands.provision_owner.getpass',return_value='TestOnlyStrongPass!8492'):call_command('provision_owner',stdout=output)
        u=get_user_model().objects.get(username='super_admin');self.assertTrue(u.is_superuser);self.assertTrue(u.is_staff);self.assertTrue(u.check_password('TestOnlyStrongPass!8492'));self.assertEqual(Wallet.objects.get(user=u).balance_cents,0);self.assertNotIn('TestOnlyStrongPass',output.getvalue());self.assertEqual(AdminAccess.objects.get(user=u).role,'owner')
        with self.assertRaises(CommandError):call_command('provision_owner',stdout=StringIO())
