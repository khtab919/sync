"""Add verified gift combo amounts and themed catalog entries without rewriting
existing administrator-defined gift prices, enables, or payout balances."""
from django.db import migrations, models

GIFTS = [
    ("falcon_gold", "صقر الصحراء الذهبي", "🦅", 5000, "depth"),
    ("royal_yacht", "اليخت الملكي", "🛥️", 3500, "depth"),
    ("cyber_supercar", "السيارة الخارقة", "🏎️", 2500, "depth"),
    ("emperor_crown", "تاج الإمبراطور", "👑", 800, "animated"),
    ("knight_sword", "سيف الفارس المضيء", "⚔️", 500, "animated"),
    ("meteor_shower", "وابل النيازك", "☄️", 100, "simple"),
    ("crystal_hearts", "قلوب كريستالية", "💎", 10, "simple"),
]


def add_catalog(apps, schema_editor):
    Gift = apps.get_model("core", "LiveGiftDefinition")
    alias = schema_editor.connection.alias
    for code, name, symbol, price, effect in GIFTS:
        Gift.objects.using(alias).get_or_create(
            code=code,
            defaults={
                "name": name, "symbol": symbol, "price_cents": price,
                "effect": effect, "enabled": True,
            },
        )


class Migration(migrations.Migration):
    dependencies = [("core", "0034_wheel_65_coins")]
    operations = [
        migrations.AddField(
            model_name="livegiftevent",
            name="combo_count",
            field=models.PositiveSmallIntegerField(default=1),
        ),
        migrations.RunPython(add_catalog, migrations.RunPython.noop),
    ]
