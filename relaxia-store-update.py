"""Apply to the existing Relaxia Docker host with sudo python3 relaxia-store-update.py."""
import ast
import fcntl
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path("/opt/layali/backend")
GATE = """    tested = verified_topup_methods(request.actor, settings)
    for method, key in [('stripe','stripe_enabled'), ('nowpayments','nowpayments_enabled'), ('shamcash','shamcash_enabled'), ('syriatelcash','syriatel_cash_enabled')]:
        result[key] = result[key] and method in tested
"""

def commerce(source):
    if 'def verified_topup_methods(' in source:
        if source.count(GATE) != 1 or source.count('verified_topup_methods(') != 2:
            raise ValueError("Unrecognized payment listing rule. Source not changed.")
        node = next(n for n in ast.parse(source).body if getattr(n, 'name', '') == 'verified_topup_methods')
        lines = source.splitlines(keepends=True)
        del lines[node.lineno - 1:node.end_lineno]
        source = ''.join(lines).replace(GATE, '')
    old = 'settings.syriatel_cash_enabled and settings.syriatel_cash_syp_per_usd>0'
    new = 'settings.syriatel_cash_enabled and settings.syriatel_cash_number.strip() and settings.syriatel_cash_syp_per_usd>0'
    if old not in source and new not in source:
        raise ValueError("Unrecognized Syriatel configuration")
    return source.replace(old, new)

def stripe(source):
    source = source.replace("('sk_test_', 'sk_live_')", "('sk_test_', 'sk_live_', 'rk_test_', 'rk_live_')")
    source = source.replace("startswith('sk_live_')", "startswith(('sk_live_', 'rk_live_'))")
    if "'rk_live_'" not in source:
        raise ValueError("Unrecognized Stripe configuration")
    return source

def compose(*args):
    subprocess.run(['docker','compose',*args], cwd=ROOT, check=True)

def install():
    backup = None
    targets = []
    for name, transform in [('commerce.py', commerce), ('stripe_checkout.py', stripe)]:
        path = ROOT / 'core' / name
        original = path.read_text()
        updated = transform(original)
        compile(updated, str(path), 'exec')
        if original != updated:
            targets.append((path, updated))
    if targets:
        backup = Path(tempfile.mkdtemp(prefix='relaxia-store-backup-', dir=ROOT.parent))
        for path, _ in targets:
            shutil.copy2(path, backup / path.name)
        print('Backup:', backup, flush=True)
    try:
        for path, content in targets:
            with tempfile.NamedTemporaryFile(dir=path.parent, delete=False) as f:
                temp = Path(f.name)
            try:
                shutil.copy2(path, temp)
                temp.write_text(content)
                temp.replace(path)
            finally:
                temp.unlink(missing_ok=True)
        compose('build','api')
        compose('run','--rm','-T','--no-deps','api','python','manage.py','check')
        compose('up','-d','--no-deps','api')
        compose('exec','-T','api','python','manage.py','check')
    except Exception:
        if backup:
            for path, _ in targets:
                shutil.copy2(backup / path.name, path)
            compose('build','api')
            compose('up','-d','--no-deps','api')
        raise
    print('Relaxia store update applied.')

if __name__ == '__main__':
    if os.geteuid() != 0:
        raise SystemExit('Run with sudo python3 relaxia-store-update.py')
    with (ROOT.parent / '.relaxia-store-update.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        install()
