# Relaxia Official Website

Independent mobile-first Arabic landing page and isolated SQLite database for **approved APK release metadata only**. No changes to Android package, signing, app database, live server or existing Docker services.

## Local
From website/: RELAXIA_SITE_DB=./site.sqlite3 python3 server.py
Visit http://localhost:8080. The APK download remains unavailable until an actual release is approved.

## DigitalOcean deployment
Copy website/ to /opt/relaxia-website (a NEW isolated directory, not /opt/layali):
1. Set a long unique server-side secret: export RELAXIA_SITE_ADMIN_TOKEN='A_RANDOM_VALUE_AT_LEAST_24_CHARS'
2. Run: docker compose up -d --build
3. Check: curl -f http://127.0.0.1:8088/api/healthz
4. Point a dedicated domain or subdomain with HTTPS using an existing Nginx/Caddy reverse proxy to http://127.0.0.1:8088

The port binds only to 127.0.0.1; do not replace or overwrite the Android stack or live streaming setup.

## Publish a verified release
After verifying APK identity, installability, signature, and SHA-256, call the authenticated POST /api/admin/release endpoint from the server. JSON example:
{"version":"11.7.x","url":"https://github.com/khtab919/sync/releases/download/VERIFIED_TAG/VERIFIED.apk","sha256":"REPLACE_WITH_REAL_64_CHARACTER_LOWERCASE_SHA256"}
This is an example only and must not be posted until the file is actually verified.
Authorization: Bearer $RELAXIA_SITE_ADMIN_TOKEN; Content-Type: application/json.

The web database is a dedicated persistent SQLite volume /data/site.sqlite3 (table releases). It is NOT the app's Firebase or production users database. No contact form, tracking, analytics, fake ratings or placeholder active download buttons are present. The admin token must never appear in the frontend.

Deployment to a real host requires deployment access, domain and TLS setup. Publishing this branch to GitHub by itself does NOT deploy the running Docker service.
