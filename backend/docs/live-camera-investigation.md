# Live camera investigation — media-1

## Established code defect, versus unproven device root cause
The old client only attached its local camera after `room.connect`, microphone publication,
and camera publication completed. A microphone or signaling error could therefore leave
an empty video stage and a generic connection message. Server screenshots show this same
ordering in the deployed source. They do not contain the Android camera exception or prove
that this ordering is the only cause on the reported phone.

This patch captures the camera independently, attaches a muted inline video and waits for
successful playback plus non-zero decoded dimensions before starting microphone capture
or joining the SFU. The same tracks are subsequently published. Audio-only capture never
opens a camera. Failed capture/preview stops acquired tracks. Leaving during capture stops
late-acquired tracks. A fresh, server-authorized token is fetched after capture so permission
prompts cannot consume the original short-lived token. Publishing permission remains
server-authoritative. Local preview is not proof that remote RTP arrived.

Failures distinguish camera capture, decoded preview, microphone capture and publication.
They do not enter a blind camera/reconnect loop. Retry is explicit, and native permission
denial also offers a retry. Camera switching updates preview mirroring. Room assets use
`media-1` cache revision. Existing no-store WebView behavior and trusted-origin grants remain.

## Server diagnostics
Authenticated room control accepts `media_diagnostic` with allowlisted stage/outcome/reason
and fixed client build only. It emits `client_media room=... user=... stage=...` to the
`relaxia.live.media` logger, capped best-effort per worker/user/room/minute. No raw exception,
SDP, token, device identifier, image or audio is accepted or logged. Reports are untrusted
client observations and cannot change authorization, billing, or live room status.

After deploying, reproduce on the affected phone and inspect:

```sh
cd /opt/layali/backend
docker compose logs --since=10m live-api api
```

Expected milestones: camera_capture ready → camera_preview ready → microphone_capture ready
→ media_publish ready. The first failed stage narrows diagnosis. Camera preview failing before
signaling cannot be repaired by changing TURN alone. Publishing-ready does not prove remote
playback; verify audio/video with another account and actual inbound RTP.

## Validation and remaining work
Seven focused Node tests cover stage ordering, microphone denial, busy camera, rejected
playback, audio-only capture, exit during capture and missing decoded frames. Django tests
verify authenticated diagnostic intake and rejection of arbitrary/raw values. The SFU smoke
checks actual decoded host preview and explicit denied-camera UI in addition to existing
12-participant media, paid admission, revocation, reconnection and gift accounting coverage.
Browser media in CI is synthetic; this is not an Android device, 4G/5G or production test.

Not implemented by this patch: preview before server room creation with a separate Start
confirmation, the full advanced-settings redesign, dynamic broadcast policy administration,
challenge invitations/new authoritative winner/reward state, administrative diagnostics UI,
or resource/capacity benchmarking. Existing PK is not a substitute for those requested
features. No recording/egress/archive service was introduced. No database schema change.
Do not call the black-screen incident resolved until the affected Android device and a
second account actually exchange video/audio with the deployed revision.
