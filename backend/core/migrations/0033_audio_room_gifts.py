from django.db import migrations

GIFTS = [('clap', 'تصفيق', '👏', 10, 'simple'), ('coffee', 'قهوة', '☕', 25, 'simple'), ('flower', 'زهرة', '🌷', 50, 'simple'), ('chocolate', 'شوكولاتة', '🍫', 75, 'simple'), ('butterfly', 'فراشة', '🦋', 150, 'animated'), ('balloons', 'بالونات', '🎈', 200, 'animated'), ('teddy', 'دبدوب', '🧸', 250, 'simple'), ('kiss', 'قبلة', '💋', 350, 'simple'), ('bouquet', 'باقة ورد', '💐', 500, 'animated'), ('cake', 'كعكة', '🎂', 750, 'animated'), ('fireworks', 'ألعاب نارية', '🎆', 1500, 'depth'), ('diamond', 'ألماسة', '💎', 2000, 'depth'), ('crown', 'تاج ملكي', '👑', 2500, 'depth'), ('dolphin', 'دلفين', '🐬', 3000, 'animated'), ('sports_car', 'سيارة رياضية', '🏎️', 4000, 'depth'), ('yacht', 'يخت', '🛥️', 5000, 'depth'), ('eagle', 'نسر', '🦅', 6000, 'depth'), ('castle', 'قصر', '🏰', 7500, 'depth'), ('whale', 'الحوت الأزرق', '🐋', 10000, 'depth'), ('lion', 'الأسد الملكي', '🦁', 15000, 'depth'), ('galaxy', 'مجرة', '🌌', 25000, 'depth')]

def seed_catalog(apps, schema_editor):
    Gift = apps.get_model('core', 'LiveGiftDefinition')
    for code, name, symbol, price, effect in GIFTS:
        # Never overwrite an administrator's prices or enabled state.
        Gift.objects.using(schema_editor.connection.alias).get_or_create(code=code, defaults={
            'name': name, 'symbol': symbol, 'price_cents': price, 'effect': effect, 'enabled': True})

class Migration(migrations.Migration):
    dependencies = [('core', '0032_message_core_messag_sender__362d33_idx_and_more')]
    operations = [migrations.RunPython(seed_catalog, migrations.RunPython.noop)]
