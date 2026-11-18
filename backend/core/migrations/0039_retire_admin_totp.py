from django.db import migrations


def clear_old_totp(apps,schema_editor):
    access=apps.get_model('core','AdminAccess')
    access.objects.all().update(totp_secret='',totp_enabled=False,last_counter=-1,
        failed_attempts=0,locked_until=None)


class Migration(migrations.Migration):
    dependencies=[('core','0038_paid_content')]
    operations=[migrations.RunPython(clear_old_totp,migrations.RunPython.noop)]
