# Layali creator platform — deployment and acceptance

This branch replaces the entry point with a four-section Arabic UI: published offers, public reels, wallet/creator earnings, and account. Legacy Compose screens remain in source for migration/reference but are not reachable. Do not re-enable the old local-auth, simulated chat, wallet, payment QR or catalog flows. Legacy balances and password hashes are deliberately not trusted or migrated.

## Implemented behavior

- Firebase email/password authentication and email password recovery. No identity document is requested to watch public reels (including guests).
- Larger typography, 54dp primary actions, secondary actions in detail dialogs; three static vector frames: platinum, sapphire and imperial gold/amethyst.
- Actual uploaded video playback; only the current reel owns a player, paused in background and released on disposal. No artificial engagement statistics.
- Customer deposits remain pending until an administrator has checked the external transfer. Deposit references cannot be credited twice.
- Gifts debit the sender and credit 80% to the creator atomically; platform share is the remainder. All monetary values are integer USD cents. Sub-cent rounding goes to the platform. No client can set balances.
- Creator withdrawal requests atomically reserve available earnings. Cancellation/rejection returns the reserved amount once. Approval moves to processing; payment requires a reference and uploaded receipt. Payout does NOT invoke a bank or crypto transfer: an authorized operator transfers externally, then records proof.
- The UI displays an expected 1–24 hour arrival window, computed from server submission time, and an overdue notice. It is an operational SLA, not automatic settlement or a guarantee.
- Administrator warnings, temporary/permanent suspension, restoration, creator-role assignment and deletion are enforced by backend account checks and audited. Temporary suspension expires by server time. Deletion is blocked while any funds remain, removes Firebase Auth identity and tombstones the account; financial/audit records remain. This is not a full personal-data erasure workflow.
- Live listeners refresh profile and balance changes. Finance/history and admin lists refresh after actions or via refresh, capped at the latest 50; reels are capped at 20. Pagination is a follow-up requirement before larger launch.
- Offers and booking requests use real data. Requests remain pending until an administrator checks availability; no debit or fabricated confirmation occurs. Scheduling/capacity automation is not implemented.

## Required deployment steps

1. Create a staging Firebase project first. Enable Email/Password in Firebase Authentication and provision Firestore and Cloud Storage. Confirm the Android google-services configuration matches the deployment project.
2. Install Node 22 and Java 21. In `functions`, run `npm ci`, `npm test` and the emulator commands below.
3. Set `FIRESTORE_DATABASE_ID` to the same named database as `app/src/main/res/values/firebase_applet_config.xml`. Callable region is `europe-west1` in both client and backend. Use the project's actual storage bucket.
4. From an authorized Firebase account, deploy `functions:layali`, Firestore rules/indexes, and Storage rules together. These rules intentionally deny legacy client writes; do NOT roll them out to existing users before the corresponding app migration and balance reconciliation.
5. Sign in to create the first account, then grant the administrator's Firebase UID the custom claim `admin: true` using a trusted Admin SDK environment. No embedded email or password grants administrator access. Preserve any existing custom claims. Refresh the session afterwards.
6. In the admin panel set the real Sham Cash and USDT BEP-20 deposit destinations. Activate designated broadcaster accounts with the creator role. No fictitious balance is seeded.
7. The Functions service account requires Storage object access and IAM permission to sign URLs (`iam.serviceAccounts.signBlob`, normally Service Account Token Creator on the signing service account). Signed receipt URLs expire after 15 minutes; playback URLs expire after one hour. Never store those URLs as permanent public links.
8. Test a small real deposit/gift/withdrawal end to end with the operator before accepting real customer money. Upload a real transfer receipt only after external settlement. Agree the staffing needed for the 1–24h SLA.

## Tests

```
cd functions
npm test
npx firebase emulators:exec --project demo-layali --config ../firebase.json --only firestore,auth,storage "npm run test:integration"
```

Coverage includes conservation of cents, 80/20 rounding, concurrent duplicate gifts, concurrent withdrawal limits, cancellation exactly once, role enforcement, temporary suspension, funded-account deletion protection, guest viewing, receipt requirements and direct-write denial.

Android: `.github/workflows/build-apk.yml` assembles a debug APK. Feature branches upload build artifacts only; automatic public release publication has been removed to prevent shipping a backend-dependent build prematurely. Production signing remains unchanged.

## Rollout limitations / next work

No backend deployment, external transfers, production deletion or data migration is performed by creating this branch. Existing user data must be reconciled before switching clients. Legacy local/demo account data must never be automatically treated as money. Chat/old profile galleries are not exposed in the new shell; migrate them deliberately with authenticated backend contracts before restoring them. Public media signed URLs can remain usable until expiry after moderation. Add operational alerting, pagination, content moderation/reports, abandoned-upload cleanup, App Check rollout and account/data export before a broad launch.
