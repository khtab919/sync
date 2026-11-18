from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [('core', '0039_retire_admin_totp')]

    operations = [
        migrations.AddField(
            model_name='withdrawal', name='network',
            field=models.CharField(max_length=16, default='legacy')),
        migrations.AddField(
            model_name='withdrawal', name='reviewed_at',
            field=models.DateTimeField(null=True, blank=True)),
        migrations.AddField(
            model_name='withdrawal', name='reviewed_by',
            field=models.ForeignKey(to=settings.AUTH_USER_MODEL, null=True, blank=True,
                related_name='reviewed_withdrawals',
                on_delete=django.db.models.deletion.PROTECT)),
        migrations.AddField(
            model_name='withdrawal', name='provider_batch_id',
            field=models.CharField(max_length=64, blank=True, null=True, unique=True)),
        migrations.AddField(
            model_name='withdrawal', name='provider_withdrawal_id',
            field=models.CharField(max_length=64, blank=True, null=True, unique=True)),
        migrations.AddField(
            model_name='withdrawal', name='provider_status',
            field=models.CharField(max_length=32, blank=True)),
        migrations.CreateModel(
            name='UserNotification',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('kind', models.CharField(max_length=32, default='wallet')),
                ('title', models.CharField(max_length=120)),
                ('body', models.CharField(max_length=500)),
                ('reference', models.CharField(max_length=100, blank=True)),
                ('read_at', models.DateTimeField(null=True, blank=True)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('user', models.ForeignKey(to=settings.AUTH_USER_MODEL, related_name='relaxia_notices',
                    on_delete=django.db.models.deletion.PROTECT)),
            ],
            options={'indexes': [models.Index(fields=['user', '-created_at'],
                name='user_notice_recent')]},
        ),
    ]
