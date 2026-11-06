#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
domain="${1:-165-227-128-113.sslip.io}"
if [[ ! "$domain" =~ ^[a-zA-Z0-9]([a-zA-Z0-9.-]*[a-zA-Z0-9])?\.[a-zA-Z]{2,}$ ]]; then
  echo 'Usage: bash deploy.sh api.your-domain.com (DNS must point to this server)'
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo 'Install Docker Engine and Docker Compose from the official Docker packages first.'
  exit 1
fi
if [[ ! -f .env ]]; then
  umask 077
  python3 - "$domain" <<'PY'
import secrets
import sys
from pathlib import Path
host = sys.argv[1]
Path('.env').write_text(f'LAYALI_DOMAIN={host}\nALLOWED_HOSTS={host}\nDJANGO_SECRET_KEY={secrets.token_urlsafe(64)}\nPOSTGRES_DB=layali\nPOSTGRES_USER=layali\nPOSTGRES_PASSWORD={secrets.token_urlsafe(48)}\nDB_HOST=db\n')
PY
fi
python3 - "$domain" <<'PYENV'
import sys
from pathlib import Path
values = dict(line.split('=', 1) for line in Path('.env').read_text().splitlines() if '=' in line and not line.startswith('#'))
if values.get('LAYALI_DOMAIN') != sys.argv[1]:
    raise SystemExit('Existing .env uses another domain. Review it before redeploying; secrets were not changed.')
PYENV
python3 - "$domain" <<'PYDNS'
import socket
import sys
host = sys.argv[1]
try:
    addresses = socket.gethostbyname_ex(host)[2]
except OSError as error:
    raise SystemExit(f'DNS lookup failed for {host}: {error}')
if host == '165-227-128-113.sslip.io' and '165.227.128.113' not in addresses:
    raise SystemExit('The default domain did not resolve to the expected server IP.')
print('DNS lookup passed for', host)
PYDNS
docker compose config --quiet
docker compose build api
docker compose up -d --wait db
docker compose run --rm api python manage.py migrate --noinput
docker compose run --rm api python manage.py collectstatic --noinput
docker compose up -d
printf 'Check HTTPS health at https://%s/health/\n' "$domain"
echo 'Create your administrator: docker compose exec api python manage.py createsuperuser'
