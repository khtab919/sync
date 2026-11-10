# Relaxia 11.0 — implementation and activation

Base: a533e621e68717c9cae80b74ba3bc6ffb726fd05. Work branch: fix/relaxia-comprehensive-20261110.

## Implemented

- Durable, account-bound Android push outbox with bounded retry, expiry, invalid-device cleanup and notification channels. Messages, calls, bookings, withdrawal/top-up status and public followed live sessions enqueue generic private alerts. Logout deregisters the current token; account security actions revoke all devices.
- Native permission request, account validation before displaying a push, generic lock-screen text, deterministic IDs, destination routing and notification settings.
- Active media foreground service. Backgrounding no longer explicitly ends the call/live screen; video is disabled while hidden. Process death still ends the media session; no background auto-recording is introduced.
- Keyset pages for messages/reels/services/conversations, server search, unread counts, real attachment previews and bounded conversation queries. Client loads older messages and additional content.
- Reels save/hide controls, search captions, app share links and authenticated capped watch telemetry. Telemetry is informational, never used to award money or XP.
- Resumable bounded chunks with checksum, ownership validation and retry idempotency. Native upload streams to a private temporary file, keeps only one chunk in memory and reports progress. Existing 20 MiB reel / 8 MiB other limits remain. Production ffprobe validates streams and duration; no claim of multi-resolution transcoding is made.
- Direct authorized receipt lookup independent of recent-history limits.
- Password recovery with single-use 30-minute code through configured SMTP; password change/deletion revokes sessions. Deletion deactivates and hides the profile while retaining financial/moderation records; nonzero balance/open bookings block deletion.
- Password confirmation for email changes; account mutation throttling.
- Permanent capped XP awards, default-only frames for new accounts and migration preserving existing XP/frames. Publishing alone no longer farms levels.
- Per-conversation encrypted inbox cache, composite message indexes and build revision metadata.
- Read-only release diagnostics, transient-data cleanup and a database backup before migrations.

## Required operator configuration

Use the SAME Firebase project as the Android google-services.json used by the build. Do not paste service-account credentials into GitHub source, chat, Android resources or the APK.

1. Put the Google service-account JSON at backend/secrets/firebase-service-account.json on the server, readable only by the deployment owner and container UID 10001. Configure FCM_PROJECT_ID and GOOGLE_APPLICATION_CREDENTIALS=/run/secrets/firebase-service-account.json in backend/.env. Grant only the permission needed to send FCM messages. The worker has a read-only secrets mount.
2. Configure EMAIL_HOST, EMAIL_PORT, EMAIL_USE_TLS, EMAIL_HOST_USER, EMAIL_HOST_PASSWORD and DEFAULT_FROM_EMAIL. Test an actual recovery message.
3. Review and deploy this branch using backend/deploy.sh with the existing domain. Keep the generated database backup. Run `docker compose exec -T api python manage.py release_check` and inspect the push worker logs without dumping credentials.
4. Install the APK signed with the existing application's signing key. A newly generated debug key cannot update an installation signed with another key; do not uninstall merely to work around this during a user-data migration.
5. Schedule `python manage.py cleanup_transient` daily in the existing operations scheduler.

## Acceptance checks requiring real infrastructure/devices

- Android 13+ permission accepted/denied; locked phone; app background/process killed; force-stop distinction; Wi-Fi/mobile handover; Samsung/Xiaomi power management.
- Incoming call notification without silently accepting; stale calls do not ring after expiry. Logout and account switching must never reveal another account's notification.
- Live audio/camera permissions, guest microphone permission escalation, first decoded camera frame, background audio, camera restoration and termination. Test two phones and TURN.
- PostgreSQL concurrency, duplicate gifts/charges/uploads, migration from the deployed revision and rollback rehearsal using a restored backup.
- Genuine FCM and SMTP delivery, not merely a queued row.
- CPU/memory and network load at the intended number of viewers; existing viewer/room limits were not increased blindly.

## Boundaries

This branch does not claim production deployment, successful FCM delivery, successful email delivery or real-phone background reliability without the checks above. Automatic video transcoding/CDN, advanced interest recommendations, scheduled live reminders, visual redesign of every screen, store reviews and full native media-service ownership remain separate product work; the core fixes here do not pretend to implement those features.
