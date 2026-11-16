# Relaxia release signing baseline — 2026-11-16

Production application ID: `com.aistudio.layalilounge.x9m4kp`. Never change this to a QA or second-app suffix.

A new PKCS12 RSA 4096-bit keystore has been created and verified for future releases. Alias: `upload`. Its public certificate SHA-256 (lowercase, no separators) is:

`879960ff8f429775285cba8be567a5a1a6f6b5d94d83e264bd67115bd9b9d89c`

Certificate validity: 2026-11-16 through 2054-04-03.

IMPORTANT: This is a **new signature baseline**, not a rotation accepted by Android Package Manager. Devices with old-signed Relaxia installed cannot directly install a same-package APK signed by this new key. Users must first preserve/export their important data, and an explicit reinstall or verified migration procedure is required. No existing user data should be silently removed.

The encrypted keystore and its password are **not** checked into Git. The owner has stored them separately in their private ChatGPT Library under `/Relaxia/Signing/`. For future signed builds, use GitHub Actions private secrets:

- `RELAXIA_NEW_KEYSTORE_BASE64` — base64 of the PKCS12 file
- `RELAXIA_NEW_STORE_PASSWORD` — PKCS12 password

The workflow `.github/workflows/build-installed-update.yml` verifies the original package ID, release version, non-debuggable status, and above certificate fingerprint. It will refuse to publish a signed APK if these private values are not available. As an alternative, the unsigned release from its build artifact can be signed locally with the saved private key, verified with Android `apksigner`, and backed up safely.

For every future version: increment `versionCode`; preserve original `applicationId`; keep the same new certificate; verify the signature and Android installation against a previous APK bearing the new certificate before publishing.
