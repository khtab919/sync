from django.db import migrations


def hide_educational_offers(apps, schema_editor):
    apps.get_model('core', 'Service').objects.filter(category='كورس').update(active=False)


class Migration(migrations.Migration):
    dependencies = [('core', '0005_profile_avatar_url_alter_service_category')]
    operations = [migrations.RunPython(hide_educational_offers, migrations.RunPython.noop)]
