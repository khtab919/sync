import base64,io,json,tempfile
from django.test import TestCase,override_settings
from django.contrib.auth import get_user_model
from PIL import Image
from .models import Profile,Wallet
from .views import issue_token

@override_settings(SECURE_SSL_REDIRECT=False,ALLOWED_HOSTS=["testserver","testserver.example"])
class AvatarTests(TestCase):
    def setUp(self):
        self.directory=tempfile.TemporaryDirectory();self.addCleanup(self.directory.cleanup)
        self.settings_override=override_settings(MEDIA_ROOT=self.directory.name);self.settings_override.enable();self.addCleanup(self.settings_override.disable)
        self.user=get_user_model().objects.create_user('avatar_owner','owner@example.com','ExistingPass!875')
        self.other=get_user_model().objects.create_user('avatar_other','other@example.com','OtherPass!875')
        for u in (self.user,self.other):Profile.objects.create(user=u);Wallet.objects.create(user=u)
        self.token=issue_token(self.user);self.other_token=issue_token(self.other)
    def call(self,path,p,method='post',token=None):return getattr(self.client,method)(path,json.dumps(p),content_type='application/json',secure=True,HTTP_HOST='testserver.example',HTTP_AUTHORIZATION='Bearer '+(token or self.token))
    def test_upload_attach_display_remove_and_ownership(self):
        stream=io.BytesIO();Image.new('RGB',(32,40),'blue').save(stream,'PNG')
        r=self.call('/api/upload',{'purpose':'avatar','data':base64.b64encode(stream.getvalue()).decode()});self.assertEqual(r.status_code,201)
        key=r.json()['id'];url='/api/media/'+key
        self.assertEqual(self.client.get(url,secure=True).status_code,403)
        self.assertEqual(self.call('/api/me',{'avatar_id':key},'patch',self.other_token).status_code,403)
        r=self.call('/api/me',{'avatar_id':key},'patch');self.assertEqual(r.status_code,200,r.content);self.assertTrue(r.json()['avatar_url'].endswith(url))
        self.assertEqual(self.client.get(url,secure=True).status_code,200)
        r=self.call('/api/me',{'avatar_url':''},'patch');self.assertEqual(r.status_code,200)
        self.assertEqual(self.client.get(url,secure=True).status_code,403)
        self.assertEqual(self.user.profile.public_id,Profile.objects.get(user=self.user).public_id)
    def test_html_is_not_accepted_as_an_avatar(self):
        self.assertEqual(self.call('/api/upload',{'purpose':'avatar','data':base64.b64encode(b'<html>not a photo</html>').decode()}).status_code,400)
