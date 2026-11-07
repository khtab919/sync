from django.db import migrations, models
class Migration(migrations.Migration):
    dependencies=[('core','0013_gatewayinvoice')]
    operations=[migrations.AddField(model_name='message',name='read_at',field=models.DateTimeField(null=True,blank=True)),migrations.AddField(model_name='profile',name='last_active_at',field=models.DateTimeField(null=True,blank=True)),migrations.AddField(model_name='internalcall',name='ringing_at',field=models.DateTimeField(null=True,blank=True))]
