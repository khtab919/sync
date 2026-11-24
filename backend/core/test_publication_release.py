import json
import uuid
from decimal import Decimal
from unittest.mock import patch
from django.contrib.auth import get_user_model
from django.test import TestCase,override_settings
from . import finance
from .models import Wallet,Profile,Service,UserNotification
from .views import issue_token

@override_settings(SECURE_SSL_REDIRECT=False)
class PublicationReleaseTests(TestCase):
    def setUp(self):
        User=get_user_model()
        self.buyer=User.objects.create_user(username="buyer")
        self.host=User.objects.create_user(username="host")
        self.tokens={}
        for user in (self.buyer,self.host):
            Profile.objects.create(user=user,publishing_allowed=True)
            self.tokens[user.pk]=issue_token(user)
            Wallet.objects.create(user=user,balance_cents=2000)
    def request(self,path,data=None,user=None):
        headers={"HTTP_AUTHORIZATION":"Bearer "+self.tokens[(user or self.host).pk]}
        if data is None:return self.client.get(path,**headers)
        return self.client.post(path,json.dumps(data),content_type="application/json",**headers)
    def test_only_confirmed_earnings_withdrawable(self):
        def withdraw(n):
            return self.request("/api/withdrawals",{"amount_cents":n,"network":"usdttrc20","destination":"T"+"A"*33,"request_key":str(uuid.uuid4())})
        self.assertEqual(withdraw(100).status_code,409)
        for kind in ("booking","live_gift","paid_reel"):
            op=finance.hold(self.buyer,self.host,kind,uuid.uuid4(),"1",100)
            finance.settle(op)
            finance.settle(op)
        self.assertEqual(self.request("/api/wallet").json()["withdrawable_coins"],160)
        self.assertEqual(withdraw(150).status_code,201)
        self.assertEqual(withdraw(11).status_code,409)
    @patch("core.bank_transfer.transfer_rate",return_value=Decimal("50.6286"))
    def test_bank_quote(self,rate):
        path="/api/topup-quotes"
        data={"method":"banktr","amount_cents":1000}
        quote=self.request(path,data).json()
        self.assertEqual((quote['payment_amount'],quote['payment_currency']),('506.29','TRY'))
        self.assertEqual(quote['beneficiary'],'Luay Elomar')
        self.assertEqual(quote['destination'],'TR850082900009491652391003')
        self.assertEqual(self.request(path,data).json()['id'],quote['id'])
        self.assertEqual(rate.call_count,1)
    def test_private_service_retry_and_comment_permission(self):
        data=dict(title="عرض خاص",description="تفاصيل العرض الخاص",price_cents=150,
                  publish_key=str(uuid.uuid4()),owner_only=True,comments_allowed=False)
        response=self.request("/api/services",data)
        self.assertEqual(response.status_code,201,response.content)
        pk=response.json()['id']
        self.assertEqual(self.request("/api/services",data).json()['id'],pk)
        self.assertEqual(UserNotification.objects.filter(user=self.host,kind="published").count(),1)
        self.assertFalse(self.request("/api/services",user=self.buyer).json()['items'])
        self.assertEqual(self.request(f"/api/publications/service/{pk}/comments",{"body":"hello"},self.buyer).status_code,404)
        self.assertEqual(self.request(f"/api/publications/service/{pk}/comments",{"body":"hello"}).status_code,409)
