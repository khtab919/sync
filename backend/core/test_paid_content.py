import io
import json
import uuid
from datetime import date
from unittest.mock import patch
from PIL import Image
from django.test import TestCase,override_settings
from django.core.files.base import ContentFile
from django.contrib.auth import get_user_model
from .admin_test_support import issue_token
from .models import Profile,Wallet,UploadedMedia,ContentUnlock,Reel,Service


@override_settings(SECURE_SSL_REDIRECT=False)
class PaidContentTests(TestCase):
    def setUp(self):
        self.buyer=get_user_model().objects.create_user('paid_buyer',password='UniquePass2026')
        self.seller=get_user_model().objects.create_user('paid_seller',password='UniquePass2026')
        for u in (self.buyer,self.seller):
            Profile.objects.create(user=u,date_of_birth=date(1990,1,1))
            Wallet.objects.create(user=u)
        Wallet.objects.filter(user=self.buyer).update(balance_cents=1000)
        self.tbuyer=issue_token(self.buyer);self.tseller=issue_token(self.seller)
        image=Image.new('RGB',(280,320),color=(145,72,89))
        buf=io.BytesIO();image.save(buf,format='JPEG')
        self.media=UploadedMedia(owner=self.seller,purpose='paid_photo',
            mime='image/jpeg',size=len(buf.getvalue()))
        self.media.file.save(uuid.uuid4().hex+'.jpg',ContentFile(buf.getvalue()),save=False)
        self.media.save()
    def req(self,path,method='GET',payload=None,token=None):
        kw={'HTTP_AUTHORIZATION':'Bearer '+(token or self.tbuyer)}
        if method=='GET':return self.client.get(path,**kw)
        return getattr(self.client,method.lower())(path,data=json.dumps(payload or {}),
            content_type='application/json',**kw)

    def test_photo_is_blurred_until_single_settled_unlock(self):
        creation=self.req('/api/paid-posts','POST',
            {'media_id':str(self.media.pk),'price_cents':250,'caption':'صورة حصرية'},
            self.tseller)
        self.assertEqual(creation.status_code,201,creation.content)
        post=creation.json()['item'];self.assertFalse(post['locked'])
        listing=self.req('/api/paid-posts?owner='+str(self.seller.pk)).json()['items'][0]
        self.assertTrue(listing['locked'])
        self.assertEqual(listing['media_url'],'')
        self.assertTrue(listing['preview_url'])
        self.assertEqual(self.req('/api/media/'+str(self.media.pk)).status_code,403)
        original=post['id']
        self.assertEqual(self.client.get(listing['preview_url']).status_code,200)
        wrong=self.req('/api/content/photo/'+str(original)+'/unlock','POST',
            {'price_cents':1,'request_key':str(uuid.uuid4())})
        self.assertEqual(wrong.status_code,409)
        request={'price_cents':250,'request_key':str(uuid.uuid4())}
        unlock=self.req('/api/content/photo/'+str(original)+'/unlock','POST',request)
        self.assertEqual(unlock.status_code,200,unlock.content)
        self.assertFalse(unlock.json()['item']['locked'])
        self.assertEqual(self.client.get(unlock.json()['item']['media_url']).status_code,200)
        repeat=self.req('/api/content/photo/'+str(original)+'/unlock','POST',request)
        self.assertEqual(repeat.status_code,200)
        self.assertTrue(repeat.json()['already_owned'])
        self.assertEqual(Wallet.objects.get(user=self.buyer).balance_cents,750)
        self.assertEqual(Wallet.objects.get(user=self.seller).balance_cents,200)
        self.assertEqual(ContentUnlock.objects.count(),1)
        content=self.req('/api/creators/'+str(self.seller.pk)+'/content').json()['items']
        self.assertEqual(content[0]['kind'],'photo')
        self.assertFalse(content[0]['locked'])

    def test_paid_reel_never_exposes_public_video_url(self):
        original=UploadedMedia(owner=self.seller,purpose='reel',mime='video/mp4',size=100)
        original.file.save(uuid.uuid4().hex+'.mp4',ContentFile(b'video'+b'0'*95),save=False)
        original.save()
        cover=UploadedMedia.objects.create(owner=self.seller,purpose='paid_preview',mime='image/jpeg',size=100)
        with patch('core.paid_content._photo_preview',return_value=cover):
            response=self.req('/api/reels','POST',{
                'media_id':str(original.pk),'caption':'ريلز مدفوع','price_cents':125
            },self.tseller)
        self.assertEqual(response.status_code,201,response.content)
        rid=response.json()['id']
        row=self.req('/api/reels').json()['items'][0]
        self.assertTrue(row['locked'])
        self.assertEqual(row['video_url'],'')
        self.assertTrue(row['preview_url'])
        self.assertEqual(self.req('/api/media/'+str(original.pk)).status_code,403)
        unlocked=self.req('/api/content/reel/'+str(rid)+'/unlock','POST',
            {'price_cents':125,'request_key':str(uuid.uuid4())})
        self.assertEqual(unlocked.status_code,200,unlocked.content)
        self.assertTrue(unlocked.json()['item']['video_url'])
        self.assertFalse(self.req('/api/reels').json()['items'][0]['locked'])

    def test_only_owner_can_update_or_delete_publications(self):
        paid=self.req('/api/paid-posts','POST',
            {'media_id':str(self.media.pk),'price_cents':50,'caption':'صورة'},
            self.tseller).json()['id']
        self.assertEqual(self.req('/api/paid-posts/'+str(paid),'DELETE').status_code,404)
        update=self.req('/api/paid-posts/'+str(paid),'PATCH',
            {'caption':'عنوان جديد','price_cents':100},self.tseller)
        self.assertEqual(update.status_code,200)
        removed=self.req('/api/paid-posts/'+str(paid),'DELETE',{},self.tseller)
        self.assertEqual(removed.status_code,200)
        self.assertEqual(self.req('/api/paid-posts?owner='+str(self.seller.pk)).json()['items'],[])
        service=Service.objects.create(owner=self.seller,title='Old',description='Description',
            price_cents=200,approved=True)
        self.assertEqual(self.req('/api/services/'+str(service.pk),'DELETE').status_code,404)
        update=self.req('/api/services/'+str(service.pk),'PATCH',
            {'title':'New Offer'},self.tseller)
        self.assertEqual(update.status_code,200,update.content)
        self.assertEqual(Service.objects.get(pk=service.pk).title,'New Offer')
