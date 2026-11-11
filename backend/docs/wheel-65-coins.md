# Relaxia — يا نصيب / 65 coins

## Single pricing unit

The existing `Wallet.balance_cents` is **internal coins**, not a USD amount inside the wheel.
The existing top-up system uses the conversion **100 coins = 1 USD equivalent**
in the purchase catalog; this migration does not change the conversion or real-money
payment processing.

A spin debits **65 coins**. There is no increasing price, no first-spin guarantee,
and no prize locked behind accumulated spend. The wheel remains disabled until an
authorized administrator enables it and adequate funds are available. No reserve,
prize money, account balances, or historical `PaidSpin` entries are created or reset.

| Prize (coins) | Weight / 1000 | Published chance |
| ---: | ---: | ---: |
| 0 | 500 | 50% |
| 13 | 250 | 25% |
| 39 | 150 | 15% |
| 130 | 80 | 8% |
| 650 | 18 | 1.8% |
| 1300 | 2 | 0.2% |

Expected award = **33.8 coins** per spin; expected gross contribution =
`65 - 33.8 = 31.2 coins`, i.e. **48%** before operational expenses,
payment processing, incentives, tax, loss reserves, and other costs. These are
statistical expectations, not guaranteed outcomes or operating profit.

## Server authority and validation

- Migration `0034_wheel_65_coins` changes the wheel rule configuration,
  **including previously edited wheel prize rules**. It preserves `enabled`,
  `bankroll_cents`, `daily_limit`, `daily_budget_cents`, and historical spins.
  Operator should review whether an active session is in progress before deploying.
- `GET /api/promotion/wheel` presents the server-defined cost, prizes and
  chance percentages to the Android app. `POST` requires the rules version,
  a fresh quote version and an idempotency key; results are drawn with server
  cryptographic randomness and financial updates are transactional.
- When the server cannot cover the maximum possible prize or hits the daily budget,
  new spins are unavailable; the interface must not offer an unusable paid action.
- Keep payment, withdrawals and the platform's local legal/age requirements under
  review because purchasable or redeemable virtual coins may be regulated as gaming.

## Safe deployment

From the authenticated deployment shell at `/opt/layali`, run the existing
`backend/deploy.sh` process targeting branch
`fix/relaxia-wheel-65-coins-20261111`. It must back up the live database
and apply migrations before promoting app clients. Do not change the existing
Android `applicationId`: `com.aistudio.layalilounge.x9m4kp`.

The 11.4 APK build is deliberately blocked until the **original 11.3 signing key**
is restored as `RELAXIA_KEYSTORE_BASE64`; generating a different signing
certificate would break in-place updates and risk local data if users uninstall.
See `backend/docs/android-signing-continuity.md`.

Validation checklist: verify `GET` returns 65 coins and the above odds, verify
balance decreases by 65 at most once on retry, verify prize settlement, verify
no-cost change after subsequent spins, confirm unavailable states and test
installing over a signed 11.3 APK without losing local data.
