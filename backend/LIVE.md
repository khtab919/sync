# Live broadcasting and service payments

The SFU is separate from Django/PostgreSQL. It forwards encrypted live media and does not run an egress, ingress, recorder, DVR, replay, clip or archive service. No media volume is mounted into the SFU. Its root filesystem is read only; `/tmp` is a bounded RAM filesystem. Only operational/financial metadata is retained. Live comments are deleted when the session closes. No broadcast audio/video enters Django storage or the Android gallery.

## Deploy

Back up the existing database and ordinary uploaded-media volume before applying migrations. In `/opt/layali`, pull `feat/layali-adult-non-explicit` and run `bash backend/deploy.sh`. Existing merchant/database secrets are preserved. The deploy script generates private SFU keys/configuration and self-hosts a pinned browser transport SDK after verifying its SHA-256.

Allow TCP 7881 and UDP 50000–50100 in the cloud firewall in addition to existing HTTPS and call-relay ports. SFU port 7880 is not published. `/livekit/*` passes an application authorization check before every signaling connection. The check validates the user/session, admission, payment entitlement and any ban; a shared room URL grants no access. Rooms cannot be automatically recreated by a client. Ending a session invalidates admission and deletes the SFU room; failed transport cleanup is retried by the maintenance worker.

New sessions have at most 25 viewers by default, with a limit of five simultaneous rooms. These are conservative configuration limits, not a capacity guarantee. Change `LAYALI_MAX_LIVE_VIEWERS`/`LAYALI_MAX_LIVE_ROOMS` only after measuring the actual production machine, codecs, network and expected concurrency. A multi-node rollout needs shared Redis routing, load balancing and additional operational validation; it is not enabled by this single-node configuration.

## Payments and refunds

The displayed entry price and minimum service duration are fixed before joining. Admission reserves available balance. Revenue is settled only after the server observes the viewer connected while host media is available for the displayed duration; the creator receives 80% and the platform 20%. The server does not infer delivery from phone network speed. This measures availability of the media service, not whether a person watched every frame.

Reconnect to the same stream/admission does not charge again. An early end refunds held admissions. An admission that never connects is released after 90 seconds. An SFU outage lasting over 120 seconds closes the session and releases unearned holds. Gifts, when explicitly enabled, require price confirmation and are final after successful sending. Service bookings hold balance until buyer completion; rejection/cancellation before acceptance releases the hold, and unaccepted bookings expire after 24 hours. Accepted-service disputes require administrator review. External withdrawals are manually executed and require a verified transfer reference; the app does not initiate bank/crypto payouts.

## Moderation and privacy

Approved adult creator accounts with broadcasting permission may publish. General users subscribe only. A staff account with the `core.moderate_livebroadcast` permission can moderate broadcasts without account-credit/payment administration; full administrators manage finance and roles. A moderator is an actual visible participant, not a hidden recorder. Without recorded media, later reports cannot be investigated using video replay; operational reports, live moderation and financial records remain available.

Android uses the secure-window flag during live viewing and exposes no download or recording controls. This does not guarantee prevention of external recorders, modified clients or cameras.

## Validation

Unit/integration checks cover free/private/invitation access, short-lived grants, revoked/ended sessions, real SFU participant counts, escrow, 80/20 settlement, refunds and permissions. The CI media fixture starts a real SFU and authorization gateway and receives RTP in twelve isolated browser viewers using synthetic media; it checks denied unpaid/forged access, reconnect idempotence, comments and teardown. The fixture does not exercise real phones, mobile carrier networks or production-scale capacity. Deployment is not considered operationally ready until those checks and a financial smoke test pass on the actual server.
