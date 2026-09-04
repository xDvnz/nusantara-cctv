"""Discovery: CCTV resmi Kota Malang (cctv.malangkota.go.id).

Prosedur (terverifikasi 2026-09-03):
1. GET halaman /sebaran-cctv untuk membangun cookie sesi publik (bukan login).
2. POST /api/v2/get-cameras (form m_kecamatan_id=99) -> daftar kamera resmi.
3. Stream HLS: /cctv-stream/streams/<stream_id>.m3u8 (butuh cookie + referer).
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import RAW_DIR, http_session, log, save_json  # noqa: E402

BASE = "https://cctv.malangkota.go.id"
SOURCE_ID = "malangkota"


def fetch_all():
    s = http_session()
    # bootstrap sesi cookie publik dari halaman resmi
    s.get(f"{BASE}/sebaran-cctv", timeout=15, headers={"Referer": BASE})
    r = s.post(
        f"{BASE}/api/v2/get-cameras",
        data={"m_kecamatan_id": "99"},
        headers={"X-Requested-With": "XMLHttpRequest", "Referer": f"{BASE}/sebaran-cctv"},
        timeout=20,
    )
    r.raise_for_status()
    payload = r.json()
    if not payload.get("msg_main", {}).get("status"):
        raise RuntimeError(f"API tolak: {payload.get('msg_main', {}).get('msg')}")
    cameras = payload["msg_detail"]["list_data"]
    log(f"malangkota: {len(cameras)} kamera dari API resmi")
    save_json(RAW_DIR / "malangkota.json", cameras)
    return cameras


if __name__ == "__main__":
    fetch_all()
