from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [('core', '0042_live_moderation_controls')]
    operations = [
        migrations.AddField(model_name='profile', name='frame_choice_explicit',
                            field=models.BooleanField(default=False)),
    ]
