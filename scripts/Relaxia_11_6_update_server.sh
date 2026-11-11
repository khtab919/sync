#!/usr/bin/env bash
# Relaxia 11.6 server update. Default: read-only inspection.
set -Eeuo pipefail
umask 077
ROOT=/opt/layali
BRANCH=fix/relaxia-115-integrated-review-20261111
PIN=8d00b7c2eee0957a224cd45da4922c95b55bde0d
MODE="${1:---check}"

for tool in git docker python3 curl tar gzip; do command -v "$tool" >/dev/null || { echo "Missing: $tool" >&2; exit 1; }; done
[[ -d "$ROOT/.git" && -f "$ROOT/backend/compose.yaml" && -f "$ROOT/backend/.env" ]] || { echo "Expected /opt/layali Git project with backend/.env and compose.yaml" >&2; exit 1; }
cd "$ROOT"
DOMAIN="$(sed -n 's/^LAYALI_DOMAIN=//p' backend/.env | tail -1 | tr -d '"\r')"
[[ -n "$DOMAIN" ]] || { echo "Missing LAYALI_DOMAIN in backend/.env" >&2; exit 1; }
PREVIOUS="$(git rev-parse HEAD)"
echo "Current: $PREVIOUS"
echo "Target:  $PIN"
echo "Domain:  $DOMAIN"

if [[ "$MODE" == "--check" ]]; then
  echo "Checking Docker Compose and current service status. No files will be changed."
  cd backend
  docker compose -f compose.yaml config --quiet
  docker compose -f compose.yaml ps
  docker compose -f compose.yaml exec -T api python manage.py release_check || echo "release_check failed; inspect backend before updating."
  curl --max-time 15 -fsS "https://$DOMAIN/health/" || echo "HTTPS health failed; investigate server."
  echo
  echo "Inspection complete. No deployment performed."
  exit 0
fi
[[ "$MODE" == "--apply" ]] || { echo "Usage: bash Relaxia_11_6_update_server.sh [--check|--apply]"; exit 2; }
[[ "${RELAXIA_APPROVE_PRODUCTION:-}" == YES ]] || { echo "Must set RELAXIA_APPROVE_PRODUCTION=YES to authorize production update." >&2; exit 2; }
[[ -z "$(git status --porcelain)" ]] || { echo "Uncommitted changes found, refusing to replace them." >&2; git status --short; exit 1; }

git fetch --no-tags origin "$BRANCH"
[[ "$(git rev-parse FETCH_HEAD)" == "$PIN" ]] || { echo "Remote branch changed. Review its commit before deploying." >&2; exit 1; }
cd backend
docker compose -f compose.yaml config --quiet

install -d -m 700 /opt/backups
BACKUP="$(mktemp -d /opt/backups/relaxia116-XXXXXXXX)"
chmod 700 "$BACKUP"
printf '%s\n' "$PREVIOUS" > "$BACKUP/previous-commit.txt"
cp -a .env "$BACKUP/backend.env"
git -C "$ROOT" archive "$PREVIOUS" | gzip > "$BACKUP/code.tar.gz"
echo "Backing up current PostgreSQL DB..."
docker compose -f compose.yaml exec -T db sh -c 'exec pg_dump -Fc -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > "$BACKUP/database.dump"
[[ -s "$BACKUP/database.dump" ]] || { echo "Database backup failed. Aborting." >&2; exit 1; }
echo "Backing up user media..."
docker compose -f compose.yaml exec -T api sh -c 'cd /app/media && exec tar -czf - .' > "$BACKUP/media.tar.gz"
[[ -s "$BACKUP/media.tar.gz" ]] || { echo "Media backup failed. Aborting." >&2; exit 1; }
echo "Backups saved to $BACKUP"

cd "$ROOT"
git switch --detach "$PIN"
bash backend/deploy.sh "$DOMAIN"

cd backend
docker compose -f compose.yaml exec -T api python manage.py release_check
curl --max-time 20 -fsS "https://$DOMAIN/health/" | python3 -c '
import json,sys
x=json.load(sys.stdin)
print("Health:",x.get("status"),"Version:",x.get("build"))
assert x.get("status")=="ok" and x.get("build")=="11.6","Wrong server build or unhealthy deployment"
'
echo "Update deployed. Backup at $BACKUP"
echo "Previous revision: $PREVIOUS"
echo "Do not restore an old database over new user activity without a recovery plan."
