# Sync backend — Railway foundation

This is a minimal HTTP service, **not a finished production API**. It has no payments, balances, withdrawals, user administration, or database access.

## Railway deployment
1. Create a Railway project and choose **Deploy from GitHub repo** (`khtab919/sync`).
2. Select branch `feature/railway-backend-foundation` for testing (or merge the PR and use `main`).
3. Set **Root Directory** to `/backend`. Railway can build using the Dockerfile in that directory.
4. Generate a public domain in Railway and call `GET /health`. Expected JSON: `{"status":"ok","service":"sync-backend"}`.
5. Configure authentication and secrets on the server **before** exposing business endpoints. Never commit private keys to GitHub.

## Current Android architecture
The app is Kotlin/Jetpack Compose and currently includes Firebase Auth, Firestore and local Room storage. Do **not** migrate data to PostgreSQL without auditing security rules, data models and production data. Firebase can stay as the existing system while a server gradually takes over privileged operations.

## Required next milestones
- Validate Firebase ID tokens on each authenticated endpoint using server-side Admin SDK and enforce role permissions.
- Implement immutable, idempotent financial ledger, payment verification via trusted provider, and server-only balance mutations.
- Implement the broadcaster's 80% earnings share and withdrawal requests with auditable approval/payout states. Display 1–24 hours only as a target, not a guaranteed settlement.
- Add admin suspension/ban and appeal/audit records, rate limits, request validation, secure secrets, backups and monitoring.
- Integrate Android with the deployed API and test end-to-end.

**Important:** A successful /health check proves only that the service is online, not that banking or live-video features work.
