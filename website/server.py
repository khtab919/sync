#!/usr/bin/env python3
"""Independent landing page + SQLite catalogue of verified APK releases."""
import hmac
import json
import os
import sqlite3
from datetime import datetime, timezone
from contextlib import closing
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

BASE = Path(__file__).resolve().parent
DATABASE = Path(os.environ.get("RELAXIA_SITE_DB", "/data/site.sqlite3"))
TOKEN = os.environ.get("RELAXIA_SITE_ADMIN_TOKEN", "")

def db():
    DATABASE.parent.mkdir(parents=True, exist_ok=True)
    c = sqlite3.connect(DATABASE, timeout=10)
    c.execute("CREATE TABLE IF NOT EXISTS releases (id INTEGER PRIMARY KEY, version TEXT NOT NULL, url TEXT NOT NULL, sha256 TEXT NOT NULL, published_at TEXT NOT NULL)")
    c.commit()
    return c

class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(BASE), **kwargs)

    def log_message(self, fmt, *args):
        # No visitor IP logging.
        pass

    def end_headers(self):
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "DENY")
        self.send_header("Referrer-Policy", "strict-origin-when-cross-origin")
        self.send_header("Content-Security-Policy", "default-src 'self'; img-src 'self' data:; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'none'")
        super().end_headers()

    def response(self, status, data):
        raw = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self):
        if self.path == "/api/healthz":
            return self.response(200, {"ok":True})
        if self.path == "/api/release":
            with closing(db()) as c:
                r = c.execute("SELECT version,url,sha256,published_at FROM releases ORDER BY id DESC LIMIT 1").fetchone()
            if r:
                return self.response(200, {"available":True, "version":r[0], "url":r[1], "sha256":r[2], "published_at":r[3]})
            return self.response(200, {"available":False})
        if self.path.startswith("/api/") or self.path.startswith("/.") or "/." in self.path or self.path == "/Dockerfile" or self.path.endswith((".py",".md",".yaml",".sqlite3")):
            return self.response(404, {"error":"not_found"})
        if self.path == "/":
            self.path = "/index.html"
        return super().do_GET()

    def do_POST(self):
        if self.path != "/api/admin/release":
            return self.response(404, {"error":"not_found"})
        if len(TOKEN) < 24:
            return self.response(503, {"error":"admin_not_configured"})
        if not hmac.compare_digest(self.headers.get("Authorization",""),"Bearer "+TOKEN):
            return self.response(401, {"error":"unauthorized"})
        try:
            length = int(self.headers.get("Content-Length","0"))
            if not 1 <= length <= 4096: return self.response(413, {"error":"invalid_size"})
            obj = json.loads(self.rfile.read(length))
            v,u,s = obj["version"],obj["url"],obj["sha256"]
            if not isinstance(v,str) or not 1 <= len(v) <= 40 or any(c not in "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._-" for c in v): raise ValueError()
            if not isinstance(u,str) or len(u)>1024: raise ValueError()
            parsed=urlparse(u)
            if parsed.scheme!="https" or parsed.hostname!="github.com" or not parsed.path.startswith("/khtab919/sync/releases/download/") or parsed.username or parsed.password or parsed.fragment: raise ValueError()
            if not isinstance(s,str) or len(s)!=64 or any(c not in "0123456789abcdef" for c in s): raise ValueError()
            with closing(db()) as c:
                c.execute("INSERT INTO releases (version,url,sha256,published_at) VALUES(?,?,?,?)",(v,u,s,datetime.now(timezone.utc).isoformat()))
                c.commit()
            return self.response(201,{"saved":True,"version":v})
        except (ValueError,KeyError,TypeError,json.JSONDecodeError):
            return self.response(400,{"error":"invalid_release"})

if __name__=="__main__":
    db().close()
    ThreadingHTTPServer(("0.0.0.0",int(os.environ.get("PORT","8080"))),Handler).serve_forever()
