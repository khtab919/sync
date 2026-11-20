from django.db import migrations


def configure_receiver(apps, schema_editor):
    settings = apps.get_model("core", "PaymentSettings")
    settings.objects.using(schema_editor.connection.alias).update_or_create(
        pk=1,
        defaults={"syriatel_cash_enabled": True, "syriatel_cash_number": "0997991404"},
    )


class Migration(migrations.Migration):
    dependencies = [("core", "0043_profile_frame_choice")]
    operations = [migrations.RunPython(configure_receiver, migrations.RunPython.noop)]
