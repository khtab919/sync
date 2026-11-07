"""Generate a non-recording, explicitly-created-room SFU configuration."""
import json,secrets,socket,sys
from pathlib import Path
path=Path(__file__).parent/'.env'
lines=path.read_text().splitlines();values=dict(line.split('=',1) for line in lines if '=' in line and not line.startswith('#'))
domain=values['LAYALI_DOMAIN']
for key,value in [('LIVEKIT_API_KEY','live'+secrets.token_hex(12)),('LIVEKIT_API_SECRET',secrets.token_urlsafe(48)),('LIVEKIT_INTERNAL_URL','http://livekit:7880'),('LIVEKIT_PUBLIC_URL','wss://'+domain+'/livekit'),('LAYALI_LIVE_ENABLED','1'),('LAYALI_MAX_LIVE_ROOMS','5')]:
    if key not in values or not values[key]:
        lines=[line for line in lines if not line.startswith(key+'=')];lines.append(key+'='+value);values[key]=value
path.write_text('\n'.join(lines)+'\n');path.chmod(0o600)
ip=values.get('LAYALI_TURN_PUBLIC_IP') or socket.gethostbyname(domain)
config='''# No egress, ingress, DVR, replay, file sinks or media persistence.
port: 7880
rtc:
  tcp_port: 7881
  port_range_start: 50000
  port_range_end: 50100
  use_external_ip: false
  node_ip: IPVALUE
  turn_servers:
    - host: TURNHOST
      port: 3478
      protocol: udp
      secret: TURNSECRET
    - host: TURNHOST
      port: 3478
      protocol: tcp
      secret: TURNSECRET
room:
  auto_create: false
  empty_timeout: 60
  departure_timeout: 10
  max_participants: 101
logging:
  level: warn
  pion_level: error
keys:
  KEYVALUE: SECRETVALUE
'''.replace('IPVALUE',json.dumps(ip)).replace('KEYVALUE',json.dumps(values['LIVEKIT_API_KEY'])).replace('SECRETVALUE',json.dumps(values['LIVEKIT_API_SECRET'])).replace('TURNHOST',json.dumps(domain)).replace('TURNSECRET',json.dumps(values.get('LAYALI_TURN_SECRET','')))
target=Path(__file__).parent/'livekit.generated.yaml';target.write_text(config);target.chmod(0o600)
print('Non-recording live transport configured. No recording service is deployed.')
