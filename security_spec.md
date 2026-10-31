# Security Specification (Payload-First Security TDD)

## 1. Data Invariants
1. **Multi-Tenant User Ownership Isolation**: Every document in `/users/{userId}`, `/users/{userId}/bookings/{bookingId}`, `/users/{userId}/reviews/{reviewId}`, and `/users/{userId}/favorites/{venueId}` must belong exclusively to `request.auth.uid == userId` and have `data.userId == request.auth.uid`.
2. **Account & Password Hash Integrity**: Every `/users/{userId}` account document stores only salted PBKDF2-HMAC-SHA256 hashes (`passwordSaltHex` and `passwordHashHex`)—never plaintext passwords—and enforces strict bounds on `level`, `xpPoints`, `nextLevelXp`, `walletBalanceUsd`, and `chatPriceUsd`.
3. **Strict Schema & Size Bounds**: Every string field has explicit minimum and maximum `.size()` bounds; every integer field (`guestCount`, `totalPriceUsd`, `rating`, `level`, `xpPoints`, `walletBalanceUsd`) has strict numeric range constraints; no unexpected/ghost fields are permitted (`keys().hasOnly(...)`).
4. **Temporal Integrity**: `createdAt` and `updatedAt` must be server timestamps (`is timestamp && <= request.time`) and `createdAt` is strictly immutable on updates.
5. **Terminal State Locking**: A `Booking` with status `'completed'` or `'cancelled'` is in a terminal state and cannot be updated further (`existing().status == 'confirmed'`).

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read/Write**: `auth = null` attempting `get`, `list`, or `create` on `/users/alice` or `/users/alice/bookings`.
2. **Cross-User Read/Write (PII Blanket Test)**: Authenticated `bob` attempting to read or write `/users/alice` or `/users/alice/bookings/b1`.
3. **Identity Spoofing on Create**: Authenticated `alice` creating `/users/alice` or `/users/alice/bookings/b1` with `userId: "bob"`.
4. **Shadow/Ghost Field Injection**: Creating or updating a user account or booking with an extra field `"isVipAdmin": true`.
5. **Terminal State Bypass**: Attempting to update a booking whose `existing().status` is `'cancelled'` or `'completed'`.
6. **Immutable Field Mutation**: Attempting to mutate `createdAt` or `userId` on an existing `/users/{userId}` or booking document.
7. **Out-of-Range Rating**: Creating a review in `/users/alice/reviews/r1` with `rating: 6` or `rating: 0`.
8. **Resource Exhaustion (1MB String)**: Creating a review or user bio with 2,000 characters (`> 500` limit).
9. **Path ID Poisoning**: Creating a document with invalid ID characters or length `> 128`.
10. **Future Timestamp Spoofing**: Submitting `createdAt` set in the future (`> request.time`).
11. **Unfiltered List Query**: Querying `bookings` without matching `userId` filter when cross-tenant.
12. **Value Type Poisoning**: Updating `level` or `guestCount` to a string `"four"` instead of an `int`.

## 3. Red Team Audit & Conflict Report
- **Identity Spoofing**: Blocked by `request.auth.uid == userId && incoming().userId == request.auth.uid` and `incoming().userId == existing().userId`.
- **PII & Password Hash Isolation**: `/users/{userId}` restricts `get` and `list` strictly to `request.auth.uid == userId || isAdmin()`, preventing cross-user leaks of email or password hashes.
- **State Shortcutting / Terminal State**: Blocked by `existing().status == 'confirmed'` gate on booking updates and strict enum checks `incoming().status in ['confirmed', 'completed', 'cancelled']`.
- **Resource Poisoning**: Blocked by `isValidId()` regex/length guard and `.size()` limits on all strings.
- **Value Poisoning**: Blocked because `isValidUserAccount(incoming())`, `isValidBooking(incoming())`, and `isValidReview(incoming())` wrap the entire `allow update` expression.
