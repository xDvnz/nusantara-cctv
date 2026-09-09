"""Discovery: CCTV resmi Kota Bukittinggi (cctv.bukittinggikota.go.id).

Terverifikasi 2026-09-07:
- GET https://hls.bukittinggikota.go.id/api/get-list-camera -> JSON lokasi & listCamera (koordinat, stream_url, snapshot_url).
- Stream HLS: https://hls.bukittinggikota.go.id/hls1/.../s.m3u8 (MPEG-TS sync byte 0x47).
- Operator: Dinas Komunikasi dan Informatika Pemerintah Kota Bukittinggi (Sumatera Barat).
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import RAW_DIR, http_session, log, save_json  # noqa: E402

API_URL = "https://hls.bukittinggikota.go.id/api/get-list-camera"
REFERER = "https://cctv.bukittinggikota.go.id/"


def fetch_all():
    s = http_session()
    r = s.get(API_URL, headers={"Referer": REFERER}, timeout=20, verify=False)
    r.raise_for_status()
    locations = r.json()

    cameras = []
    for loc in locations:
        loc_id = loc.get("id")
        loc_name = loc.get("nama_lokasi", "").strip()
        lat = loc.get("latitude")
        lng = loc.get("longitude")
        for cam in loc.get("listCamera", []):
            cam_name = cam.get("nama_kamera", "").strip()
            full_name = f"{loc_name} {cam_name}".strip()
            pid = f"{loc_id}_{cam_name}"
            cameras.append({
                "id": pid,
                "name": full_name,
                "nama_lokasi": loc_name,
                "nama_kamera": cam_name,
                "latitude": lat,
                "longitude": lng,
                "stream_url": cam.get("stream_url"),
                "snapshot_url": cam.get("snapshot_url"),
            })

    log(f"bukittinggi: {len(locations)} lokasi, {len(cameras)} kamera ditemukan")
    save_json(RAW_DIR / "bukittinggi.json", cameras)
    return cameras


if __name__ == "__main__":
    fetch_all()
