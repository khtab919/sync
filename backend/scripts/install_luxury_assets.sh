#!/usr/bin/env bash
# Install the user-supplied Relaxia Luxury Assets pack, then deploy the
# updated Django gift catalog, WebView and Caddy static files.
set -euo pipefail

ZIP="${1:-/opt/layali/Relaxia-Luxury-Assets.zip}"
ROOT="/opt/layali"
BRANCH="fix/live-controls-reels-20261116"
DOMAIN="165-227-128-113.sslip.io"

cd "$ROOT"
test -f "$ZIP"
test -s backend/.env
test -f backend/deploy.sh
if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
  echo "There are modified tracked files. Stopping to protect your work." >&2
  exit 1
fi
git fetch origin "refs/heads/$BRANCH:refs/remotes/origin/$BRANCH"
if git show-ref --verify --quiet "refs/heads/$BRANCH"; then
  git switch "$BRANCH"
else
  git switch -c "$BRANCH" --track "origin/$BRANCH"
fi
git merge --ff-only "origin/$BRANCH"

grep -Fq 'rooms-controls-26' backend/core/templates/core/live_room.html
test -f backend/core/migrations/0036_luxury_gift_catalog.py

# The pack contains only backend/static gift WebPs and Android source assets.
unzip -oq "$ZIP" -d "$ROOT"

for id in gift_clap gift_coffee gift_flower gift_chocolate gift_rose gift_teddy \
 gift_butterfly gift_balloons gift_heart gift_bouquet gift_cake gift_dolphin \
 gift_star gift_fireworks gift_diamond gift_crown gift_supercar gift_yacht \
 gift_eagle gift_palace gift_blue_whale; do
 test -s "backend/core/static/core/gifts/icons/$id.webp"
 test -s "backend/core/static/core/gifts/animations/$id.webp"
done

bash backend/deploy.sh "$DOMAIN"
cd backend

# Verify the new static assets, app data and deployment health.
docker compose exec -T api python manage.py release_check
curl -fsSL "https://$DOMAIN/health/" >/dev/null
curl -fsSL "https://$DOMAIN/static/core/gifts/icons/gift_crown.webp" -o /tmp/relaxia-gift-crown-check.webp
test "$(wc -c </tmp/relaxia-gift-crown-check.webp)" -gt 1000
rm -f /tmp/relaxia-gift-crown-check.webp
curl -fsSL "https://$DOMAIN/static/core/live-stage.js?v=rooms-controls-26" -o /tmp/relaxia-live-stage.js
grep -Fq "gift-webp" /tmp/relaxia-live-stage.js
rm -f /tmp/relaxia-live-stage.js

echo "Relaxia gift assets, migrations and WebView UI deployed successfully."
echo "Android coin art will appear only after an APK is rebuilt with app/src/main/assets/coins included."
