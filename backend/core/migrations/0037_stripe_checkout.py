from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [('core', '0036_luxury_gift_catalog')]

    operations = [
        migrations.CreateModel(
            name='StripeCheckout',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('session_id', models.CharField(blank=True, max_length=255, null=True, unique=True)),
                ('session_url', models.URLField(blank=True, max_length=2048)),
                ('payment_intent_id', models.CharField(blank=True, max_length=255, null=True, unique=True)),
                ('status', models.CharField(default='creating', max_length=32)),
                ('credited_at', models.DateTimeField(blank=True, null=True)),
                ('review_required', models.BooleanField(default=False)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('quote', models.OneToOneField(on_delete=django.db.models.deletion.PROTECT, to='core.topupquote')),
            ],
        ),
    ]
