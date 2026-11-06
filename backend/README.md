# Layali self-hosted migration

This branch adds a self-hosted Django API, PostgreSQL, and Caddy HTTPS. It is a migration candidate, not a completed production cutover. No demo accounts, seeded services, or seeded balances are created. Existing Firebase and local accounts are not imported automatically.

## On the DigitalOcean server

1. The configured default is `165-227-128-113.sslip.io`, a free IP-based DNS hostname. No domain purchase or DNS account is needed. Alternatively point your own API domain's A record to `165.227.128.113`. Open inbound TCP 80 and 443 in the provider firewall. Keep PostgreSQL and the API's port 8000 private; Compose exposes only Caddy.
2. `bootstrap-ubuntu.sh` installs Docker Engine and Compose from the official Docker Ubuntu apt repository if Docker is not installed. It stops rather than replacing an existing incompatible container installation.
3. Update the checked-out branch, then run:

```bash
cd /opt/layali
git pull --ff-only origin feat/layali-adult-non-explicit
cd backend
bash bootstrap-ubuntu.sh 165-227-128-113.sslip.io
docker compose exec api python manage.py createsuperuser
curl --fail https://165-227-128-113.sslip.io/health/
```

The default origin is also configured in gradle.properties and the APK workflow. A free third-party hostname depends on that DNS service and the IP remaining available; it is not a purchased domain owned by the user. The deployment checks DNS before starting containers.

The deployment generates database and Django secrets on the server, in an ignored `.env` with private permissions. It does not overwrite existing secrets, delete volumes, remove other applications, or reset existing data. It runs migrations before bringing up the proxy. HTTPS issuance requires working DNS and reachable ports. The final health request must succeed before calling the deployment operational.

The administrator is created interactively, with no default password. Use `/admin/` to review services and reels, grant creator status after checking identity, review reports, and disable accounts via Django's `is_active`. Financial records are read-only in the admin. Identity evidence storage, temporary bans and payout processing still need implementation.

## Android

Build the server-backed native UI with:

```bash
gradle assembleDebug -PLAYALI_API_URL=https://165-227-128-113.sslip.io
```

With this build property, the application selects `ServerAppScreen` as the sole application navigation. It uses server-issued, seven-day opaque bearer tokens stored only in memory, not local administrator credentials. Register with a new username and a password of at least 12 characters. The birth date and policy acceptance are also validated server-side. Google login is not part of this server mode.

The server mode includes accounts, approved services, unpaid booking requests, free text chats, identity-gated reels (opened in the external video handler), reports, balance reads and withdrawal reservations. This is an initial functional interface; full parity with the previous visual design, profile settings, paid posts, realtime chats, media uploads and playback remain work. Legacy Firebase screens and dependencies remain in the repository for migration reference, but the launcher no longer routes to them and automatic Firebase initialization is removed. A build without an API URL shows a configuration notice and cannot sign in or access data. This branch does not claim to remove the unused Firebase dependencies from the APK yet.

## Money and privacy

Prices and balances use integer USD cents. Booking prices come from the service stored in the database, never from the buyer's input. Requests remain `awaiting_payment`; a client cannot mark them paid or confirmed. No endpoint credits funds. Paid chat is rejected until a verified payment integration exists.

Withdrawal requests reserve available funds under a PostgreSQL row lock, reject overspending, and use a caller-supplied UUID for retry deduplication. This does not transfer money. There is no payment-provider webhook, earnings settlement, creator 80% ledger or receipt issuance yet. Do not process real money until these are implemented and reviewed. No 1–24 hour transfer promise is displayed.

Only approved services and reels are listed. All private API endpoints require an active account. Disabling an account invalidates its existing tokens. The API never accepts role, balance, verification status or staff flags from a profile update. Admin and bearer-token authentication are separate. Login and registration are limited to 20 attempts per IP per endpoint per 15 minutes; Caddy overwrites the client-IP header. Run behind Caddy, not a publicly exposed Gunicorn.

## Data migration and backups

Do not delete the Firebase database. Export it from the existing project and inspect its collection schema before creating an import. Preserve original user IDs in a mapping, migrate approved content deliberately, and reconcile balances from payment records; client-written balances and local password hashes are not trusted financial or authentication records. Plan password resets rather than silently importing insecure local credentials. Credentials or exports must not be committed.

Back up PostgreSQL before updates, keep a copy outside the Droplet, and verify restoration into an isolated database before cutover:

```bash
umask 077
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > layali-backup.sql
```

The dump contains private user data. Protect and encrypt the off-server copy. Never run `docker compose down -v` against the live deployment.

## Tests

```bash
DJANGO_SECRET_KEY=test-only POSTGRES_PASSWORD=test LAYALI_TESTING=1 python manage.py test core
```

SQLite tests cover API behavior and validation, not PostgreSQL lock behavior. CI additionally runs against PostgreSQL. APK build verification is separate. HTTPS deployment, external reachability, Firebase import, and payment provider verification cannot be inferred from passing unit tests.

## Reference design update (7.0)

The supplied Wisal native source is the visual reference: warm paper background, sage accents, Noto Sans Arabic local fonts (OFL license included), native five-tab navigation, feed cards, two-column offers, pinned support, profile header and three metallic frames. The reference's fictional people, product inventory, simulated orders and local payment unlocks are not copied. Real accounts, listings, likes, saves, follows, conversations, profile edits, frames and support tickets use the authenticated API. Dark mode is retained as a local appearance preference. Reels now play through native Android VideoView, subject to device codec support and identity approval. Media upload and paid entitlements are still outside this change.

Before installing the 7.0 APK, update the server (additive migrations; do not remove volumes):

```bash
cd /opt/layali
git pull --ff-only origin feat/layali-adult-non-explicit
bash backend/deploy.sh
```

The redesigned login is the first screen. Registration requires date of birth and explicitly states acceptance of the non-explicit policy; age is validated by the backend. The server account role and identity flags remain administrator-controlled. The Noto font files come from the user-provided archive; see the included OFL license.

## Registration update (7.1)

Registration no longer asks for a date of birth. It requires an explicit 18+ and non-explicit-policy acknowledgement; this is self-declaration, not identity verification. New registrations store no invented birth date. Existing dates remain intact. The API continues to validate birth dates supplied by older app versions. Apply migration 0004 and restart the API before installing 7.1.

## Simplified sign-in (7.2)

Registration requires a name (two characters or more; Arabic, English and spaces supported), a password of at least eight characters, and the explicit 18+ / policy checkbox. Email is optional. No uppercase or symbol rule is imposed; very common passwords remain rejected. Field-specific errors replace the earlier generic password message. The public health response advertises `registration_version: 3`; the app checks this before registration and identifies an outdated server rather than blaming credentials. Existing accounts and old registration clients remain supported.

The native client remembers an opaque seven-day server session using AES-GCM with an AndroidKeyStore key. Ciphertext is stored only in the app's no-backup directory, bound to the API origin; passwords are never stored. Expired/revoked sessions are cleared on 401. An offline restore keeps the token and offers retry rather than requiring credentials again. Logout revokes the server token and clears the encrypted local copy. Device keystore failure leaves the current login usable and reports that it could not be remembered. KeyStore support on physical devices remains a separate device test.

Update the API via the documented `git pull` / `backend/deploy.sh` command before installing 7.2. No new database migration is required beyond 0004.

## Relaxia 8.0

The current product is Relaxia: a non-explicit 18+ social/service platform. Name and a valid email are required when creating an account. Password minimum remains eight characters with the adult/policy acknowledgement; date of birth is not collected. Existing accounts remain intact, including accounts created before email was mandatory. Profile editing now supports updating email and an optional HTTPS avatar image.

Identity verification is not required for reels, creator publication, creator discovery/following or withdrawal reservations. Authentication, administrator creator-role approval, content moderation, account bans and all financial safeguards remain enforced. The legacy identity flag stays in the database to preserve history but is hidden in admin and is not an access gate. The health registration capability is now version 4, so a new app can detect an outdated server.

Only service, session and product categories are accepted. Existing educational-category listings are deactivated by migration 0006, not deleted, and are hidden from active lists. The new rose/plum theme uses native shaded surfaces, press depth and ten ornamental vector frames plus a plain ring; no demo level badges or fictional profile identities are introduced. These are native 2.5D lighting/depth effects, not a 3D scene engine.

Run the same documented `git pull` and `backend/deploy.sh` update to apply migrations 0005–0006 before installing Relaxia 8.0. Database volume, user IDs and saved-session origin remain unchanged. Payments and real transfers are still not implemented.


### Account administration
After deploying migrations, authorize an existing account (exactly one email match required):
```bash
docker compose exec api python manage.py authorize_creator macitcomah@gmail.com
```
This enables publishing and creator discovery; content still requires moderation.
Create a separate administrator interactively with `docker compose exec api python manage.py createsuperuser`. Never place passwords in shell command arguments.
At `/admin/`, Users supports ban/unban and account previews. Bans revoke API sessions and hide creator content from public listings. Wallets supports superuser-only positive credit adjustments in integer USD cents, requiring a reason and storing an immutable administrator audit entry. These adjustments are internal credits, not payment-provider deposits. Financial bookings, withdrawals and audit entries cannot be directly rewritten through admin.


### Native administration — Relaxia 8.2
The ordinary app login opens the native management dashboard automatically for active staff superusers. No management menu is shown to other accounts. Existing administrators created with `createsuperuser` have their app profile and wallet initialized on successful login; the same username/password works in the app. Usernames should be lowercase to match app normalization. New API routes under `/api/admin/` require active staff AND superuser privileges on every request.

The dashboard supports account search/pagination, account previews, ban/unban with API-token revocation, creator publishing/discovery activation, audited USD credit additions with retry idempotency, and approving/hiding offers and reels. It does not grant administrator privileges or impersonate accounts. Payment and payout integration remain separate unfinished features. Deploy migration 0008 before using the new dashboard.


### Relaxia 9.0 — uploads, funding, seller requests and chat
Deploy migration 0009 and rebuild the containers. The API persists uploaded files in the new `media_data` Docker volume; include it with database backups. Proxy/JSON upload limit is 32 MB. Image uploads are decoded, bounded, stripped of metadata and re-encoded as JPEG. Chat/receipt media use expiring signed grants; public offer/reel media are accessible only after approval and while the owner remains active. Per-user media quota is 500 MB. Images/audio are limited to 8 MB; MP4 reels to 20 MB.

USDT BEP20 destination is `0xc35797768180752b67743d791f79f812bb0972d1`; QR encodes this exact address, not the unrelated screenshot address. Funding is manual: users submit amount, unique transaction hash, and uploaded receipt. Administration must verify token, network, successful confirmation, destination and amount on-chain before approving. Approval atomically credits the submitted amount plus the disclosed volume bonus (4% at 25, 6% at 50, 8% at 100 USDT), once only. An uploaded receipt does not prove payment and no automatic blockchain verification is claimed.

Seller applications require a description and administrator approval before offer/reel uploads and publication. Uploaded reels remain pending content moderation.

The chat has a fixed composer, physical left/right bubbles, periodic refresh, image messages and microphone recordings (60 seconds maximum). Calls are explicit external Jitsi room invitations requiring both parties to join; they are not integrated ringing/push calls, and Jitsi may require host authentication. Paid-message settlement and automated withdrawals remain unavailable.

Paid cash-value wheel is disabled by default. The administrator configures cost, weighted prizes and a separate reward reserve from the native dashboard. Odds are shown before a charge; a cryptographic draw, atomic wallet/reserve update and idempotent spin ledger prevent duplicate charge retries. Wheel settings cannot change the quoted cost/odds unnoticed; stale quotes are rejected. No paid wheel is enabled or funded by this deployment. The operator must determine whether and where a real-money chance product may be offered before enabling it.

The floating support chooser opens WhatsApp `https://wa.me/201145781363` or Telegram `https://t.me/RamaNUR`; the user writes and sends the message.


### Relaxia 9.2 — internal two-party calls
Audio/video calls now use a first-party HTTPS room inside an origin-restricted Android WebView; external Jitsi invitations are removed. Native accept/reject, foreground ringtone, microphone/camera consent, mute/camera toggle, speaker routing, hangup and call-history status are implemented. Calls stop when the app leaves the foreground. Closed-app/background incoming calls are not implemented; both participants need this version open.

Migration 0010 adds authorized call sessions, bounded SDP/ICE signals and one-use 60-second room tickets. Room tokens are scoped to one call and an active API session; logout, bans, expiration and participant checks revoke signaling access. Ringing expires after 60 seconds, lost participants after 45 seconds, maximum call duration is two hours. Signaling is deleted after calls end. Media is WebRTC traffic and is not recorded by the app/backend.

Deployment adds coturn 4.18.0 with a private generated TURN authentication secret, expiring per-call credentials, relay bandwidth/allocation limits, and denied private/multicast destinations. Existing account/database secrets are preserved. The relay binds the configured public address on the Droplet using host networking. Allow TCP/UDP 3478 and UDP 49160-49200 in any attached DigitalOcean firewall. The deploy script opens these specific ports in active UFW only; it does not configure DigitalOcean Cloud Firewall. `LAYALI_TURN_HOST`/`LAYALI_TURN_PUBLIC_IP` may be set for a different server before deployment. Calls are unavailable until these settings exist and coturn is reachable.

CI tests participant isolation, role/state changes, consumed/expired tickets, scoped signaling, revocation, missed/stale calls, and origin restrictions. The WebRTC browser smoke test runs two isolated Chromium peers with synthetic camera/microphone devices, forces a local authenticated coturn relay, verifies audio/video RTP bytes and relay candidates, and checks mute/camera/hangup. This does not replace testing two physical Android phones across mobile networks after deployment.


### Relaxia 10.0 — coin shop, queued delivery and disclosed wheel policy
Removes the large wallet-card UI and uses a small balance/coin-shop entry. Adds richer burgundy/rose themes, actual peer usernames, local recording level feedback and single-player voice playback controls. A scoped encrypted local outbox retries messages with stable UUID keys; server uniqueness prevents duplicate send retries. Clock = locally queued, one tick = persisted by server, two ticks = explicit recipient-device acknowledgement. This is delivery, not a claim that a message was read. Outbox metadata and private attachments survive temporary network failures; sending resumes while the conversation is open. Closed-app background delivery still requires future push/work integration.

Coin accounting is explicit: 100 coins equal USD 1 of internal balance; existing integer wallet cents are the coin units, no existing money is rescaled. A purchase creates a server-owned quote expiring after 30 minutes; re-opening the same unexpired purchase does not reset it. Destination/currency/rate/bonus are snapshotted, including its QR. Late receipts remain reviewable instead of forfeiting actual transfers. USDT stays BEP20 to the provided 0x address. Sham Cash destination `42c9f2bd84250c5ab6c444616b8ef0c2` was decoded from the user's QR, defaults to USD/manual verification, and is editable in native administration. No official Sham Cash API automation is claimed.

Requested wheel policy: first price USD 3.50, +0.50 per subsequent spin capped at 10.00, first paid spin guarantees a USD 5 prize, subsequent weights are 0/1/3/5/10/50/100 USD with integer weights 500/120/100/160/100/18/2. The USD 100 prize is excluded until prior cumulative wagers reach USD 200, and then becomes eligible; reaching the threshold never guarantees that prize. Actual current price, first-spin guarantee, increase/cap, eligibility threshold and conditional odds remain available before each charge. Old app versions cannot wager without acknowledging rules version 2. Operator activation and funded reserve are preserved; no reserve money is invented or seeded. Migrations 0011/0012 apply the new policy/configuration while preserving wallets and history.

### NOWPayments hosted gateway
The coin shop includes an optional hosted NOWPayments invoice rail. Merchant API/IPN keys remain on the server and are not shipped in Android. After deployment run `bash backend/configure-nowpayments.sh` in the server checkout, paste credentials only into its hidden console prompts, and test a real invoice from the merchant account. Alternatively configure `NOWPAYMENTS_API_KEY` and `NOWPAYMENTS_IPN_SECRET` in `backend/.env` and recreate the API container. Gateway credentials and a live merchant payment are not available to this agent; no live activation, approval, or transaction is claimed.

Invoices bind a private order UUID to an immutable server quote, snapshot amount/bonus, owner and invoice id. Only merchant-signed HMAC SHA-512 IPN data with matching order/price/currency and a completed `finished` payment with sufficient `actually_paid` can credit once. Pending/confirming/underpaid/forged/wrong-order callbacks never credit. Return/cancel URLs are informational and cannot affect money. Reused invoice creation returns the existing checkout; uncertain creation outcomes require review instead of automatically creating another invoice. Later refund/conflicting-payment events are flagged for operator review, never silently credited twice. IPN endpoint is `/api/nowpayments/ipn`; it verifies locally and avoids remote API requests inside the callback.

Protocol references: https://documenter.getpostman.com/view/7907941/S1a32n38 and https://nowpayments.zendesk.com/hc/en-us/articles/21395546303389-IPN-and-how-to-setup . Provider merchant eligibility, fees, minimums and production credentials are controlled by NOWPayments; do not assume a successful mock integration means a live merchant account is approved.
