"""Pin and self-host the browser transport dependency; no runtime CDN dependency."""
import hashlib,urllib.request
from pathlib import Path
URL='https://cdn.jsdelivr.net/npm/livekit-client@2.23.0/dist/livekit-client.umd.js'
SHA256='fb646f2260d84f91238d0a2dc10726c456631d6a26c7cafc43cfee5959b4d31d'
def fetch():
    data=urllib.request.urlopen(URL,timeout=30).read(2*1024*1024)
    if hashlib.sha256(data).hexdigest()!=SHA256:raise SystemExit('Live transport dependency checksum mismatch')
    target=Path(__file__).parent/'core/static/core/livekit-client.umd.min.js'
    target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
if __name__=='__main__':fetch()
