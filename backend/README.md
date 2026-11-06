# Layali self-hosted migration

This branch adds a self-hosted Django API, PostgreSQL, and Caddy HTTPS. It is a migration candidate, not a completed production cutover. No demo accounts, seeded services, or seeded balances are created. Existing Firebase and local accounts are not imported automatically.

## On the DigitalOcean server

1. Point an API domain's A record to `165.227.128.113`. Open inbound TCP 80 and 443 in the provider firewall. Keep PostgreSQL and the API's port 8000 private; Compose exposes only Caddy.
2. Install Docker Engine and Compose using the official Docker Ubuntu packages. The deployment script requires `docker compose` and Python 3.
3. Update the checked-out branch, then run:

```bash
cd /opt/layali
git pull --ff-only origin feat/layali-adult-non-explicit
cd backend
bash deploy.sh api.your-domain.com
docker compose exec api python manage.py createsuperuser
curl --fail https://api.your-domain.com/health/
```

The deployment generates database and Django secrets on the server, in an ignored `.env` with private permissions. It does not overwrite existing secrets, delete volumes, remove other applications, or reset existing data. It runs migrations before bringing up the proxy. HTTPS issuance requires working DNS and reachable ports. The final health request must succeed before calling the deployment operational.

The administrator is created interactively, with no default password. Use `/admin/` to review services and reels, grant creator status after checking identity, review reports, and disable accounts via Django's `is_active`. Financial records are read-only in the admin. Identity evidence storage, temporary bans and payout processing still need implementation.

## Android

Build the server-backed native UI with:

```bash
gradle assembleDebug -PLAYALI_API_URL=https://api.your-domain.com
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
