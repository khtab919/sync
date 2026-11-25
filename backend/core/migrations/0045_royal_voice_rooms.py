from django.db import migrations, models


def normalize_audio_rooms(apps, schema_editor):
    rooms = apps.get_model("core", "LiveBroadcast").objects.using(schema_editor.connection.alias)
    rooms.filter(mode="audio").exclude(max_speakers__in=[5, 9, 13]).update(max_speakers=9)


class Migration(migrations.Migration):
    dependencies = [("core", "0043_profile_frame_choice")]
    operations = [
        migrations.AddField(model_name="livebroadcast", name="room_theme",
                            field=models.CharField(max_length=16, default="royal")),
        migrations.RunPython(normalize_audio_rooms, migrations.RunPython.noop),
    ]
