"""Discovery: CCTV Kota Palembang (cctv.palembang.go.id).

Terverifikasi 2026-09-03:
- GET /api/cctv -> JSON daftar kamera (koordinat, kecamatan/kelurahan, cctv_link HLS).
- Stream: https://stream.palembang.go.id/cam{N}/index.m3u8
- Server agresif throttl: kunci 1 detik antar request (read-only, one-shot).
"""
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import RAW_DIR, http_session, log, save_json  # noqa: E402

BASE = "https://cctv.palembang.go.id"


def fetch_all():
    s = http_session()
    r = s.get(f"{BASE}/api/cctv", timeout=20, headers={"Referer": f"{BASE}/"})
    r.raise_for_status()
    data = r.json()
    # struktur: list langsung atau dibungkus; normalisasi
    cameras = data if isinstance(data, list) else data.get("data", [])
    log(f"palembang: {len(cameras)} kamera")
    save_json(RAW_DIR / "palembang.json", cameras)
    time.sleep(1)  # hormati rate limit; jangan gentayangi server publik
    return cameras


if __name__ == "__main__":
    fetch_all()
