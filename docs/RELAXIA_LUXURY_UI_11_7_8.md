# Relaxia 11.7.8 — Royal Gift UI, Safe Navigation, Recharge Store

**Original Android applicationId is unchanged:** `com.aistudio.layalilounge.x9m4kp`.  
**Original backend:** Django/Docker, not Node.js/Socket.IO.  
**Release branch:** `fix/live-controls-reels-20261116`.

## 1. Complete edited source files

- `backend/core/migrations/0036_luxury_gift_catalog.py`: seeds exactly 21 stable `gift_*` product identifiers with the requested names, prices, and effect tiers. Keeps old settlement records.
- `backend/core/live_stage.py`: returns the 21 public gifts in ascending price order with `simple`, `animated`, `luxury` categories and WebP image/animation paths. Server continues validating unit prices, combo amounts, recipients, ledger idempotency and settled credit.
- `backend/core/static/core/live-stage.js`: category filters, real WebP images, price-confirmed gift checkout, full-screen/seat effects for settled events, retry protection.
- `backend/core/static/core/live-stage.css`: 3-column responsive royal navy cards, gold premium outlines above 1,000 coins, readable price tags and fullscreen effects.
- `backend/core/templates/core/live_room.html`: asset cache key `rooms-controls-26`.
- `backend/qa/audio_rooms_ui_test.cjs`, `backend/core/test_live_stage.py`: gift tests.
- `app/src/main/java/com/example/ui/screens/ServerAppScreen.kt`: Android safe bottom navigation, readable icons/text, divider, extra feed bottom padding, left-aligned FAB.
- `app/src/main/java/com/example/ui/screens/CoinShopScreen.kt`: six royal navy glass cards in a 3x2 grid, local WebP art with safe fallback, dynamic backend wallet balance, fixed blue/gold payment CTA and actual payment quote flow.
- `app/src/test/java/com/example/ui/screens/ReferenceDesignTest.kt`: updated coin tests.
- `app/build.gradle.kts`: Android version 11.7.8; original package and signing identity preserved.
- `backend/core/static/core/gifts/catalog.json`: exact gift IDs and asset URLs.
- `backend/scripts/install_luxury_assets.sh`: installer for separately delivered binary WebP assets with deployment verification.

## 2. Gift assets supplied separately in Relaxia-Luxury-Assets.zip

The asset archive contains:
- `backend/core/static/core/gifts/icons/gift_*.webp`: 21 transparent photorealistic/stylized CGI-derived sprites.
- `backend/core/static/core/gifts/animations/gift_*.webp`: 21 short animated WebP files.
- `app/src/main/assets/coins/ic_coin_100.webp`, `ic_coin_300.webp`, `ic_coin_1000.webp`, `ic_coin_2500.webp`, `ic_coin_5000.webp`, `ic_coin_10000.webp`: 6 transparent recharge sprites.

The new WebP images are distributed in the attached archive, not as binaries in this Git repository. They must be copied into these exact folders before deploying/rebuilding.

## 3. Safe server deployment

**First download the assets archive to the server as** `/opt/layali/Relaxia-Luxury-Assets.zip`. Then run:

```bash
cd /opt/layali
git fetch origin fix/live-controls-reels-20261116
git switch fix/live-controls-reels-20261116
git merge --ff-only origin/fix/live-controls-reels-20261116
bash backend/scripts/install_luxury_assets.sh /opt/layali/Relaxia-Luxury-Assets.zip
```

This runs the existing deployment script, which makes a `pg_dump` backup before migrations. No `down -v`, no reset of accounts/balances.

## 4. Android build and payment security

The APK must be rebuilt once the 6 coin WebP files are placed under `app/src/main/assets/coins/`, using the **existing NEW Relaxia signing certificate**, not a third-party or debug key. Existing payment routes remain server-owned: `/api/payment-info`, `/api/topup-quotes`, `/api/topups`, and NOWPayments invoice/status routes.

The visual gift animations are **animated WebP with CSS depth effects**, not 3D meshes or Lottie/SVGA files. They do not control balances or financial settlement.

## 5. Safety/quality

- Gift unit price and total combo price are enforced by Django, not HTML/Kotlin.
- Same retry request key means the same financial operation.
- Existing wallet history and legacy gifts remain available for historical record integrity.
- All gift UI is 18+ non-explicit.
- The updated UI must be evaluated on a physical Android phone after deployment; automated tests are not a substitute for that.
