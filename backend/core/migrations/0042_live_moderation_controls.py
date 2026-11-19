from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [('core', '0041_syriatel_cash_settings')]

    operations = [
        migrations.AddField(model_name='livebroadcast',name='chat_enabled',
            field=models.BooleanField(default=True)),
        migrations.AddField(model_name='livebroadcast',name='chat_slow_seconds',
            field=models.PositiveSmallIntegerField(default=0)),
        migrations.AddField(model_name='livebroadcast',name='block_external_links',
            field=models.BooleanField(default=True)),
        migrations.AddField(model_name='livebroadcast',name='seat_join_mode',
            field=models.CharField(max_length=10,default='request',
                choices=[('request','طلب'),('free','مفتوح')])),
        migrations.AddField(model_name='livebroadcast',name='locked_seat_numbers',
            field=models.JSONField(default=list)),
        migrations.AddField(model_name='livebroadcast',name='password_hash',
            field=models.CharField(max_length=150,blank=True)),
        migrations.AddField(model_name='livebroadcast',name='control_revision',
            field=models.PositiveIntegerField(default=0)),
        migrations.AddField(model_name='liveseat',name='seat_number',
            field=models.PositiveSmallIntegerField(null=True,blank=True)),
        migrations.AddField(model_name='liveadmission',name='forced_mic_mute',
            field=models.BooleanField(default=False)),
        migrations.AddField(model_name='liveadmission',name='password_verified_at',
            field=models.DateTimeField(null=True,blank=True)),
        migrations.AddField(model_name='livereport',name='target_user',
            field=models.ForeignKey(to=settings.AUTH_USER_MODEL,null=True,blank=True,
                on_delete=django.db.models.deletion.PROTECT,related_name='live_reports_received')),
        migrations.CreateModel(
            name='LiveChatRestriction',
            fields=[
                ('id',models.BigAutoField(auto_created=True,primary_key=True,serialize=False,verbose_name='ID')),
                ('muted_until',models.DateTimeField(null=True,blank=True)),
                ('entire_stream',models.BooleanField(default=False)),
                ('reason',models.CharField(max_length=300,blank=True)),
                ('broadcast',models.ForeignKey(to='core.livebroadcast',on_delete=django.db.models.deletion.CASCADE)),
                ('user',models.ForeignKey(to=settings.AUTH_USER_MODEL,on_delete=django.db.models.deletion.PROTECT))
            ],
            options={'constraints':[models.UniqueConstraint(fields=('broadcast','user'),name='unique_room_chat_mute')]}
        ),
        migrations.CreateModel(
            name='LiveHostBan',
            fields=[
                ('id',models.BigAutoField(auto_created=True,primary_key=True,serialize=False,verbose_name='ID')),
                ('reason',models.CharField(max_length=300,blank=True)),
                ('created_at',models.DateTimeField(auto_now_add=True)),
                ('host',models.ForeignKey(to=settings.AUTH_USER_MODEL,on_delete=django.db.models.deletion.PROTECT,related_name='live_bans_issued')),
                ('user',models.ForeignKey(to=settings.AUTH_USER_MODEL,on_delete=django.db.models.deletion.PROTECT,related_name='live_bans_received'))
            ],
            options={'constraints':[models.UniqueConstraint(fields=('host','user'),name='unique_host_lifetime_ban')]}
        ),
    ]
