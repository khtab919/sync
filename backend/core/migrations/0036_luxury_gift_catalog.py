"""Public Relaxia gift store: 21 server-priced gifts, exact stable identifiers.

Historical gifts and old payment events are preserved. The storefront filters
to these public IDs while old ledger entries remain readable.
"""
from django.db import migrations

GIFT_ITEMS = [
    ("gift_clap", "تصفيق", "👏", 10, "simple"),
    ("gift_coffee", "قهوة", "☕", 25, "simple"),
    ("gift_flower", "زهرة", "🌷", 50, "simple"),
    ("gift_chocolate", "شوكولاتة", "🍫", 75, "simple"),
    ("gift_rose", "وردة", "🌹", 100, "simple"),
    ("gift_teddy", "دبدوب", "🧸", 250, "simple"),
    ("gift_butterfly", "فراشة", "🦋", 150, "animated"),
    ("gift_balloons", "بالونات", "🎈", 200, "animated"),
    ("gift_heart", "قلب", "❤️", 300, "animated"),
    ("gift_bouquet", "باقة ورد", "💐", 500, "animated"),
    ("gift_cake", "كعكة", "🎂", 750, "animated"),
    ("gift_dolphin", "دلفين", "🐬", 3000, "animated"),
    ("gift_star", "نجمة", "⭐", 1000, "depth"),
    ("gift_fireworks", "ألعاب نارية", "🎆", 1500, "depth"),
    ("gift_diamond", "ألماسة", "💎", 2000, "depth"),
    ("gift_crown", "تاج ملكي", "👑", 2500, "depth"),
    ("gift_supercar", "سيارة رياضية", "🏎️", 4000, "depth"),
    ("gift_yacht", "يخت", "🛥️", 5000, "depth"),
    ("gift_eagle", "نسر", "🦅", 6000, "depth"),
    ("gift_palace", "قصر", "🏰", 7500, "depth"),
    ("gift_blue_whale", "الحوت الأزرق", "🐋", 10000, "depth"),
]


def seed_gifts(apps, schema_editor):
    Gift = apps.get_model("core", "LiveGiftDefinition")
    db = schema_editor.connection.alias
    for code, name, symbol, price, effect in GIFT_ITEMS:
        Gift.objects.using(db).update_or_create(
            code=code,
            defaults={"name": name, "symbol": symbol, "price_cents": price,
                      "effect": effect, "enabled": True},
        )


class Migration(migrations.Migration):
    dependencies = [("core", "0035_live_gift_combos")]
    operations = [migrations.RunPython(seed_gifts, migrations.RunPython.noop)]
