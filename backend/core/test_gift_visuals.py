"""Regression checks for the curated 3D-style live gift catalog."""
from django.test import SimpleTestCase
from importlib import import_module
from pathlib import Path

class AnimatedGiftCatalogTest(SimpleTestCase):
    def test_missing_gifts_have_unique_codes_and_positive_prices(self):
        module = import_module('core.migrations.0035_animated_live_gifts')
        gifts = module.NEW_GIFTS
        self.assertEqual(len(gifts), 4)
        self.assertEqual(len({gift[0] for gift in gifts}), len(gifts))
        self.assertTrue(all(gift[3] > 0 for gift in gifts))

    def test_gift_art_uses_allowlisted_code_not_server_markup(self):
        file = Path(__file__).parent / 'static' / 'core' / 'gift-art.js'
        script = file.read_text(encoding='utf-8')
        self.assertIn('textContent = a ? a[2] : (g.symbol', script)
        self.assertNotIn('innerHTML', script)

    def test_view_includes_local_animation_resources(self):
        template = (Path(__file__).parent / 'templates' / 'core' / 'live_room.html').read_text(encoding='utf-8')
        self.assertIn('/static/core/gift-art.js?v=gift-1', template)
        self.assertIn('rooms-6-gifts', template)
