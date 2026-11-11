# Relaxia 11.6 — safe integration and reliability improvements (11 November 2026)

This branch is based on `feat/relaxia-coin-shop-payment-sheet-20261111`,
not the unverified AI Studio claim about commit `946538c`.
The uploaded archive is a partial file export, not a complete APK or a verified
commit. Do **not** copy the entire archive over this branch.

## Applied improvements

- Home: remove redundant top-level search/filter and account-story carousel;
  show five working shortcuts to livestream, audio lobby, coin shop,
  `يا نصيب`, and services. Leave search in dedicated contexts.
- Move create-content and customer-support actions into the toolbar/menu;
  remove floating controls that obscure feed cards, and omit the legacy
  home logo asset (text title remains).
- Audio room shortcut selects the audio room filter and preselects the
  15-seat audio creation mode. The API remains authoritative about eligibility,
  admission fees and capacity.
- Home live activity is populated by `/api/live` and refreshed while the app is
  foregrounded; unavailable servers are labelled honestly, without fake rooms.
- New chats can find real accounts by name or unique five-digit public ID;
  the public ID is resolved through the server to the internal recipient key.
- Android no longer sets `FLAG_SECURE` during live/voice calls; screenshots are
  permitted as requested, without modifying anyone's saved media.
- Audio room QA checks the seat request by action type so the preceding comment
  and like tests do not incorrectly fail based on event order.
- Calls: scoped `WebChromeClient.onPermissionRequest` may grant an Android-
  authorized subset of microphone/camera resources *only from the trusted
  server origin*; DOM storage is enabled for the trusted WebRTC room.
- Conversations: preview includes `is_mine`, `delivered_at`, and
  `read_at` populated from real messages. The list displays delivered/read
  indicators and unread counts. Regression tests cover both directions.
- Remove remaining old wheel label from mobile navigation; keep old API route
  recognized for backwards compatibility.

## Preserved payment safety

- Do **not** use the ZIP's purported permanent quote modification. Quotes
  expire in **30 minutes**, with payment price/destination taken from the server.
- Manual transfers already sent may still be submitted for late review;
  expiration does not approve or credit a wallet.
- Pay method enablement is controlled by the backend; do not hardcode
  Sham Cash or NOWPayments enabled flags or invent a Google Play rail.
- No wallet amounts, IDs, payment rules, signing secrets, or historical data
  are migrated by these changes.

## Deployment and acceptance

This branch is **not deployed**. It does not itself produce a compatible APK.
Use the existing `backend/deploy.sh` only after a DB backup, review of
`backend/compose.yaml` (service name `api`, not `backend`), and green CI.
The Android release metadata is **11.6 / versionCode 33**. The Android `applicationId` must stay
`com.aistudio.layalilounge.x9m4kp`, and an in-place APK upgrade must use
the **original release signing certificate**.

Before merge, verify Android Kotlin compilation, Django tests, live-room
admission, microphone/camera permission prompts, audio and video calls on
two actual devices, 15-seat audio capacity, purchase order status and
wallet idempotency. Never claim these device/network tests passed unless
they have actually run.
