import uuid,json,os
from datetime import timedelta
from django.test import TestCase,TransactionTestCase,override_settings
from django.utils import timezone
from django.contrib.auth import get_user_model
from . import test_live as fixtures,live
from .models import *
from .views import issue_token

@override_settings(SECURE_SSL_REDIRECT=False)
class StageTests(TestCase):
    setUp=fixtures.LiveBroadcastTests.setUp
    request=fixtures.LiveBroadcastTests.request
    create=fixtures.LiveBroadcastTests.create
    join=fixtures.LiveBroadcastTests.join
    def event(self,row,p,user=None):return self.request(f'/api/live/{row.pk}/events',p,user)
    def stage(self,limit=2):
        row=self.create();row.max_speakers=limit;row.gifts_enabled=True;row.save();self.join(row,self.viewer);self.join(row,self.other);return row
    def test_media_diagnostics_are_authorized_bounded_and_do_not_accept_raw_errors(self):
        row=self.stage()
        payload={'action':'media_diagnostic','stage':'camera_capture','outcome':'failed','reason':'NotAllowedError','build':'media-1'}
        with self.assertLogs('relaxia.live.media',level='WARNING') as logs:
            response=self.event(row,payload,self.viewer)
        self.assertEqual(response.status_code,200)
        self.assertEqual(response.json(),{'accepted':True})
        self.assertIn('camera_capture',logs.output[0])
        self.assertEqual(self.event(row,{**payload,'reason':'secret-token-or-device-data'},self.viewer).status_code,400)
        self.assertEqual(self.event(row,{**payload,'stage':'arbitrary-command'},self.viewer).status_code,400)
        self.assertEqual(self.client.post(f'/api/live/{row.pk}/control',data=json.dumps(payload),content_type='application/json').status_code,403)

    def test_audio_room_has_visible_host_before_javascript_or_connection(self):
        row=self.create();row.mode='audio';row.save()
        admitted=self.join(row,self.owner).json()
        page=self.client.post(f'/api/live/{row.pk}/room',{'ticket':admitted['ticket']})
        self.assertEqual(page.status_code,200)
        self.assertContains(page,'<body class="audio-mode">')
        self.assertContains(page,'<div id="audioStage">')
        self.assertContains(page,'aria-label="البث المباشر" hidden')
        self.assertContains(page,'جارٍ تجهيز الصوت…')
        self.assertContains(page,'window.relaxiaStageLoaded')
        self.assertContains(page,'v=rooms-controls-26')

    def test_audio_host_sources_and_guest_consent_permissions(self):
        row=self.stage();row.mode='audio';row.save()
        a=LiveAdmission.objects.select_related('broadcast','user').get(broadcast=row,user=self.viewer)
        token=live.join_token(a,a.session);claims=__import__('jwt').decode(token,os.environ['LIVEKIT_API_SECRET'],algorithms=['HS256']);self.assertFalse(claims['video']['canPublish'])
        self.assertEqual(self.event(row,{'action':'seat_request','mode':'video'},self.viewer).status_code,400)
        self.assertEqual(self.event(row,{'action':'seat_request','mode':'audio'},self.viewer).status_code,200)
        self.assertEqual(self.event(row,{'action':'seat_decision','user_id':self.viewer.pk,'approve':True},self.other).status_code,403)
        self.assertEqual(self.event(row,{'action':'seat_decision','user_id':self.viewer.pk,'approve':True}).status_code,200)
        a.refresh_from_db();token=live.join_token(a,a.session);claims=__import__('jwt').decode(token,os.environ['LIVEKIT_API_SECRET'],algorithms=['HS256'])
        self.assertEqual(claims['video']['canPublishSources'],['microphone']);self.assertTrue(claims['video']['canSubscribe'])
        self.assertEqual(self.client.get('/api/live/authorize',HTTP_X_FORWARDED_URI='/rtc?access_token='+token).status_code,204)
        self.event(row,{'action':'seat_decision','user_id':self.viewer.pk,'approve':False})
        self.assertEqual(self.client.get('/api/live/authorize',HTTP_X_FORWARDED_URI='/rtc?access_token='+token).status_code,403)
    def test_stage_capacity_cannot_be_bypassed(self):
        row=self.stage()
        for u in [self.viewer,self.other]:self.assertEqual(self.event(row,{'action':'seat_request','mode':'video'},u).status_code,200)
        self.assertEqual(self.event(row,{'action':'seat_decision','user_id':self.viewer.pk,'approve':True}).status_code,200)
        self.assertEqual(self.event(row,{'action':'seat_decision','user_id':self.other.pk,'approve':True}).status_code,409)
        self.assertEqual(LiveSeat.objects.filter(status='approved').count(),1)
    def test_pk_counts_settled_gifts_once_and_credits_eighty_percent(self):
        row=self.stage();self.event(row,{'action':'seat_request','mode':'audio'},self.other);self.event(row,{'action':'seat_decision','user_id':self.other.pk,'approve':True})
        LiveAdmission.objects.filter(broadcast=row,user=self.other).update(connected=True)
        self.parts.append({'identity':str(LiveAdmission.objects.get(broadcast=row,user=self.other).pk),'tracks':[{'type':'AUDIO','muted':False}]})
        teams=[[self.owner.pk],[self.other.pk]]
        self.assertEqual(self.event(row,{'action':'pk_start','teams':teams,'duration':60}).status_code,200)
        self.assertEqual(self.event(row,{'action':'pk_start','teams':teams,'duration':60}).status_code,409)
        data={'action':'gift','gift_code':'rose','price_cents':100,'confirmed':True,'request_key':str(uuid.uuid4()),'recipient_id':self.other.pk}
        for _ in range(2):self.assertEqual(self.event(row,data,self.viewer).status_code,200)
        self.assertEqual(LiveGiftEvent.objects.count(),1);self.assertEqual(Wallet.objects.get(user=self.viewer).balance_cents,900);self.assertEqual(Wallet.objects.get(user=self.other).balance_cents,80)
        result=self.request(f'/api/live/{row.pk}/events').json();self.assertEqual(result['pk']['scores'],[0,100]);self.assertEqual(result['top_supporters'][0]['total'],100)
        LivePK.objects.filter(broadcast=row).update(ends_at=timezone.now()-timedelta(seconds=1))
        data['request_key']=str(uuid.uuid4());self.assertEqual(self.event(row,data,self.viewer).status_code,200)
        self.assertEqual(self.request(f'/api/live/{row.pk}/events').json()['pk']['scores'],[0,100])
    def test_gift_price_tampering_self_gifting_and_disabled_gifts_are_rejected(self):
        row=self.stage();data={'action':'gift','gift_code':'star','price_cents':1,'confirmed':True,'request_key':str(uuid.uuid4())}
        self.assertEqual(self.event(row,data,self.viewer).status_code,409)
        data.update(gift_code='rose',price_cents=100,recipient_id=self.viewer.pk)
        self.assertEqual(self.event(row,data,self.viewer).status_code,400)
        data['recipient_id']=self.owner.pk;LiveGiftDefinition.objects.filter(pk='rose').update(enabled=False)
        self.assertEqual(self.event(row,data,self.viewer).status_code,409);self.assertEqual(FinancialOperation.objects.filter(kind='live_gift').count(),0)
    def test_room_moderator_can_pin_but_not_start_pk_or_change_catalog(self):
        row=self.stage();self.assertEqual(self.event(row,{'action':'set_moderator','user_id':self.viewer.pk,'enabled':True}).status_code,200)
        self.event(row,{'action':'comment','text':'رسالة حقيقية للاختبار'},self.other);c=LiveComment.objects.get(broadcast=row)
        self.assertEqual(self.event(row,{'action':'pin_comment','comment_id':c.pk},self.viewer).status_code,200)
        self.assertEqual(self.request(f'/api/live/{row.pk}/events').json()['pinned_comment']['id'],c.pk)
        self.assertEqual(self.event(row,{'action':'pk_start','teams':[[self.owner.pk],[self.viewer.pk]],'duration':60},self.viewer).status_code,403)
        self.assertEqual(self.request('/api/admin/live/gifts',{'code':'rose'},self.viewer).status_code,403)
        live.close(row);self.assertFalse(LiveSeat.objects.filter(admission__broadcast=row).exists());self.assertFalse(LiveComment.objects.filter(broadcast=row).exists());self.assertFalse(LiveRoomModerator.objects.filter(broadcast=row).exists())
    def test_signed_room_controls_work_without_exposing_bearer_tokens(self):
        import re
        row=self.stage();admitted=self.join(row,self.viewer).json()
        page=self.client.post(f'/api/live/{row.pk}/room',{'ticket':admitted['ticket']})
        config=json.loads(re.search(r'<script id="config" type="application/json">(.*?)</script>',page.content.decode(),re.S).group(1))
        self.assertEqual(config['ownerPublicId'], self.owner.profile.public_id)
        self.assertRegex(config['ownerPublicId'], r'^[1-9][0-9]{4}$')
        self.assertIn("style-src 'self'",page['Content-Security-Policy'])
        path=f'/api/live/{row.pk}/control'
        headers={'HTTP_X_LIVE_CONTROL':config['control']}
        self.assertEqual(self.client.get(path,**headers).status_code,200)
        r=self.client.post(path,json.dumps({'action':'seat_request','mode':'audio'}),content_type='application/json',**headers)
        self.assertEqual(r.status_code,200);self.assertEqual(r.json()['seat_status'],'pending')
        self.assertEqual(self.client.get(path,HTTP_X_LIVE_CONTROL='forged').status_code,403)
        self.assertEqual(self.client.post(path,json.dumps({'action':'end'}),content_type='application/json',**headers).status_code,403)

    def test_creation_rejects_more_than_twelve_speakers_and_invalid_modes(self):
        for payload in [{'mode':'video','max_speakers':13},{'mode':'fake'},{'mode':'audio','max_speakers':True}]:
            r=self.request('/api/live',dict(title='اختبار البث',**payload));self.assertEqual(r.status_code,400)

    def test_audio_rooms_use_eight_seats_even_from_legacy_clients(self):
        payload={'title':'غرفة صوتية','mode':'audio','max_speakers':15,'max_viewers':25}
        response=self.request('/api/live',payload)
        self.assertEqual(response.status_code,201)
        self.assertEqual(response.json()['max_speakers'],8)
        self.assertEqual(LiveBroadcast.objects.get(pk=response.json()['id']).max_speakers,8)
        live.close(LiveBroadcast.objects.get(pk=response.json()['id']))
        for mode,seats in [('audio',16),('video',15)]:
            self.assertEqual(self.request('/api/live',dict(payload,mode=mode,max_speakers=seats)).status_code,400)

    def test_paid_audio_reentry_reserves_once_and_requires_price_confirmation(self):
        response=self.request('/api/live',{'title':'غرفة مدفوعة','mode':'audio','max_speakers':15,'max_viewers':25,'private':True,'price_cents':100})
        self.assertEqual(response.status_code,201)
        row=LiveBroadcast.objects.get(pk=response.json()['id']);self.join(row,self.owner)
        self.parts=[{'identity':'host-'+str(self.owner.pk),'tracks':[{'type':'AUDIO','muted':False}]}]
        live.synchronize(row,True)
        path=f'/api/live/{row.pk}/admission'
        self.assertEqual(self.request(path,{'confirmed':False,'price_cents':100},self.viewer).status_code,409)
        for _ in range(2):self.assertEqual(self.join(row,self.viewer).status_code,200)
        self.assertEqual(Wallet.objects.get(user=self.viewer).balance_cents,1000)
        self.assertEqual(Wallet.objects.get(user=self.viewer).reserved_cents,100)
        self.assertEqual(FinancialOperation.objects.filter(buyer=self.viewer).count(),1)

    def test_public_luxury_store_has_exactly_21_webp_items_sorted_by_price(self):
        from .live_stage import LUXURY_GIFT_CODES
        row=self.stage()
        catalog=self.request(f'/api/live/{row.pk}/events').json()['gift_catalog']
        self.assertEqual(len(catalog),21)
        self.assertEqual({g['code'] for g in catalog},set(LUXURY_GIFT_CODES))
        self.assertEqual([g['price_cents'] for g in catalog],sorted(g['price_cents'] for g in catalog))
        self.assertEqual({g['category'] for g in catalog},{'simple','animated','luxury'})
        for gift in catalog:
            self.assertEqual(gift['icon_url'],f"/static/core/gifts/icons/{gift['code']}.webp")
            self.assertEqual(gift['animation_url'],f"/static/core/gifts/animations/{gift['code']}.webp")

    def test_luxury_gift_catalog_has_server_owned_prices_and_tiers(self):
        for code, name, price, effect in [
            ('falcon_gold','صقر الصحراء الذهبي',5000,'depth'),
            ('royal_yacht','اليخت الملكي',3500,'depth'),
            ('cyber_supercar','السيارة الخارقة',2500,'depth'),
            ('emperor_crown','تاج الإمبراطور',800,'animated'),
            ('knight_sword','سيف الفارس المضيء',500,'animated'),
            ('meteor_shower','وابل النيازك',100,'simple'),
            ('crystal_hearts','قلوب كريستالية',10,'simple'),
        ]:
            gift=LiveGiftDefinition.objects.get(pk=code)
            self.assertEqual((gift.name,gift.price_cents,gift.effect, gift.enabled),(name,price,effect,True))

    def test_combo_gift_settles_once_at_catalog_price_and_shows_correct_multiplier(self):
        row=self.stage()
        Wallet.objects.filter(user=self.viewer).update(balance_cents=60000)
        key=str(uuid.uuid4())
        data={'action':'gift','gift_code':'falcon_gold','price_cents':5000,
              'recipient_id':self.owner.pk,'combo_count':10,'total_cost':1,
              'confirmed':True,'request_key':key}
        for _ in range(2):
            self.assertEqual(self.event(row,data,self.viewer).status_code,200)
        self.assertEqual(Wallet.objects.get(user=self.viewer).balance_cents,10000)
        self.assertEqual(Wallet.objects.get(user=self.owner).balance_cents,40000)
        self.assertEqual(FinancialOperation.objects.filter(kind='live_gift').count(),1)
        gift=LiveGiftEvent.objects.get()
        self.assertEqual(gift.combo_count,10)
        self.assertEqual(gift.operation.amount_cents,50000)
        event=self.request(f'/api/live/{row.pk}/events').json()['gift_events'][0]
        self.assertEqual(event['combo_count'],10)
        self.assertEqual(event['total_price_cents'],50000)
        self.assertEqual(event['code'],'falcon_gold')
        self.assertEqual(self.request(f'/api/live/{row.pk}/events',None,self.viewer).json()['available_coins'],10000)
        changed={**data,'combo_count':1}
        self.assertEqual(self.event(row,changed,self.viewer).status_code,409)
        self.assertEqual(Wallet.objects.get(user=self.viewer).balance_cents,10000)

    def test_maximum_combo_99_settles_atomically(self):
        row=self.stage()
        key=str(uuid.uuid4())
        data={'action':'gift','gift_code':'crystal_hearts','price_cents':10,
              'combo_count':99,'confirmed':True,'request_key':key}
        self.assertEqual(self.event(row,data,self.viewer).status_code,200)
        self.assertEqual(self.event(row,data,self.viewer).status_code,200)
        self.assertEqual(Wallet.objects.get(user=self.viewer).balance_cents,10)
        self.assertEqual(FinancialOperation.objects.filter(kind='live_gift').count(),1)
        self.assertEqual(LiveGiftEvent.objects.get().combo_count,99)
        self.assertEqual(LiveGiftEvent.objects.get().operation.amount_cents,990)

    def test_combo_rejects_invalid_counts_without_creating_financial_operation(self):
        row=self.stage()
        base={'action':'gift','gift_code':'crystal_hearts','price_cents':10,
              'recipient_id':self.owner.pk,'confirmed':True,'request_key':str(uuid.uuid4())}
        for count in (0,100,999,-1,True,'10',1.5):
            response=self.event(row,{**base,'combo_count':count},self.viewer)
            self.assertEqual(response.status_code,400)
        self.assertEqual(FinancialOperation.objects.filter(kind='live_gift').count(),0)

    def test_whale_and_lion_catalog_gifts_are_settled_once_at_server_prices(self):
        self.assertGreaterEqual(LiveGiftDefinition.objects.filter(enabled=True).count(),24)
        row=self.stage();Wallet.objects.filter(user=self.viewer).update(balance_cents=50000)
        for code,price in [('whale',10000),('lion',15000)]:
            payload={'action':'gift','gift_code':code,'price_cents':price,'confirmed':True,'request_key':str(uuid.uuid4())}
            for _ in range(2):self.assertEqual(self.event(row,payload,self.viewer).status_code,200)
        self.assertEqual(LiveGiftEvent.objects.count(),2)
        self.assertEqual(Wallet.objects.get(user=self.viewer).balance_cents,25000)
        self.assertEqual(Wallet.objects.get(user=self.owner).balance_cents,20000)
        result=self.request(f'/api/live/{row.pk}/events').json()
        self.assertEqual({g['code'] for g in result['gift_events']},{'lion','whale'})

@override_settings(SECURE_SSL_REDIRECT=False)
class StageConcurrencyTests(TransactionTestCase):
    setUp=fixtures.LiveBroadcastTests.setUp
    request=fixtures.LiveBroadcastTests.request
    create=fixtures.LiveBroadcastTests.create
    join=fixtures.LiveBroadcastTests.join
    stage=StageTests.stage
    event=StageTests.event
    def race(self,callback):
        from django.db import connection,connections,close_old_connections
        if connection.vendor!='postgresql':self.skipTest('Requires PostgreSQL row locking')
        from concurrent.futures import ThreadPoolExecutor
        from threading import Barrier
        barrier=Barrier(2)
        def run(i):
            close_old_connections()
            try:barrier.wait(timeout=10);return callback(i)
            finally:connections.close_all()
        with ThreadPoolExecutor(max_workers=2) as pool:return list(pool.map(run,[0,1]))
    def test_two_approvals_cannot_overfill_one_guest_seat(self):
        row=self.stage()
        for u in (self.viewer,self.other):self.event(row,{'action':'seat_request','mode':'audio'},u)
        from django.test import Client
        def approve(i):return Client().post(f'/api/live/{row.pk}/events',json.dumps({'action':'seat_decision','user_id':[self.viewer.pk,self.other.pk][i],'approve':True}),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+self.tokens[self.owner.pk]).status_code
        self.assertEqual(sorted(self.race(approve)),[200,409]);self.assertEqual(LiveSeat.objects.filter(status='approved').count(),1)
    def test_retried_gift_is_one_debit_and_one_pk_event(self):
        # TransactionTestCase flush removes migration-seeded catalog rows.
        LiveGiftDefinition.objects.get_or_create(code='rose',defaults={'name':'وردة','symbol':'🌹','price_cents':100,'effect':'simple'})
        row=self.stage();data={'action':'gift','gift_code':'rose','price_cents':100,'confirmed':True,'request_key':str(uuid.uuid4())}
        from django.test import Client
        def send(_):return Client().post(f'/api/live/{row.pk}/events',json.dumps(data),content_type='application/json',HTTP_AUTHORIZATION='Bearer '+self.tokens[self.viewer.pk]).status_code
        self.assertEqual(self.race(send),[200,200]);self.assertEqual(LiveGiftEvent.objects.count(),1);self.assertEqual(Wallet.objects.get(user=self.viewer).balance_cents,900);self.assertEqual(Wallet.objects.get(user=self.owner).balance_cents,80)
