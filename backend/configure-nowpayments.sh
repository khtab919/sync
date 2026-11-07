#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
python3 - <<'PY'
from pathlib import Path
from getpass import getpass
path=Path('.env')
if not path.exists():raise SystemExit('Run backend/deploy.sh first to create the server environment.')
values={}
for key,label in [('NOWPAYMENTS_API_KEY','NOWPayments API key'),('NOWPAYMENTS_IPN_SECRET','NOWPayments IPN secret')]:
    value=getpass(label+' (hidden): ').strip()
    if len(value)<10 or len(value)>512 or any(c in value for c in "\n\r\x00'\\"):
        raise SystemExit('Use the exact key without line breaks, quotes or backslashes. Nothing was changed.')
    values[key]=value
lines=[line for line in path.read_text().splitlines() if not any(line.startswith(key+'=') for key in values)]
# Single-quoted Compose values preserve any literal dollar signs.
lines.extend(key+"='"+value+"'" for key,value in values.items())
path.write_text('\n'.join(lines)+'\n');path.chmod(0o600)
print('Merchant credentials saved on the server. Verify a real small test invoice before launch.')
PY
docker compose up -d --no-deps --force-recreate api
echo 'IPN callback: https://165-227-128-113.sslip.io/api/nowpayments/ipn (or your configured domain).'
