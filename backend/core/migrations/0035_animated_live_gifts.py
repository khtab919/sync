"""Add the four gifts from the premium visual catalog missing from older rooms.

Existing prices, admin-managed enabled states and effects are never changed.
"""
from django.db import migrations

NEW_GIFTS = (
    ('thumbs_up', 'إعجاب', '👍', 20, 'simple'),
    ('gift_box', 'علبة هدية', '🎁', 400, 'animated'),
    ('unicorn', 'وحيد القرن', '🦄', 3500, 'depth'),
    ('airplane', 'طائرة', '✈️', 6500, 'depth'),
)

def seed_gifts(apps, schema_editor):
    Gift = apps.get_model('core', 'LiveGiftDefinition')
    for code, name, symbol, price, effect in NEW_GIFTS:
        Gift.objects.using(schema_editor.connection.alias).get_or_create(
            code=code,
            defaults={'name':name,'symbol':symbol,'price_cents':price,
                      'effect':effect,'enabled':True},
        )

class Migration(migrations.Migration):
    dependencies = [('core', '0034_wheel_65_coins')]
    operations = [migrations.RunPython(seed_gifts, migrations.RunPython.noop)]
