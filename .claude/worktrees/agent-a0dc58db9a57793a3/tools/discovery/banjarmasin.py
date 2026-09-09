"""Discovery: ATCS Dishub Kota Banjarmasin (atcs.banjarmasinkota.go.id).

Terverifikasi 2026-09-03:
- GET /api/public/cameras/wall -> [{code, name, stream, poster}], tanpa auth.
- Stream: https://atcs.banjarmasinkota.go.id/stream/{code}/index.m3u8 (1280x720).
- Kadang balas [] tanpa Referer -> selalu kirim Referer.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import RAW_DIR, http_session, log, save_json  # noqa: E402

BASE = "https://atcs.banjarmasinkota.go.id"


def fetch_all():
    s = http_session()
    for attempt in range(3):
        r = s.get(f"{BASE}/api/public/cameras/wall", timeout=20, headers={"Referer": f"{BASE}/"})
        r.raise_for_status()
        cameras = r.json()
        if cameras:
            break
        log(f"banjarmasin: percobaan {attempt+1} kosong, ulangi")
    log(f"banjarmasin: {len(cameras)} kamera")
    save_json(RAW_DIR / "banjarmasin.json", cameras)
    return cameras


if __name__ == "__main__":
    fetch_all()
