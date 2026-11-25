from django.db import migrations,models

class Migration(migrations.Migration):
    dependencies=[("core","0047_wallet_earned_coins")]
    operations=[
        migrations.AddField(model_name="uploadsession",name="processing",field=models.CharField(blank=True,max_length=12)),
        migrations.AddField(model_name="uploadsession",name="clip_start",field=models.PositiveIntegerField(default=0)),
        migrations.AddField(model_name="uploadsession",name="clip_end",field=models.PositiveIntegerField(default=0)),
    ]
