# Android update identity

Keep `applicationId` fixed at `com.aistudio.layalilounge.x9m4kp`.
The display name Relaxia and source namespace com.example are not install identities.

The delivered APKs have different SHA-256 signing certificate fingerprints:

- 11.3: `eadf736b5beaa7fa9395c633b83ab9a0892126219ef3f05ae59eb511465c501f`
- 11.4: `6ecbfb02875d37721ae417a33088820ffb76835e668d0d68f2a224e758072a5a`

The previous workflow generated an ephemeral debug keystore when no key was present.
An APK contains the public certificate, not the signing private key; it cannot
recover the original keystore. Recover the 11.3 private keystore from the original
build environment or its backup, then store its base64 bytes in the GitHub Actions
secret `RELAXIA_KEYSTORE_BASE64`. Do not commit signing keys or put them in logs.
The current signing configuration expects alias androiddebugkey and the existing
debug passwords; preserve the original certificate identity.

The workflow now stops if the original key is missing or the fingerprint differs.
It also checks the final APK metadata for the fixed applicationId and increasing
versionCode. Until the original key is restored, no compatible replacement APK
can be produced. If the installed APK is a different historic version, compare
its certificate before selecting the compatibility baseline.

Do not advise uninstalling as an automatic fix: uninstalling removes local app
data. A new signing identity would require an explicit migration plan, not merely
changing the package name or retrying installation.
