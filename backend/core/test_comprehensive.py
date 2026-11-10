import base64,hashlib,io,json,uuid
from unittest.mock import patch
from datetime import timedelta
from django.test import TestCase,override_settings
from django.contrib.auth import get_user_model
from django.utils import timezone
from django.core import mail
from PIL import Image
from .models import Profile,Wallet,Message,Reel,ReelPreference,UserBlock,PushDevice,PushDelivery,ExperienceAward,Withdrawal,UploadedMedia,SessionToken
from .views import issue_token

@override_settings(SECURE_SSL_REDIRECT=False,PASSWORD_HASHERS=['django.contrib.auth.hashers.MD5PasswordHasher'])
class ComprehensiveTests(TestCase):
    def setUp(self):
        self.user=get_user_model().objects.create_user('member','member@example.com','Password!875')
        self.peer=get_user_model().objects.create_user('peer','peer@example.com','Password!875')
        for u in (self.user,self.peer):Profile.objects.create(user=u);Wallet.objects.create(user=u)
        self.token=issue_token(self.user)
    def post(self,path,data,token=None):return self.client.post(path,json.dumps(data),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+(token or self.token))
    def get(self,path):return self.client.get(path,HTTP_AUTHORIZATION='Bearer '+self.token)
    def test_message_pages_reach_oldest_without_duplicates(self):
        Message.objects.bulk_create([Message(sender=self.peer,recipient=self.user,body=str(i)) for i in range(205)])
        ids=[];cursor='';pages=0
        while True:
            result=self.get(f'/api/messages/{self.peer.pk}?before={cursor}').json();ids += [m['id'] for m in result['items']];pages+=1
            cursor=result['next_cursor']
            if not cursor:break
        self.assertEqual(len(ids),205);self.assertEqual(len(set(ids)),205);self.assertEqual(pages,3)
    def test_reels_search_block_hide_save_and_old_pages(self):
        Reel.objects.bulk_create([Reel(owner=self.peer,approved=True,video_url='https://example.org/a',caption='travel') for _ in range(305)])
        first=self.get('/api/reels?q=travel').json();self.assertEqual(len(first['items']),100)
        cursor=first['next_cursor'];ids={x['id'] for x in first['items']}
        while cursor:
            page=self.get('/api/reels?before='+cursor).json();ids.update(x['id'] for x in page['items']);cursor=page['next_cursor']
        self.assertEqual(len(ids),305)
        target=first['items'][0]['id']
        self.assertEqual(self.post(f'/api/reels/{target}/action',{'action':'saved','enabled':True}).status_code,200)
        self.assertEqual(len(self.get('/api/reels?saved=1').json()['items']),1)
        self.post(f'/api/reels/{target}/action',{'action':'hidden','enabled':True})
        self.assertEqual(self.get('/api/reels?reel_id='+str(target)).json()['items'],[])
        UserBlock.objects.create(user=self.peer,blocked=self.user)
        self.assertEqual(self.get('/api/reels').json()['items'],[])
    def test_new_frames_and_permanent_capped_awards(self):
        from .profile_levels import profile_progress
        from .activity_events import award
        self.assertEqual(profile_progress(self.user)['owned_frames'],[0])
        award(self.user.pk,'booking:a',80);award(self.user.pk,'booking:a',80)
        award(self.user.pk,'booking:b',80);award(self.user.pk,'booking:c',80)
        self.assertEqual(profile_progress(self.user)['xp_points'],200)
        self.assertNotIn(2,profile_progress(self.user)['owned_frames'])
        Reel.objects.create(owner=self.user,approved=True,video_url='https://example.org/a').delete()
        self.assertEqual(profile_progress(self.user)['xp_points'],200)
    def test_push_is_account_bound_and_deduplicated(self):
        from .push import enqueue,send_one
        device=PushDevice.objects.create(user=self.user,token='a'*40)
        enqueue(self.user.pk,'one','messages',self.peer.pk,'new');enqueue(self.user.pk,'one','messages',self.peer.pk,'new')
        self.assertEqual(PushDelivery.objects.count(),1)
        device.user=self.peer;device.save()
        with patch.dict('os.environ',{'FCM_PROJECT_ID':'test'}):self.assertTrue(send_one())
        self.assertTrue(PushDelivery.objects.get().failed)
    def test_push_rolls_back_with_message(self):
        from django.db import transaction
        PushDevice.objects.create(user=self.user,token='b'*40)
        try:
            with transaction.atomic():
                Message.objects.create(sender=self.peer,recipient=self.user,body='secret');raise RuntimeError()
        except RuntimeError:pass
        self.assertEqual(PushDelivery.objects.count(),0)
    def test_resumable_upload_retries_and_ownership(self):
        output=io.BytesIO();Image.new('RGB',(20,20),'red').save(output,'PNG');raw=output.getvalue()
        payload={'purpose':'avatar','size':len(raw),'sha256':hashlib.sha256(raw).hexdigest()}
        uid=self.post('/api/uploads',payload).json()['id'];chunk={'offset':0,'data':base64.b64encode(raw).decode()}
        path=f'/api/uploads/{uid}/chunk'
        self.assertEqual(self.post(path,chunk).status_code,200);self.assertEqual(self.post(path,chunk).status_code,200)
        self.assertEqual(self.post(path,chunk,issue_token(self.peer)).status_code,404)
        result=self.post(f'/api/uploads/{uid}/finish',{});self.assertEqual(result.status_code,201)
        again=self.post(f'/api/uploads/{uid}/finish',{});self.assertEqual(result.json()['id'],again.json()['id']);self.assertEqual(UploadedMedia.objects.count(),1)
    @override_settings(EMAIL_HOST='smtp.example.org',EMAIL_BACKEND='django.core.mail.backends.locmem.EmailBackend',DEFAULT_FROM_EMAIL='support@example.org')
    def test_reset_is_single_use_and_revokes_sessions(self):
        self.assertEqual(self.post('/api/account/reset/request',{'email':self.user.email}).status_code,200)
        code=mail.outbox[0].body.split('\n\n')[1]
        self.assertEqual(self.post('/api/account/reset/confirm',{'code':code,'password':'NewPassword!938'}).status_code,200)
        self.assertFalse(SessionToken.objects.filter(user=self.user).exists())
        self.assertEqual(self.post('/api/account/reset/confirm',{'code':code,'password':'NewPassword!948'}).status_code,400)
    def test_old_receipt_and_cross_account_access(self):
        first=None
        for _ in range(160):
            r=Withdrawal.objects.create(user=self.user,amount_cents=100,destination='test',request_key=uuid.uuid4())
            if first is None:first=r
        self.assertEqual(self.get('/api/receipts/withdrawal:'+str(first.pk)).status_code,200)
        self.token=issue_token(self.peer)
        self.assertEqual(self.get('/api/receipts/withdrawal:'+str(first.pk)).status_code,404)
    def test_account_deletion_preserves_financial_records(self):
        Wallet.objects.filter(user=self.user).update(balance_cents=100)
        payload={'action':'delete','current_password':'Password!875'}
        self.assertEqual(self.post('/api/account/security',payload).status_code,409)
        Wallet.objects.filter(user=self.user).update(balance_cents=0)
        self.assertEqual(self.post('/api/account/security',payload).status_code,200)
        self.user.refresh_from_db();self.assertFalse(self.user.is_active);self.assertTrue(Wallet.objects.filter(user=self.user).exists())
    def test_email_change_requires_password(self):
        response=self.client.patch('/api/me',json.dumps({'email':'changed@example.com'}),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+self.token)
        self.assertEqual(response.status_code,403)
    def test_hidden_reel_analytics_do_not_credit_money_or_xp(self):
        reel=Reel.objects.create(owner=self.peer,approved=True,video_url='https://example.org/v')
        path=f'/api/reels/{reel.pk}/action'
        self.post(path,{'action':'watch','seconds':20});self.post(path,{'action':'watch','seconds':10})
        self.assertEqual(ReelPreference.objects.get().watch_seconds,20)
        self.assertFalse(ExperienceAward.objects.exists());self.assertEqual(Wallet.objects.get(user=self.peer).balance_cents,0)
    def test_profile_requests_cannot_enable_legacy_frames(self):
        response=self.client.patch('/api/me',json.dumps({'legacy_frames':True}),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+self.token)
        self.assertEqual(response.status_code,400)
    def test_resumable_upload_obeys_store_maintenance(self):
        from .models import AdminFeature
        AdminFeature.objects.create(key='store',enabled=False)
        payload={'purpose':'reel','size':16,'sha256':'a'*64}
        self.assertEqual(self.post('/api/uploads',payload).status_code,503)
    def test_same_file_can_be_uploaded_for_distinct_messages(self):
        payload={'purpose':'chat_image','size':100,'sha256':'b'*64,'request_key':str(uuid.uuid4())}
        first=self.post('/api/uploads',payload).json()['id']
        self.assertEqual(self.post('/api/uploads',payload).json()['id'],first)
        payload['request_key']=str(uuid.uuid4())
        self.assertNotEqual(self.post('/api/uploads',payload).json()['id'],first)
    def test_resume_key_cannot_change_content(self):
        payload={'purpose':'avatar','size':100,'sha256':'b'*64,'request_key':str(uuid.uuid4())}
        self.post('/api/uploads',payload)
        payload['sha256']='c'*64
        self.assertEqual(self.post('/api/uploads',payload).status_code,409)

    def test_milestone_frames_require_level_even_for_legacy_accounts(self):
        from .profile_levels import profile_progress
        self.user.profile.legacy_frames=True;self.user.profile.save()
        self.assertNotIn(110,profile_progress(self.user)['owned_frames'])
        url='/api/me'
        def equip(frame):
            return self.client.patch(url,json.dumps({'selected_frame':frame}),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+self.token)
        self.assertEqual(equip(110).status_code,403)
        ExperienceAward.objects.create(user=self.user,source='milestone-test',points=1800)
        self.assertIn(110,profile_progress(self.user)['owned_frames'])
        self.assertNotIn(120,profile_progress(self.user)['owned_frames'])
        self.assertEqual(equip(110).status_code,200)
        self.assertEqual(equip(111).status_code,400)
        ExperienceAward.objects.create(user=self.user,source='milestone-max',points=18000)
        self.assertEqual(equip(200).status_code,200)

    def test_repeated_double_tap_like_does_not_toggle_or_duplicate(self):
        reel=Reel.objects.create(owner=self.peer,approved=True,video_url='https://example.org/reel')
        for _ in range(3):
            response=self.post(f'/api/reels/{reel.pk}/like',{'enabled':True})
            self.assertTrue(response.json()['liked'])
            self.assertEqual(response.json()['like_count'],1)

    def test_purchase_submission_retry_keeps_one_pending_order(self):
        from .models import TopUp
        quote=self.post('/api/topup-quotes',{'method':'usdt','amount_cents':100}).json()
        receipt=UploadedMedia.objects.create(owner=self.user,purpose='receipt',file='test-receipt.png',mime='image/png',size=1)
        payload={'quote_id':quote['id'],'receipt_id':str(receipt.pk),'transaction_hash':'a'*64}
        first=self.post('/api/topups',payload)
        self.assertEqual(first.status_code,201)
        again=self.post('/api/topups',payload)
        self.assertEqual(again.status_code,200)
        self.assertEqual(first.json()['id'],again.json()['id'])
        payload['transaction_hash']='b'*64
        self.assertEqual(self.post('/api/topups',payload).status_code,409)
        self.assertEqual(TopUp.objects.count(),1)
        self.assertEqual(self.get('/api/topups').json()['items'][0]['status'],'pending')
