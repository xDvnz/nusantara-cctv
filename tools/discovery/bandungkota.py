"""Discovery: ATCS Dishub Kota Bandung (atcs-dishub.bandung.go.id).

Terverifikasi 2026-09-03:
- Homepage HTML berisi onclick="showListCamera(<area_id>)" untuk wilayah aktif.
- POST /ajax/cctv-list (id=area_id) -> HTML berisi onclick="showStreamingModal(<cam_id>)" + nama.
- POST /ajax/cctv-info (id=cam_id) -> {"name","desc","src"}; src = HLS
  https://atcs-dishub.bandung.go.id:1990/{Nama}/index.m3u8 (704x576).
- Wajib header X-Requested-With: XMLHttpRequest.
- Tidak ada koordinat dari portal -> katalog pakai location_accuracy administrative_only.
"""
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import RAW_DIR, http_session, log, save_json  # noqa: E402

BASE = "https://atcs-dishub.bandung.go.id"
XHR = {"X-Requested-With": "XMLHttpRequest", "Referer": f"{BASE}/"}

AREA_RE = re.compile(r'showListCamera\((\d+)\)')
CAM_ID_RE = re.compile(r'showStreamingModal\((\d+)\)')


def fetch_all():
    s = http_session()
    home = s.get(BASE, timeout=20).text
    area_ids = sorted(set(AREA_RE.findall(home)))
    log(f"bandungkota: {len(area_ids)} area: {area_ids}")

    cam_ids = []
    cam_names = {}
    for area in area_ids:
        html = s.post(f"{BASE}/ajax/cctv-list", data={"id": area}, headers=XHR, timeout=20).text
        ids = CAM_ID_RE.findall(html)
        # nama diekstrak dari HTML list (tag <p> setelah link): parsing per blok
        blocks = re.split(r'showStreamingModal\((\d+)\)', html)
        for i in range(1, len(blocks) - 1, 2):
            cid = blocks[i]
            block = blocks[i + 1]
            m = re.search(r'<p[^>]*>\s*(.*?)\s*</p>', block)
            name = re.sub(r"<[^>]+>", "", m.group(1)).strip() if m else ""
            cam_ids.append(cid)
            cam_names[cid] = name
        time.sleep(0.4)

    cam_ids = sorted(set(cam_ids), key=int)
    log(f"bandungkota: {len(cam_ids)} kamera unik")

    cameras = []
    for cid in cam_ids:
        r = s.post(f"{BASE}/ajax/cctv-info", data={"id": cid}, headers=XHR, timeout=20)
        if not r.ok:
            log(f"bandungkota: cctv-info {cid} HTTP {r.status_code}, skip")
            continue
        try:
            info = r.json()
        except ValueError:
            log(f"bandungkota: cctv-info {cid} bukan JSON, skip")
            continue
        src = (info.get("src") or "").strip()
        if not src:
            continue
        cameras.append({
            "cctv_id": cid,
            "name": info.get("name") or cam_names.get(cid) or f"ATCS Bandung {cid}",
            "desc": info.get("desc") or "",
            "stream_url": src,
        })
        time.sleep(0.4)
    log(f"bandungkota: {len(cameras)} kamera dengan stream")
    save_json(RAW_DIR / "bandungkota.json", cameras)
    return cameras


if __name__ == "__main__":
    fetch_all()
