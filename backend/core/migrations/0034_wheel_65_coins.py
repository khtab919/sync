"""Set transparent 65-coin Relaxia wheel odds without touching prior spin history.

Wallet balance_cents represents internal coins (100 coins = 1 USD).
Existing administrative enablement, reserve and daily safety limits are preserved.
"""
from django.db import migrations


PRIZES = [
    {'amount_cents': 0, 'weight': 500},     # 50%
    {'amount_cents': 13, 'weight': 250},    # 25%
    {'amount_cents': 39, 'weight': 150},    # 15%
    {'amount_cents': 130, 'weight': 80},    # 8%
    {'amount_cents': 650, 'weight': 18},    # 1.8%
    {'amount_cents': 1300, 'weight': 2},    # 0.2%
]


def apply_wheel_rules(apps, schema_editor):
    configuration = apps.get_model('core', 'PaidWheelConfiguration')
    db = schema_editor.connection.alias
    wheel, _ = configuration.objects.using(db).get_or_create(pk=1)
    wheel.cost_cents = 65
    wheel.price_step_cents = 0
    wheel.maximum_cost_cents = 65
    wheel.first_reward_cents = 0
    wheel.grand_unlock_spend_cents = 0
    wheel.prizes = PRIZES
    wheel.save(using=db, update_fields=[
        'cost_cents', 'price_step_cents', 'maximum_cost_cents',
        'first_reward_cents', 'grand_unlock_spend_cents', 'prizes',
    ])


class Migration(migrations.Migration):
    dependencies = [('core', '0033_audio_room_gifts')]
    operations = [migrations.RunPython(apply_wheel_rules, migrations.RunPython.noop)]
