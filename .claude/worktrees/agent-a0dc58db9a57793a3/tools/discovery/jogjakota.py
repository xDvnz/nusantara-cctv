"""Discovery: CCTV resmi Kota Yogyakarta (cctv.jogjakota.go.id).

Prosedur (terverifikasi 2026-09-03):
- GET /home/getdata (XHR) -> JSON publik: title, HLS url, koordinat, kecamatan/kelurahan.
- Stream: https://cctvjss.jogjakota.go.id/atcs/ATCS_*.stream/playlist.m3u8 (tanpa sesi).
- cctv_status: 0=public, 1=private, 2=maintenance -> hanya 0 yang masuk katalog.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import RAW_DIR, http_session, log, save_json  # noqa: E402

BASE = "https://cctv.jogjakota.go.id"


def fetch_all():
    s = http_session()
    r = s.get(f"{BASE}/home/getdata", headers={
        "X-Requested-With": "XMLHttpRequest",
        "Referer": f"{BASE}/",
    }, timeout=20)
    r.raise_for_status()
    cameras = r.json()
    public = [c for c in cameras if str(c.get("cctv_status")) == "0"]
    log(f"jogjakota: {len(cameras)} total, {len(public)} publik")
    save_json(RAW_DIR / "jogjakota.json", public)
    return public


if __name__ == "__main__":
    fetch_all()
