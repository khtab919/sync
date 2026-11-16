# Relaxia Stripe Checkout integration

## Approval comes before live payment acceptance

Stripe categorizes third-party creator platforms involving tips/exclusive digital content and virtual credits/stored-value products as **restricted** categories. Submit the real Relaxia business model (18+ age policy, non-explicit content, live gifts, coin top-ups, creator payouts, and paid streaming) to Stripe for confirmation. **Do not misrepresent the service or attempt to evade review.** Adult sexual live chat/content is not supported by Stripe.

If Relaxia is distributed via Google Play, the purchase of in-app coins normally requires Google Play Billing unless a permitted alternative-billing/external-offer program applies. This integration is for eligible distribution channels (such as the website or a direct-distribution Android release) and must be enabled only when platform and payment-provider conditions allow it.

## Existing integration

- The user chooses a package in the **existing** Coin Shop and Stripe Visa/Mastercard method when enabled.
- `POST /api/topup-quotes` (authenticated) creates an immutable 30-minute quote using `method=stripe`.
- `POST /api/stripe/checkout` (authenticated) creates a Stripe-hosted card Checkout Session with the server-authoritative USD amount and metadata; it uses an idempotency key stable per quote.
- `GET /api/stripe/status/<uuid:quote_id>` (authenticated) returns locally verified state; **it does not trust the return page**.
- `POST /api/stripe/webhook` (Stripe signed) verifies the raw-body signature, timestamp, quote/session ID, metadata, USD amount, provider mode, environment and paid status; only then credits the wallet and promotional points in a single database transaction. Repeated events do not credit twice.
- `GET /api/stripe/return` is informational only. A user navigating there by hand never receives coins.
- Refund and dispute events flag the payment for financial review; an administrator must reconcile the wallet and check possible negative exposures.

## Enable on your own server (only after Stripe review/approval)

Edit **`/opt/layali/backend/.env`** privately as root, keeping previous secrets and names intact:

```dotenv
STRIPE_ENABLED=0
STRIPE_SECRET_KEY=sk_test_REPLACE_WITH_YOUR_REAL_TEST_KEY
STRIPE_WEBHOOK_SECRET=whsec_REPLACE_WITH_YOUR_ENDPOINT_SECRET
```

Configure the actual Checkout webhook in [Stripe Workbench](https://dashboard.stripe.com/workbench/webhooks) pointing at:

`https://165-227-128-113.sslip.io/api/stripe/webhook`

If using a different API hostname, replace the hostname. Subscribe at minimum to:
- `checkout.session.completed`
- `checkout.session.async_payment_succeeded`
- `checkout.session.async_payment_failed`
- `checkout.session.expired`
- `charge.refunded`
- `charge.dispute.created`
- `charge.dispute.closed`

The `STRIPE_WEBHOOK_SECRET` value starts with `whsec_` and is **not** the secret API key. For production, use `sk_live_...` with the signing secret of the live endpoint; test and live secrets are not interchangeable. Never paste either secret in ChatGPT, Android code, a Git commit, or a public log.

After Stripe accepts the business and the correct keys and endpoint are configured, switch `STRIPE_ENABLED=1`. If Stripe hasn't confirmed eligibility, keep it 0. For a direct-distribution app, confirm its billing channel is permitted in the countries served.

```bash
cd /opt/layali
git fetch origin fix/live-controls-reels-20261116
git switch fix/live-controls-reels-20261116
git pull --ff-only origin fix/live-controls-reels-20261116
bash backend/deploy.sh 165-227-128-113.sslip.io
cd backend
docker compose exec -T api python manage.py check
curl -fsS https://165-227-128-113.sslip.io/health/
```

The deploy script makes a database backup before migrations. It must be scheduled when active live streams can be interrupted. Do **not** run `docker compose down -v`.

## Testing, state, failure and reconciliation

```bash
cd /opt/layali/backend
docker compose exec -T api python manage.py test core.test_stripe_checkout --verbosity 2
```

Create a test-mode checkout in the Coin Shop. Stripe webhook deliveries should return HTTP 200 after a valid event; the wallet remains unchanged if `payment_status` is unpaid or signature/amount doesn't match. Check the provider Event/Payments dashboards if payment appears successful but funds don't credit. Webhook retries, a stable session ID, unique charge ID, row locks and per-quote promotional grant references prevent repeated credits. If the webhook arrives before the session ID is stored, Stripe receives an error and retries.

On refunds/chargebacks, `review_required=true` is set for manual reconciliation; coins aren't automatically clawed back (they may already be spent). Do not claim full automatic reversal or fraud protection, and establish a refund/dispute operating procedure before live launch.

**There is no live key in this code. Integrating the feature does not create or verify a Stripe account, enable card processing, deploy to DigitalOcean, or build a signed APK.**
