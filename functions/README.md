# Layali trusted backend (candidate, NOT PRODUCTION READY)

The Firebase Cloud Functions in `index.js` are the **only authority** for money, bookings, payouts, chat entitlements, and confirmed message delivery. Android and Firestore rules cannot credit funds or mark transactions verified.

## Deploy prerequisites

1. Set up Firebase project, enable Firebase Authentication, Firestore named database `ai-studio-android-nightlyc-90974dd0-99e1-42a7-a4b5-a3bd95657476`, Cloud Functions (Node 22), Cloud Storage and a real private receipt bucket.
2. Ensure `firebase.json`, `firestore.rules` and `storage.rules` are deployed **together** with the Functions to the correct Firebase project. The default database/bucket IDs must be verified. The Android app must use the same project ID and Storage bucket.
3. Grant admin permission through a secure, logged server-side Firebase Admin SDK operation with `setCustomUserClaims(uid, { admin: true })` to a specifically verified administrator. A local app session or email match is **not** sufficient. Revoke claims for suspended admins.
4. Operators must independently match a submitted Sham Cash / USDT payment reference **against the actual payment provider records/on-chain transaction** and check amount, recipient, network, payer, currency, and final settlement **before** calling `approvePaymentReceipt`. A screenshot alone never proves payment. The callable is a manual admin approval workflow, **not an automated provider verification**.
5. Only admin claim holders can run `createServiceSlot`, `approvePaymentReceipt`, `rejectPaymentReceipt`, `completeBooking` and `recordWithdrawalPayout`; further operational separation of duties, MFA and audit monitoring should be added before launch. Payout marking requires an independently confirmed external transfer.
6. Populate real published services and admin-defined `service_availability/{serviceId}/slots/{hash}` documents. A booking cannot be submitted for arbitrary dates or unapproved slots. The server quotes the service price and consumes capacity transactionally.
7. Use Firebase Emulator Suite to test security rules and back-end transactions. Run Android tests, profile multi-device chat, inspect release build, complete payment reconciliation and a fraud/security review. Do not enable public money flows or ship until all tests pass.
8. Check product compliance, seller identity verification, moderation, withdrawal policies, receipts retention, refunds/chargebacks, terms, privacy and payment provider permissions; USDT/BEP-20 and Sham Cash require appropriate provider-side procedures.

## Server data

- `wallets/{uid}.balanceCents` — verified purchaser wallet, integer cents.
- `creator_wallets/{uid}` — `availableCents` and `reservedCents`; credited with **80%** at completion, after admin approval.
- `wallet_ledger/{entryId}` — immutable deposits, charges, earnings and payouts.
- `payment_receipts/{uid_submissionId}` — pending until admin verifies provider settlement; rejected receipts never mint money.
- `withdrawals/{id}` — pending reserved amount, paid only after actual payout transfer.
- `chat_threads/{threadId}/messages/{messageId}` — server-verified purchase entitlements and messages.
- `service_availability/{serviceId}/slots/{hash(date:slot)}` — real date/slot capacities.
- `users/{uid}/bookings/{id}` — pending/confirmed/completed/cancelled transitions written by server.

## Known launch blockers

- Payment review is manual; no real payment provider API/webhook is integrated. There is no production reconciliation, webhook signature verification, provider chargeback handling or automatic payout system yet.
- Cloud Functions/rules/storage have NOT been deployed by these commits, nor have transactions been tested against a production Firebase project.
- Refunds of confirmed wallet bookings, seller disputes, moderation dashboards, report flows, and account-level role administration still require end-to-end workflows.
- Real media upload/transcoding/moderation and backend persistence of likes/comments are incomplete; no mock engagement numbers are displayed as live counts.
- No real admin custom claims have been provisioned here. Local admin features should not be treated as server authorization.
- The Android UI does not yet implement all admin approval/payout completion actions. Do not attempt live financial operations with this partial deployment.

All client flows must fail closed on server errors. Do not restore client-side balance mutation, instant local booking confirmation, or receipt-based chat unlocking.
