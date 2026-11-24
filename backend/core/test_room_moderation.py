"""Regression tests: client flags are untrusted; server owns all moderation and money."""
import json,uuid
from datetime import timedelta
from django.test import TestCase,override_settings
from django.utils import timezone
from . import test_live as fixture
from .models import (
    LiveBroadcast,LiveAdmission,LiveSeat,LiveRoomModerator,LiveComment,
    LiveChatRestriction,LiveHostBan,LiveReport,SessionToken,AccountModerationState,
    Wallet,FinancialOperation
)

@override_settings(SECURE_SSL_REDIRECT=False)
class RoomModerationTests(TestCase):
    setUp=fixture.LiveBroadcastTests.setUp
    request=fixture.LiveBroadcastTests.request
    create=fixture.LiveBroadcastTests.create
    join=fixture.LiveBroadcastTests.join

    def event(self,row,p,user=None):return self.request(f'/api/live/{row.pk}/events',p,user)

    def test_nonmanager_cannot_restrict_or_kick_and_admin_is_protected(self):
        row=self.create();self.join(row,self.viewer);self.join(row,self.other)
        denied={'action':'kick_user','user_id':self.other.pk}
        self.assertEqual(self.event(row,denied,self.viewer).status_code,403)
        self.assertEqual(self.event(row,{**denied,'user_id':self.admin.pk}).status_code,403)
        self.assertEqual(self.event(row,{'action':'ban_from_host_rooms','user_id':self.admin.pk}).status_code,403)
        self.assertEqual(LiveHostBan.objects.count(),0)

    def test_moderator_controls_room_but_cannot_change_host_options(self):
        row=self.create();self.join(row,self.viewer);self.join(row,self.other)
        self.assertEqual(self.event(row,{'action':'set_room_moderator','user_id':self.viewer.pk,'enabled':True}).status_code,200)
        self.assertTrue(LiveRoomModerator.objects.filter(broadcast=row,user=self.viewer).exists())
        self.assertEqual(self.event(row,{'action':'room_settings','settings':{'chat_enabled':False}},self.viewer).status_code,403)
        self.assertEqual(self.event(row,{'action':'kick_user','user_id':self.other.pk},self.viewer).status_code,200)
        self.assertEqual(self.join(row,self.other).status_code,403)
        self.assertEqual(self.join(row,self.viewer).status_code,200)

    def test_chat_muting_is_room_scoped_and_expires(self):
        row=self.create();self.join(row,self.viewer)
        self.assertEqual(self.event(row,{'action':'mute_chat','user_id':self.viewer.pk,'duration':'10m'}).status_code,200)
        self.assertEqual(self.event(row,{'action':'comment','text':'hello'},self.viewer).status_code,403)
        LiveChatRestriction.objects.filter(broadcast=row,user=self.viewer).update(muted_until=timezone.now()-timedelta(seconds=1))
        self.assertEqual(self.event(row,{'action':'comment','text':'allowed now'},self.viewer).status_code,200)
        self.assertEqual(self.event(row,{'action':'mute_chat','user_id':self.viewer.pk,'duration':'stream'}).status_code,200)
        self.assertEqual(self.event(row,{'action':'comment','text':'blocked'},self.viewer).status_code,403)
        self.assertEqual(self.event(row,{'action':'unmute_chat','user_id':self.viewer.pk}).status_code,200)

    def test_slow_mode_links_and_disabled_chat_enforced_by_server(self):
        row=self.create();self.join(row,self.viewer)
        self.assertEqual(self.event(row,{'action':'room_settings','settings':{'chat_slow_seconds':5,'block_external_links':True}}).status_code,200)
        self.assertEqual(self.event(row,{'action':'comment','text':'https://spam.example.com'},self.viewer).status_code,403)
        self.assertEqual(self.event(row,{'action':'comment','text':'first'},self.viewer).status_code,200)
        self.assertEqual(self.event(row,{'action':'comment','text':'second'},self.viewer).status_code,429)
        self.assertEqual(self.event(row,{'action':'room_settings','settings':{'chat_enabled':False}}).status_code,200)
        self.assertEqual(self.event(row,{'action':'comment','text':'blocked'},self.viewer).status_code,403)

    def test_permanent_host_ban_applies_to_new_rooms_and_cannot_target_admin(self):
        row=self.create();self.join(row,self.viewer)
        self.assertEqual(self.event(row,{'action':'ban_from_host_rooms','user_id':self.viewer.pk}).status_code,200)
        self.assertTrue(LiveHostBan.objects.filter(host=self.owner,user=self.viewer).exists())
        self.assertEqual(self.join(row,self.viewer).status_code,403)
        self.assertEqual(self.event(row,{'action':'ban_from_host_rooms','user_id':self.admin.pk}).status_code,403)
        self.assertEqual(self.event(row,{'action':'remove_host_ban','user_id':self.viewer.pk}).status_code,200)

    def test_invite_mic_requires_local_consent_and_forced_mute_revokes_publish(self):
        row=self.create();row.max_speakers=9;row.mode='audio';row.save()
        # The fixture's original video host must publish an audio track before
        # the server will accept a listener into this audio-mode test room.
        self.parts=[{'identity':'host-'+str(self.owner.pk),
                     'tracks':[{'type':'AUDIO','muted':False}]}]
        from . import live
        live.synchronize(row,True)
        self.assertEqual(self.join(row,self.viewer).status_code,200)
        self.assertEqual(self.event(row,{'action':'invite_to_seat','user_id':self.viewer.pk}).status_code,200)
        seat=LiveSeat.objects.get(admission__broadcast=row,admission__user=self.viewer)
        self.assertEqual(seat.seat_number,1)
        self.assertEqual(self.event(row,{'action':'force_mute_mic','user_id':self.viewer.pk}).status_code,200)
        a=LiveAdmission.objects.select_related('broadcast','user').get(broadcast=row,user=self.viewer)
        from .live_stage import publishing
        self.assertEqual(publishing(a),(False,[]))
        self.assertEqual(self.event(row,{'action':'release_mic_mute','user_id':self.viewer.pk}).status_code,200)
        a.refresh_from_db()
        self.assertEqual(publishing(a),(True,['microphone']))

    def test_lock_seat_blocks_invitation_and_free_join_requires_space(self):
        row=self.create();row.max_speakers=9;row.mode='audio';row.save()
        # The fixture's original video host must publish an audio track before
        # the server will accept a listener into this audio-mode test room.
        self.parts=[{'identity':'host-'+str(self.owner.pk),
                     'tracks':[{'type':'AUDIO','muted':False}]}]
        from . import live
        live.synchronize(row,True)
        self.assertEqual(self.join(row,self.viewer).status_code,200)
        for number in range(1,row.max_speakers):
            self.assertEqual(self.event(row,{'action':'room_settings',
                'settings':{'lock_seat':{'number':number,'locked':True}}}).status_code,200)
        self.assertEqual(self.event(row,{'action':'invite_to_seat','user_id':self.viewer.pk}).status_code,409)
        self.assertEqual(self.event(row,{'action':'room_settings','settings':{'lock_seat':{'number':1,'locked':False},
                   'seat_join_mode':'free'}}).status_code,200)
        self.assertEqual(self.event(row,{'action':'seat_request','mode':'audio'},self.viewer).status_code,200)
        self.assertEqual(LiveSeat.objects.get(admission__user=self.viewer).status,'approved')

    def test_password_protects_admission_before_financial_hold(self):
        row=self.create(True);self.assertEqual(self.event(row,{'action':'room_settings','settings':{'password':'secret-987'}}).status_code,200)
        self.assertEqual(self.join(row,self.viewer).status_code,403)
        self.assertEqual(FinancialOperation.objects.count(),0)
        p={'password':'secret-987','confirmed':True,'price_cents':100}
        self.assertEqual(self.request(f'/api/live/{row.pk}/admission',p,self.viewer).status_code,200)
        self.assertEqual(FinancialOperation.objects.count(),1)
        self.assertEqual(self.event(row,{'action':'room_settings','settings':{'password':'new-secret'}}).status_code,200)
        self.assertEqual(self.request(f'/api/live/{row.pk}/events',user=self.viewer).status_code,403)

    def test_user_report_has_verified_room_target_and_server_timestamp(self):
        row=self.create();self.join(row,self.viewer)
        report={'action':'report_user','user_id':self.viewer.pk,'reason':'مخالفة معروفة'}
        self.assertEqual(self.event(row,report).status_code,200)
        report=LiveReport.objects.get()
        self.assertEqual(report.target_user_id,self.viewer.pk)
        self.assertIsNotNone(report.created_at)
        self.assertEqual(self.event(row,{'action':'report_user','user_id':999999,'reason':'inexistent'}).status_code,404)


@override_settings(SECURE_SSL_REDIRECT=False)
class SuperAdminDisciplineTests(TestCase):
    setUp=fixture.LiveBroadcastTests.setUp
    request=fixture.LiveBroadcastTests.request
    create=fixture.LiveBroadcastTests.create
    join=fixture.LiveBroadcastTests.join

    def action(self,kind,user=None,hours=None):
        payload={'action':kind,'user_id':(user or self.viewer).pk,'reason':'Confirmed policy violation'}
        if hours:payload['hours']=hours
        return self.request('/api/admin/control/discipline',payload,self.admin)
    def test_suspension_invalidates_tokens_and_freezes_wallet_requests(self):
        row=self.create();self.join(row,self.viewer)
        before=Wallet.objects.get(user=self.viewer).balance_cents
        self.assertEqual(self.action('suspend',hours=24).status_code,200)
        self.assertEqual(self.request('/api/wallet',user=self.viewer).status_code,401)
        self.assertFalse(SessionToken.objects.filter(user=self.viewer).exists())
        state=AccountModerationState.objects.get(user=self.viewer)
        self.assertEqual(state.status,'suspended')
        self.assertIsNotNone(state.suspended_until)
        self.assertEqual(Wallet.objects.get(user=self.viewer).balance_cents,before)
        self.assertEqual(self.request('/api/admin/control/discipline',user=self.viewer).status_code,401)
        self.assertEqual(self.action('restore').status_code,200)
        self.assertEqual(AccountModerationState.objects.get(user=self.viewer).status,'active')

    def test_permanent_ban_revokes_auth_and_protects_super_user(self):
        self.assertEqual(self.action('ban_permanent').status_code,200)
        self.viewer.refresh_from_db();self.assertFalse(self.viewer.is_active)
        self.assertEqual(self.action('restore').status_code,200)
        self.viewer.refresh_from_db();self.assertTrue(self.viewer.is_active)
        self.assertEqual(self.action('ban_permanent',self.admin).status_code,403)
