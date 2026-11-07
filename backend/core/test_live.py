import json,uuid,os
from datetime import timedelta
from unittest.mock import patch
from django.test import TestCase,override_settings,TransactionTestCase,Client
from django.contrib.auth import get_user_model
from django.utils import timezone
from .models import Profile,Wallet,LiveBroadcast,LiveAdmission,FinancialOperation,Booking,Service,Withdrawal,Message
from .admin_test_support import issue_token,reviewed_payload
from . import live

@override_settings(SECURE_SSL_REDIRECT=False)
class LiveBroadcastTests(TestCase):
    def setUp(self):
        self.env=patch.dict(os.environ,{'LAYALI_LIVE_ENABLED':'1','LIVEKIT_API_KEY':'test-key','LIVEKIT_API_SECRET':'test-secret-not-production','LIVEKIT_PUBLIC_URL':'wss://testserver/livekit'});self.env.start();self.addCleanup(self.env.stop)
        self.owner=get_user_model().objects.create_user('livehost',password='test-pass');self.viewer=get_user_model().objects.create_user('liveviewer',password='test-pass');self.other=get_user_model().objects.create_user('liveother',password='test-pass');self.admin=get_user_model().objects.create_superuser('liveadmin','admin@example.com','test-pass')
        self.tokens={}
        for u in (self.owner,self.viewer,self.other,self.admin):Profile.objects.create(user=u,creator=u==self.owner);Wallet.objects.create(user=u,balance_cents=1000 if u==self.viewer else 0);self.tokens[u.pk]=issue_token(u)
        self.parts=[]
        def provider(method,payload,room=None):return {'participants':self.parts} if method=='ListParticipants' else {}
        self.provider=patch('core.live.provider',side_effect=provider);self.mock=self.provider.start();self.addCleanup(self.provider.stop)
    def request(self,path,payload=None,user=None):
        if payload is not None and path.startswith('/api/admin/') and not payload.get('reason'):payload=dict(payload,reason='Explicit live administration fixture')
        token=self.tokens[(user or self.owner).pk]
        if payload is None:return self.client.get(path,HTTP_AUTHORIZATION='Bearer '+token)
        return self.client.post(path,json.dumps(payload),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+token)
    def create(self,private=False,invite=False):
        p={'title':'Actual session','private':private,'price_cents':100,'invite_only':invite,'invite_names':[self.viewer.username] if invite else [],'max_viewers':2,'minimum_seconds':30}
        r=self.request('/api/live',p);self.assertEqual(r.status_code,201);row=LiveBroadcast.objects.get(pk=r.json()['id']);self.join(row,self.owner);self.parts=[{'identity':'host-'+str(self.owner.pk),'tracks':[{'type':'VIDEO','muted':False}]}];live.synchronize(row,True);return row
    def join(self,row,user=None):return self.request(f'/api/live/{row.pk}/admission',{'confirmed':True,'price_cents':100},user or self.viewer)
    def test_public_host_eligibility_and_no_recording_grants(self):
        Profile.objects.filter(user=self.viewer).update(broadcast_allowed=False)
        self.assertEqual(self.request('/api/live',{'title':'Denied'},self.viewer).status_code,403)
        row=self.create();r=self.join(row);self.assertEqual(r.status_code,200);self.assertEqual(FinancialOperation.objects.count(),0)
        room=self.client.post(f'/api/live/{row.pk}/room',{'ticket':r.json()['ticket']});self.assertEqual(room.status_code,200)
        self.assertEqual(self.client.post(f'/api/live/{row.pk}/room',{'ticket':r.json()['ticket']}).status_code,403)
        import re,jwt
        config=json.loads(re.search(r'<script id="config" type="application/json">(.*?)</script>',room.content.decode(),re.S).group(1));claims=jwt.decode(config['token'],'test-secret-not-production',algorithms=['HS256'])
        self.assertFalse(claims['video']['canPublish']);self.assertFalse(claims['video'].get('roomRecord',False));self.assertFalse(claims['video'].get('roomCreate',False));self.assertLessEqual(claims['exp']-claims['iat'],30)
        self.assertNotIn('MediaRecorder',room.content.decode());self.assertNotIn('test-secret-not-production',room.content.decode())
        created=next(c for c in self.mock.call_args_list if c.args[0]=='CreateRoom');self.assertNotIn('egress',created.args[1])
    def test_paid_access_is_held_once_and_actual_duration_settles_eighty_percent(self):
        row=self.create(True)
        self.assertEqual(self.request(f'/api/live/{row.pk}/admission',{},self.viewer).status_code,409)
        self.assertEqual(self.request(f'/api/live/{row.pk}/events',user=self.viewer).status_code,403)
        self.assertEqual(self.join(row).status_code,200);self.assertEqual(self.join(row).status_code,200)
        self.assertEqual(FinancialOperation.objects.count(),1);self.assertEqual(Wallet.objects.get(user=self.viewer).reserved_cents,100);self.assertEqual(Wallet.objects.get(user=self.owner).balance_cents,0)
        a=LiveAdmission.objects.get(broadcast=row,user=self.viewer);self.parts.append({'identity':str(a.pk),'tracks':[]})
        live.synchronize(row,True)
        for i in range(3):
            LiveBroadcast.objects.filter(pk=row.pk).update(last_checked_at=timezone.now()-timedelta(seconds=10));row.refresh_from_db();live.synchronize(row,True)
        op=FinancialOperation.objects.get();self.assertEqual(op.status,'settled');self.assertEqual(op.creator_cents,80);self.assertEqual(op.platform_cents,20)
        self.assertEqual(Wallet.objects.get(user=self.owner).balance_cents,80);self.assertEqual(Wallet.objects.get(user=self.viewer).balance_cents,900)
        self.assertEqual(self.join(row).status_code,200);self.assertEqual(FinancialOperation.objects.count(),1)
    def test_early_end_refunds_and_revokes_existing_token_at_gateway(self):
        row=self.create(True);r=self.join(row);a=LiveAdmission.objects.select_related('broadcast__owner','user','operation').get(broadcast=row,user=self.viewer)
        session=__import__('core.models',fromlist=['SessionToken']).SessionToken.objects.get(user=self.viewer)
        token=live.join_token(a,session);uri='/rtc?access_token='+token
        self.assertEqual(self.client.get('/api/live/authorize',HTTP_X_FORWARDED_URI=uri).status_code,204)
        self.assertEqual(self.request(f'/api/live/{row.pk}',{'action':'end'}).status_code,200)
        self.assertEqual(Wallet.objects.get(user=self.viewer).reserved_cents,0);self.assertEqual(Wallet.objects.get(user=self.viewer).balance_cents,1000)
        self.assertEqual(FinancialOperation.objects.get().status,'refunded');self.assertEqual(self.client.get('/api/live/authorize',HTTP_X_FORWARDED_URI=uri).status_code,403)
        self.assertEqual(self.request('/api/live',user=self.viewer).json()['items'],[])
    def test_invitation_ban_and_real_viewer_count(self):
        row=self.create(True,True);self.assertEqual(self.join(row,self.other).status_code,403);self.join(row)
        a=LiveAdmission.objects.select_related('broadcast__owner','user','operation').get(broadcast=row,user=self.viewer)
        self.parts.append({'identity':str(a.pk),'tracks':[]});live.synchronize(row,True);row.refresh_from_db();self.assertEqual(row.viewers,1)
        token=live.join_token(a,__import__('core.models',fromlist=['SessionToken']).SessionToken.objects.get(user=self.viewer))
        self.assertEqual(self.request(f'/api/live/{row.pk}/events',{'action':'ban','user_id':self.viewer.pk}).status_code,200)
        self.assertEqual(self.client.get('/api/live/authorize',HTTP_X_FORWARDED_URI='/rtc?access_token='+token).status_code,403)
        self.assertEqual(self.join(row).status_code,403)
    def test_service_booking_escrow_completion_and_cancel(self):
        service=Service.objects.create(owner=self.owner,title='Session',description='An actual service',price_cents=200,approved=True)
        data={'service_id':service.pk,'confirmed':True,'price_cents':200,'request_key':str(uuid.uuid4())}
        response=self.request('/api/bookings',data,self.viewer);self.assertEqual(response.status_code,201);pk=response.json()['id']
        self.assertEqual(self.request('/api/bookings',data,self.viewer).status_code,200);self.assertEqual(Booking.objects.count(),1)
        self.assertEqual(self.request(f'/api/bookings/{pk}',{'action':'accept'}).status_code,200)
        self.assertEqual(self.request(f'/api/bookings/{pk}',{'action':'complete'},self.other).status_code,404)
        self.assertEqual(self.request(f'/api/bookings/{pk}',{'action':'complete'},self.viewer).status_code,200)
        self.assertEqual(Wallet.objects.get(user=self.owner).balance_cents,160)
        data['request_key']=str(uuid.uuid4());response=self.request('/api/bookings',data,self.viewer);pk=response.json()['id']
        self.assertEqual(self.request(f'/api/bookings/{pk}',{'action':'cancel'},self.viewer).status_code,200);self.assertEqual(Wallet.objects.get(user=self.viewer).reserved_cents,0)
    def test_block_reply_delete_and_readonly_receipt(self):
        sent=self.request(f'/api/messages/{self.owner.pk}',{'body':'Original'},self.viewer).json()
        reply=self.request(f'/api/messages/{self.viewer.pk}',{'body':'Reply','reply_to':sent['id']}).json();self.assertEqual(reply['reply_to'],sent['id'])
        self.assertEqual(self.request(f'/api/messages/{sent["id"]}/action',{'action':'delete'},self.other).status_code,404)
        self.assertEqual(self.request(f'/api/messages/{sent["id"]}/action',{'action':'delete'},self.viewer).status_code,200)
        self.assertEqual(self.request('/api/blocks',{'user_id':self.owner.pk,'blocked':True},self.viewer).status_code,200)
        self.assertEqual(self.request(f'/api/messages/{self.owner.pk}',{'body':'Blocked'},self.viewer).status_code,403)
    def test_payout_requires_verified_external_reference_and_is_idempotent(self):
        wallet=Wallet.objects.get(user=self.viewer);wallet.reserved_cents=100;wallet.save()
        w=Withdrawal.objects.create(user=self.viewer,amount_cents=100,destination='test destination',request_key=uuid.uuid4())
        p={'id':str(w.pk),'action':'paid','payout_reference':'external-test-ref'}
        self.assertEqual(self.request('/api/admin/payouts',p,self.admin).status_code,409);p['paid_verified']=True;p['reason']='Explicit live administration fixture';p=reviewed_payload(self.admin,'/api/admin/payouts',p,self.viewer.pk)
        self.assertEqual(self.request('/api/admin/payouts',p,self.admin).status_code,200);self.assertEqual(self.request('/api/admin/payouts',p,self.admin).status_code,200)
        self.assertEqual(Wallet.objects.get(user=self.viewer).balance_cents,900)

    def test_live_moderator_cannot_access_account_money(self):
        from django.contrib.auth.models import Permission
        self.other.is_staff=True;self.other.save();self.other.user_permissions.add(Permission.objects.get(codename='moderate_livebroadcast',content_type__app_label='core'))
        from .models import SessionToken
        SessionToken.objects.filter(user=self.other).update(admin_verified_at=timezone.now())
        self.assertEqual(self.request('/api/admin/live',user=self.other).status_code,200)
        self.assertEqual(self.request('/api/admin/accounts',user=self.other).status_code,403)
        self.assertEqual(self.request('/api/admin/payouts',user=self.other).status_code,403)
        row=self.create(True,True)
        self.assertEqual(self.join(row,self.other).status_code,200)
        self.assertEqual(FinancialOperation.objects.count(),0)

    def test_failed_host_and_unconnected_paid_admission_refund(self):
        row=self.create(True);self.join(row)
        self.parts=[];LiveBroadcast.objects.filter(pk=row.pk).update(host_missing_at=timezone.now()-timedelta(seconds=46));row.refresh_from_db();live.synchronize(row,True)
        row.refresh_from_db();self.assertEqual(row.status,'failed');self.assertEqual(FinancialOperation.objects.get().status,'refunded')

@override_settings(SECURE_SSL_REDIRECT=False)
class LivePaymentConcurrencyTests(TransactionTestCase):
    def test_two_joins_reserve_one_entry_and_no_recording_services_configured(self):
        from django.db import connection,close_old_connections,connections
        from concurrent.futures import ThreadPoolExecutor
        if connection.vendor!='postgresql':self.skipTest('Row locking requires PostgreSQL')
        owner=get_user_model().objects.create_user('concurrent_live_owner',password='test');viewer=get_user_model().objects.create_user('concurrent_live_viewer',password='test')
        Profile.objects.create(user=owner,creator=True);Profile.objects.create(user=viewer);Wallet.objects.create(user=owner);Wallet.objects.create(user=viewer,balance_cents=100)
        token=issue_token(viewer);broadcast=LiveBroadcast.objects.create(owner=owner,title='Concurrent test',private=True,price_cents=100,status='active',started_at=timezone.now(),last_checked_at=timezone.now())
        from .models import SessionToken
        issue_token(owner)
        LiveAdmission.objects.create(broadcast=broadcast,user=owner,session=SessionToken.objects.get(user=owner))
        parts={'participants':[{'identity':'host-'+str(owner.pk),'tracks':[{'type':'VIDEO'}]}]}
        def join(_):
            close_old_connections()
            try:return Client().post(f'/api/live/{broadcast.pk}/admission',json.dumps({'confirmed':True,'price_cents':100}),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+token).status_code
            finally:connections.close_all()
        with patch('core.live.provider',return_value=parts),ThreadPoolExecutor(max_workers=2) as pool:results=list(pool.map(join,range(2)))
        self.assertEqual(results,[200,200]);self.assertEqual(FinancialOperation.objects.count(),1);self.assertEqual(Wallet.objects.get(user=viewer).reserved_cents,100)
