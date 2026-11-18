from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion
import django.db.models
import django.db.models.functions


class Migration(migrations.Migration):
    dependencies=[('core','0037_stripe_checkout')]

    operations=[
        migrations.AddField(model_name='reel',name='price_cents',field=models.PositiveIntegerField(default=0)),
        migrations.AddField(model_name='reel',name='active',field=models.BooleanField(default=True)),
        migrations.AddField(model_name='reel',name='preview',field=models.ForeignKey(
            to='core.uploadedmedia',related_name='reel_previews',null=True,blank=True,
            on_delete=django.db.models.deletion.PROTECT)),
        migrations.CreateModel(name='PaidPost',fields=[
            ('id',models.BigAutoField(auto_created=True,primary_key=True,serialize=False,verbose_name='ID')),
            ('caption',models.CharField(max_length=500,blank=True)),
            ('price_cents',models.PositiveIntegerField()),
            ('active',models.BooleanField(default=True)),
            ('created_at',models.DateTimeField(auto_now_add=True)),
            ('owner',models.ForeignKey(to=settings.AUTH_USER_MODEL,on_delete=django.db.models.deletion.PROTECT)),
            ('media',models.OneToOneField(to='core.uploadedmedia',related_name='paid_post_original',on_delete=django.db.models.deletion.PROTECT)),
            ('preview',models.OneToOneField(to='core.uploadedmedia',related_name='paid_post_preview',on_delete=django.db.models.deletion.PROTECT)),
        ]),
        migrations.CreateModel(name='ContentUnlock',fields=[
            ('id',models.BigAutoField(auto_created=True,primary_key=True,serialize=False,verbose_name='ID')),
            ('created_at',models.DateTimeField(auto_now_add=True)),
            ('buyer',models.ForeignKey(to=settings.AUTH_USER_MODEL,on_delete=django.db.models.deletion.PROTECT)),
            ('reel',models.ForeignKey(to='core.reel',null=True,blank=True,on_delete=django.db.models.deletion.PROTECT)),
            ('post',models.ForeignKey(to='core.paidpost',null=True,blank=True,on_delete=django.db.models.deletion.PROTECT)),
            ('operation',models.OneToOneField(to='core.financialoperation',on_delete=django.db.models.deletion.PROTECT))
        ],options={'constraints':[
            models.UniqueConstraint(fields=('buyer','reel'),name='one_paid_reel_unlock'),
            models.UniqueConstraint(fields=('buyer','post'),name='one_paid_photo_unlock'),
            models.CheckConstraint(condition=(models.Q(reel__isnull=False,post__isnull=True)|models.Q(reel__isnull=True,post__isnull=False)),name='one_media_content_kind')
        ]})
    ]
