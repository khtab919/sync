from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [('core', '0040_usdt_withdrawals_and_notices')]

    operations = [
        migrations.AddField(
            model_name='paymentsettings', name='syriatel_cash_enabled',
            field=models.BooleanField(default=False)),
        migrations.AddField(
            model_name='paymentsettings', name='syriatel_cash_number',
            field=models.CharField(default='0997991404', max_length=10)),
        migrations.AddField(
            model_name='paymentsettings', name='syriatel_cash_syp_per_usd',
            field=models.DecimalField(decimal_places=4, max_digits=16, default=0)),
        migrations.AddField(
            model_name='paymentsettings', name='syriatel_cash_instructions',
            field=models.CharField(blank=True, max_length=500)),
    ]
