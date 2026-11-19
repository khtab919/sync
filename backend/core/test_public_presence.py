"""Cross-screen public appearance contract (database is sole authority)."""
from datetime import timedelta
from django.utils import timezone
from django.test import override_settings
from .models import Profile, BenefitGrant, ExperienceAward
from .views import profile_json
from .public_presence import public_presence, public_presence_bulk
from . import test_live as fixtures


@override_settings(SECURE_SSL_REDIRECT=False)
class PublicAppearanceTests(fixtures.LiveBroadcastTests):
    def test_profile_and_room_host_use_same_xp_level_frame_and_badges(self):
        row=self.create()
        Profile.objects.filter(user=self.owner).update(
            display_name='المضيف الفعلي',avatar_url='https://example.com/host.png',
            selected_frame=110,identity_verified=True,
        )
        ExperienceAward.objects.create(user=self.owner,source='verified-invite',points=800)
        BenefitGrant.objects.create(
            user=self.owner,actor=self.admin,kind='level',value=42,
            reason='Verified prestige grant',expires_at=timezone.now()+timedelta(days=4))
        BenefitGrant.objects.create(
            user=self.owner,actor=self.admin,kind='frame',value=120,
            reason='Verified frame grant',expires_at=timezone.now()+timedelta(days=4))
        BenefitGrant.objects.create(
            user=self.owner,actor=self.admin,kind='vip',value=2,
            reason='Verified VIP grant',expires_at=timezone.now()+timedelta(days=4))
        profile=profile_json(self.owner,self.viewer)
        from .live_stage import snapshot
        host=snapshot(row,self.viewer)['speakers'][0]
        for key in ('level','selected_frame','avatar_url','name','vip_tier','identity_verified'):
            expected=profile['display_name'] if key=='name' else profile[key]
            self.assertEqual(host[key],expected,key)
        self.assertEqual(host['level'],42,'admin level grant must override earned XP level')
        self.assertEqual(host['selected_frame'],120,'the actual active grant wins over equipped frame')
        self.assertEqual(host['vip_tier'],2)
        self.assertTrue(host['identity_verified'])
        self.assertEqual(host['name'],'المضيف الفعلي')

    def test_comment_appearance_updates_without_recreating_room_or_login(self):
        row=self.create()
        self.assertEqual(self.join(row,self.viewer).status_code,200)
        Profile.objects.filter(user=self.viewer).update(
            display_name='صوت المشاهد',avatar_url='https://example.com/a.png',
            selected_frame=0)
        posted=self.request(f'/api/live/{row.pk}/events',
            {'action':'comment','text':'رسالة ظاهرة للجميع'},self.viewer)
        self.assertEqual(posted.status_code,200,posted.content)
        original=next(c for c in posted.json()['comments'] if c['user_id']==self.viewer.pk)
        self.assertEqual(original['level'],1)
        self.assertEqual(original['selected_frame'],0)
        self.assertEqual(original['avatar_url'],'https://example.com/a.png')
        ExperienceAward.objects.create(user=self.viewer,source='new-XP-achievement',points=2000)
        Profile.objects.filter(user=self.viewer).update(
            selected_frame=110,avatar_url='https://example.com/b.png')
        updated=self.request(f'/api/live/{row.pk}/events',user=self.viewer)
        self.assertEqual(updated.status_code,200,updated.content)
        refreshed=next(c for c in updated.json()['comments'] if c['user_id']==self.viewer.pk)
        self.assertEqual(refreshed['level'],11)
        self.assertEqual(refreshed['selected_frame'],110)
        self.assertEqual(refreshed['avatar_url'],'https://example.com/b.png')
        self.assertEqual(refreshed['name'],'صوت المشاهد')
        self.assertEqual(profile_json(self.viewer,self.owner)['level'],refreshed['level'])
        self.assertEqual(profile_json(self.viewer,self.owner)['selected_frame'],refreshed['selected_frame'])

    def test_bulk_presence_never_leaks_old_invalid_frame(self):
        Profile.objects.filter(user=self.viewer).update(selected_frame=200)
        first=public_presence(self.viewer)
        self.assertEqual(first['selected_frame'],0,'unowned frames cannot be shown to others')
        ExperienceAward.objects.create(user=self.viewer,source='level-10-award',points=1800)
        Profile.objects.filter(user=self.viewer).update(selected_frame=110)
        second=public_presence_bulk([self.viewer,self.owner])
        self.assertEqual(second[self.viewer.pk]['level'],10)
        self.assertEqual(second[self.viewer.pk]['selected_frame'],110)
        self.assertEqual(second[self.owner.pk]['level'],1)
        self.assertEqual(second[self.owner.pk]['selected_frame'],0)
