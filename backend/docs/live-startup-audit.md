# Relaxia startup audit — 2026-11-07

Baseline: `09e918c5a8f279b153a17aa7f7858624f5d5e5a3`, branch
`feat/layali-adult-non-explicit`. Main is older (`a5ea19f`).

## Findings established from source and read-only production checks

- Production health endpoint responds successfully. Its response is an API health signal,
  not evidence of functioning media or container health.
- Production live-stage.js, live-media.js and live-stage.css matched the baseline byte for
  byte. The served SDK matches the pinned SHA256 in fetch_live_sdk.py. A stale static
  deployment is therefore not established as the cause at the time of this check.
- The audio stage was hidden in initial HTML and only populated by polling after successful
  connection. Signaling/capture failure could leave the audio stage empty as well as video.
- A failure to load/parse the SDK or stage script could prevent JavaScript error UI itself
  from initializing. Android checks symbols after page load, but this does not cover every
  partial initialization failure or a stalled script download.
- A token request or room.connect rejection after successful capture did not synchronously
  release the prepared microphone/camera; cleanup depended on a subsequent retry or exit.
- Android requests camera/audio permissions and validates WebView origins; these source
  paths do not prove that permissions are granted on the affected phone.

These are reproducible code defects/risk paths, not a proven single root cause of the
reported Android incident. Runtime device diagnostics and two-account production reception
remain necessary. RTP errors cannot be assigned to ICE, codec, permissions or rendering
without correlated runtime evidence.

## Changes

1. Render an escaped audio host placeholder in server HTML before any script or connection.
2. Initialize audio/video layout before connecting.
3. Add a nonce-authorized, independent bootstrap error/timeout message with a working exit.
   It does not log exceptions, credentials or user content. No automatic POST-ticket reload.
4. Release captured resources and disconnect on token/signaling failure and late exit.
5. Bump room asset cache query to media-2; the diagnostic protocol remains media-1 for
   compatibility with the existing server allowlist.
6. Add a Django audio HTML regression test and Chromium startup failure tests in CI.

No financial logic, migrations, production settings or Android signing keys changed.

## Validation and limits

- Baseline focused Node media tests: 8 passed.
- Baseline focused Django live tests: 21 run, 3 PostgreSQL-specific tests skipped under SQLite.
- Django system check and JavaScript syntax check passed after changes.
- Local Chromium launch blocked by environment: socket() Operation not permitted. Both the
  existing actual-SDK capture test and new startup test must run in GitHub CI.
- New startup tests cover initial audio UI without JS, missing SDK/media/stage assets and
  stopping real synthetic tracks when the token endpoint fails.
- Synthetic browser tracks are not physical Android camera/microphone or mobile-network proof.
- No production shell/session was available during this audit. Container state, runtime
  environment, ICE candidates, RTP statistics, database backup and deployment not verified.
- No two-account production media test, physical Android test or release-signed APK delivery.
- Full pre-broadcast redesign, dynamic administration and other requested product phases
  are not completed by this focused incident patch.

## Deployment gate

Run CI including PostgreSQL concurrency and SFU tests. Back up the current production
checkout/configuration and database before deployment, retain the prior image for rollback,
then deploy the reviewed commit through the established procedure. Verify the exact served
revision and reproduce on the affected Android phone with a second account on a separate
network. Record decoded video dimensions, inbound RTP byte growth, audible microphone,
camera switching, audio-only start, reconnection and resource shutdown. Do not mark the
incident resolved based only on a green build or HTTP 200.
