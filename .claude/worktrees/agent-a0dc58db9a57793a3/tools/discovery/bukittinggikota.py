"""Discovery: CCTV Kota Bukittinggi (cctv.bukittinggikota.go.id).

Terverifikasi 2026-09-06:
- GET https://hls.bukittinggikota.go.id/api/get-list-camera (tanpa auth)
  -> [{nama_lokasi, latitude, longitude, listCamera:[{nama_kamera, stream_url, snapshot_url}]}]
- HLS TS 2 detik, tanpa sesi. Koordinat exact.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import RAW_DIR, http_session, log, save_json  # noqa: E402

API = "https://hls.bukittinggikota.go.id/api/get-list-camera"


def fetch_all():
    s = http_session()
    r = s.get(API, timeout=20, headers={"Referer": "https://cctv.bukittinggikota.go.id/"})
    r.raise_for_status()
    locations = r.json()
    cameras = []
    for loc in locations:
        for cam in loc.get("listCamera") or []:
            cameras.append({
                "nama_kamera": f"{loc['nama_lokasi']} {cam.get('nama_kamera','')}".strip(),
                "nama_lokasi": loc.get("nama_lokasi"),
                "latitude": loc.get("latitude"),
                "longitude": loc.get("longitude"),
                "stream_url": cam.get("stream_url"),
                "snapshot_url": cam.get("snapshot_url"),
            })
    log(f"bukittinggikota: {len(cameras)} kamera dari {len(locations)} lokasi")
    save_json(RAW_DIR / "bukittinggikota.json", cameras)
    return cameras


if __name__ == "__main__":
    fetch_all()
