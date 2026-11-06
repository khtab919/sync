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
# Preserve all existing secrets; add call-relay credentials once.
python3 - "$domain" <<'PYTURN'
import secrets, socket, sys
from pathlib import Path
path = Path('.env')
content = path.read_text()
values = dict(line.split('=', 1) for line in content.splitlines() if '=' in line and not line.startswith('#'))
additions = []
for key, value in [('LAYALI_TURN_SECRET', secrets.token_urlsafe(48)), ('LAYALI_TURN_HOST', sys.argv[1]), ('LAYALI_TURN_PUBLIC_IP', socket.gethostbyname(sys.argv[1]))]:
    if key == 'LAYALI_TURN_SECRET' and values.get(key) == 'replace-this-with-a-unique-random-secret':
        content = content.replace('LAYALI_TURN_SECRET=replace-this-with-a-unique-random-secret', f'LAYALI_TURN_SECRET={value}')
        additions.append('')
        continue
    if not values.get(key):
        if key in values:
            raise SystemExit(f'{key} is empty in .env; set it before starting the internal call relay.')
        additions.append(f'{key}={value}')
if additions:
    path.write_text(content.rstrip() + '\n' + '\n'.join(additions) + '\n')
    path.chmod(0o600)
PYTURN
if command -v ufw >/dev/null 2>&1 && ufw status 2>/dev/null | head -1 | grep -q 'Status: active'; then
    ufw allow 3478/tcp
    ufw allow 3478/udp
    ufw allow 49160:49200/udp
fi
docker compose config --quiet
docker compose build api
docker compose up -d --wait db
docker compose run --rm api python manage.py migrate --noinput
docker compose run --rm api python manage.py collectstatic --noinput
docker compose up -d
printf 'Check HTTPS health at https://%s/health/\n' "$domain"
echo 'Create your administrator: docker compose exec api python manage.py createsuperuser'

echo "Internal calls: allow TCP/UDP 3478 and UDP 49160-49200 in any DigitalOcean Cloud Firewall attached to this server."
