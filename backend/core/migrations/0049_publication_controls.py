from django.conf import settings
from django.db import migrations,models
import django.db.models.deletion

class Migration(migrations.Migration):
    dependencies=[("core","0048_reel_processing")]
    operations=[
        migrations.AddField(model_name=model,name=field,field=models.BooleanField(default=default))
        for model in ("service","reel","paidpost")
        for field,default in (("owner_only",False),("comments_allowed",True))
    ]+[
        migrations.AddField(model_name="service",name="publish_key",field=models.UUIDField(null=True,blank=True,unique=True)),
        migrations.CreateModel(name="PublicationComment",fields=[
          ("id",models.BigAutoField(primary_key=True,serialize=False,auto_created=True,verbose_name="ID")),
          ("kind",models.CharField(max_length=10)),("item_id",models.PositiveBigIntegerField()),
          ("body",models.CharField(max_length=500)),("created_at",models.DateTimeField(auto_now_add=True)),
          ("user",models.ForeignKey(to=settings.AUTH_USER_MODEL,on_delete=django.db.models.deletion.CASCADE))])
    ]
