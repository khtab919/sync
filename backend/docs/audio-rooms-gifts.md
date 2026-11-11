# Audio rooms and gifts — 11.4

Audio rooms use the existing authenticated LiveKit admission and publishing flow.
Hosts choose free or paid entry and 1, 5, 10 or 15 speaking places (including the host).
Video stages retain their 12-speaker cap. Listener capacity is still bounded by
`LAYALI_MAX_LIVE_VIEWERS`; it must allow at least 14 guests for a 15-seat room.
The grid assigns approved speakers in stable seat-record order. An empty seat
opens the microphone request sheet; approval by the host/moderator is required.
This is not a persistent room: normal end/disconnect and no-recording rules apply.

The 0033 data migration adds 21 gifts to the three existing gifts using
`get_or_create`, preserving existing administrator prices and disabled entries.
The resulting default catalog contains 24 gifts from 10 to 25,000 internal credit
units. Whale: 10,000; lion: 15,000. Catalog administration remains available via
`/api/admin/live/gifts`; the server price is authoritative. Effects use the gift
code and the existing simple/animated/depth classification, respect reduced motion,
and never block room controls. Gifts may be sent to the host or approved speakers.

Paid admission reserves credit with explicit price confirmation. Re-entry uses the
same admission. Gifts settle through the existing idempotent financial operation,
with an 80% recipient share. Neither UI animations nor client-supplied amounts
credit accounts. New clients require stage protocol version 3 to avoid offering
15-seat creation against an old server.

Deploy this branch using `backend/deploy.sh` (backs up the database, migrates and
collects static files), then install the 11.4 APK. No production deployment is
implied by a successful CI build.

Validation: backend tests for 15-seat limits, paid audio entry retries, whale/lion
settlement and duplicates; browser tests at 360 and 430 pixels for seat layout,
guest requests, gift tabs and confirmation/cancellation; SFU smoke test renders
15 audio seats with two actual publishers. The existing 12-speaker transport test
continues to run. A simultaneous 15-person production load test is not claimed.
