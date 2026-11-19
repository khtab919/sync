#!/usr/bin/env bash
# Relaxia 11.7.11 server upgrade; does not delete Docker volumes or overwrite .env.
set -Eeuo pipefail
umask 077
ACTION="${1:---check}"
PACKAGE="${2:-/opt/Relaxia-11.7.11-Server-Moderation.zip}"
ROOT="${RELAXIA_ROOT:-/opt/layali}"
die() { echo "ERROR: $*" >&2; exit 1; }
[[ "$ACTION" == "--check" || "$ACTION" == "--apply" ]] || die "Use --check or --apply"
[[ -f "$PACKAGE" ]] || die "ZIP not found: $PACKAGE"
[[ -f "$ROOT/backend/compose.yaml" && -f "$ROOT/backend/.env" ]] || die "Existing Compose and .env not found: $ROOT/backend"
[[ -d "$ROOT/backend/core" && -d "$ROOT/backend/config" ]] || die "Existing core/config source missing"
for cmd in docker python3 unzip tar; do command -v "$cmd" >/dev/null || die "Missing $cmd"; done
docker compose version >/dev/null || die "Docker Compose unavailable"
unzip -tqq "$PACKAGE" >/dev/null || die "ZIP integrity check failed"
python3 - "$PACKAGE" <<'PY'
from pathlib import Path
from zipfile import ZipFile
import sys
required={'backend/core/models.py','backend/core/live.py','backend/core/live_moderation.py',
          'backend/core/admin_discipline.py','backend/core/firestore_realtime.py',
          'backend/core/migrations/0042_live_moderation_controls.py',
          'backend/config/urls.py','backend/requirements.txt',
          'backend/core/static/core/room-moderation-ui.js',
          'backend/core/static/core/room-moderation-ui.css'}
with ZipFile(sys.argv[1]) as z:
    seen=set(z.namelist())
    assert required<=seen, f"Missing files: {sorted(required-seen)}"
    for item in z.infolist():
        p=Path(item.filename)
        assert not p.is_absolute() and '..' not in p.parts, f"Unsafe path: {p}"
        if p.parts[0]=='backend':
            assert len(p.parts)>1 and p.parts[1] in {'core','config','manage.py','requirements.txt','Dockerfile','fetch_live_sdk.py'},f"Unexpected path: {p}"
        else:
            assert p.name=='DEPLOY-README-AR.txt',f"Unexpected member: {p}"
        assert p.name not in {'.env','google-services.json'} and p.suffix not in {'.jks','.pem','.key','.p12'},f"Sensitive file: {p}"
    print(f'Package verified: {len(seen)} members')
PY
cd "$ROOT/backend"
docker compose config -q || die "Compose config invalid"
docker compose exec -T db sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >/dev/null || die "PostgreSQL is unavailable"
echo "Existing services:"
docker compose ps
if [[ "$ACTION" == "--check" ]]; then
    echo "Preflight completed; no server changes were made."
    exit 0
fi
BACKUP_BASE="/opt/layali-backups"
mkdir -p "$BACKUP_BASE"
chmod 700 "$BACKUP_BASE"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP="$BACKUP_BASE/pre-relaxia-11.7.11-$stamp"
mkdir -p "$BACKUP"
chmod 700 "$BACKUP"
echo "Backing up source..."
tar -czf "$BACKUP/backend-source.tar.gz" --exclude='__pycache__' -C "$ROOT" \
    backend/core backend/config backend/Dockerfile backend/requirements.txt backend/manage.py
test -s "$BACKUP/backend-source.tar.gz" || die "Source backup failed"
echo "Backing up PostgreSQL..."
docker compose exec -T db sh -c 'exec pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB"' > "$BACKUP/postgres.dump"
test -s "$BACKUP/postgres.dump" || die "Database backup failed"
if [[ "${RELAXIA_BACKUP_MEDIA:-1}" == "1" ]]; then
    echo "Backing up uploaded media..."
    docker compose exec -T api tar -czf - -C /app media > "$BACKUP/media.tar.gz" || die "Media backup failed: no changes made"
    test -s "$BACKUP/media.tar.gz" || die "Media backup is empty"
fi
echo "Backup ready at $BACKUP"
echo "Overlaying application files, preserving Compose/env/volumes..."
unzip -oq "$PACKAGE" -d "$ROOT"
echo "Building updated Django image..."
docker compose build api
docker compose run --rm -T --no-deps api python manage.py makemigrations --check --dry-run
docker compose run --rm -T --no-deps api python manage.py check
echo "Applying additive database migrations..."
docker compose run --rm -T --no-deps api python manage.py migrate --noinput
docker compose run --rm -T --no-deps api python manage.py collectstatic --noinput
echo "Restarting API-dependent services (not database, media or LiveKit)..."
docker compose up -d --no-deps --force-recreate api live-api live-maintenance push-worker
docker compose exec -T api python manage.py check
docker compose ps
echo "Update applied. Backup: $BACKUP"
echo "Firestore mirroring stays disabled until separately configured, tested and rules deployed."
echo "Verify account moderation, wallet, room roles, video/audio, gifts, login and payment flows."
