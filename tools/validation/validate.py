"""Validasi stream katalog (§9 master prompt).

Aturan:
- ONLINE hanya jika: manifest 200 + body #EXTM3U + segment termanifest benar-benar video
  (magic byte TS 0x47, atau content-type fMP4).
- Status: ONLINE / OFFLINE / TIMEOUT / INVALID_STREAM / AUTH_REQUIRED.
- Uji satu segment per kamera (read-only, satu-shot, bukan polling).
- Malangkota butuh cookie sesi publik + referer (bootstrap halaman resmi dulu).
"""
import concurrent.futures as cf
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import DATA_DIR, ROOT, http_session, log, now_wib, save_json  # noqa: E402

import urllib3  # noqa: E402
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

RAW = ROOT / "data" / "raw"

SOURCES = {
    "malangkota": {
        "stream": lambda cam, sid: (
            f"https://cctv.malangkota.go.id/cctv-stream/streams/{cam['stream_id']}.m3u8",
            {"Referer": "https://cctv.malangkota.go.id/sebaran-cctv"},
        ),
        "session": "bootstrap_malangkota",
        "workers": 8,
        "delay": 0.0,
    },
    "jogjakota": {
        "stream": lambda cam, sid: (cam["cctv_link"], {}),
        "session": None,
        "workers": 8,
        "delay": 0.0,
    },
    "palembang": {
        "stream": lambda cam, sid: (cam["cctv_link"], {"Referer": "https://cctv.palembang.go.id/"}),
        "session": None,
        "workers": 1,  # server agresif menutup koneksi beruntun — proses serial
        "delay": 1.0,
    },
    "banjarmasin": {
        "stream": lambda cam, sid: (cam["stream"], {"Referer": "https://atcs.banjarmasinkota.go.id/"}),
        "session": None,
        "workers": 4,
        "delay": 0.2,
    },
    "bandungkota": {
        "stream": lambda cam, sid: (cam["stream_url"], {"Referer": "https://atcs-dishub.bandung.go.id/"}),
        "session": None,
        "workers": 4,
        "delay": 0.2,
    },
}

_sessions = {}


def get_session(key):
    if key not in _sessions:
        s = http_session()
        if key == "bootstrap_malangkota":
            s.get("https://cctv.malangkota.go.id/sebaran-cctv", timeout=20,
                  headers={"Referer": "https://cctv.malangkota.go.id"})
        _sessions[key] = s
    return _sessions[key]


def join_url(base, rel):
    if rel.startswith("http"):
        return rel
    from urllib.parse import urljoin
    return urljoin(base, rel)


def validate_one(source_id, cam):
    try:
        url, extra_headers = SOURCES[source_id]["stream"](cam, None)
        if not url:
            return {**cam, "status": "INVALID_STREAM", "last_checked": now_wib()}
        s = get_session(SOURCES[source_id]["session"])
        r = s.get(url, timeout=15, headers={**extra_headers, "Accept": "*/*"}, verify=False)
        if r.status_code in (401, 403):
            return {**cam, "status": "AUTH_REQUIRED", "last_checked": now_wib()}
        if r.status_code == 404:
            return {**cam, "status": "OFFLINE", "last_checked": now_wib()}
        if r.status_code != 200:
            return {**cam, "status": "OFFLINE", "last_checked": now_wib()}
        text = r.text.lstrip()
        if not text.startswith("#EXTM3U"):
            return {**cam, "status": "INVALID_STREAM", "last_checked": now_wib()}
        # master playlist -> ambil varian pertama; media playlist -> segment pertama
        target = None
        for line in text.splitlines():
            line = line.strip()
            if line and not line.startswith("#"):
                target = line
                break
        if not target:
            return {**cam, "status": "INVALID_STREAM", "last_checked": now_wib()}
        if target.endswith(".m3u8"):
            r2 = s.get(join_url(url, target), timeout=15, headers=extra_headers, verify=False)
            if r2.status_code != 200 or not r2.text.lstrip().startswith("#EXTM3U"):
                return {**cam, "status": "INVALID_STREAM", "last_checked": now_wib()}
            seg = None
            for line in r2.text.splitlines():
                line = line.strip()
                if line and not line.startswith("#"):
                    seg = line
                    break
            if not seg:
                return {**cam, "status": "INVALID_STREAM", "last_checked": now_wib()}
            seg_url = join_url(url, seg)
        else:
            seg_url = join_url(url, target)
        r3 = s.get(seg_url, timeout=20, headers=extra_headers, stream=True, verify=False)
        head = next(r3.iter_content(chunk_size=188), b"")
        r3.close()
        if not head:
            return {**cam, "status": "INVALID_STREAM", "last_checked": now_wib()}
        if head[0] == 0x47:  # MPEG-TS sync byte
            return {**cam, "status": "ONLINE", "last_checked": now_wib()}
        fmp4_boxes = (b"ftyp", b"moof", b"styp", b"moov", b"mdat")  # fMP4: box size dulu, lalu magic
        if any(box in head[:16] for box in fmp4_boxes):
            return {**cam, "status": "ONLINE", "last_checked": now_wib()}
        return {**cam, "status": "INVALID_STREAM", "last_checked": now_wib()}
    except json.JSONDecodeError:
        return {**cam, "status": "INVALID_STREAM", "last_checked": now_wib()}
    except Exception as e:  # noqa: BLE001
        kind = type(e).__name__
        status = "TIMEOUT" if ("timeout" in str(e).lower() or "Timeout" in kind) else "OFFLINE"
        return {**cam, "status": status, "last_checked": now_wib()}


def main():
    limit = None
    if "--limit" in sys.argv:
        limit = int(sys.argv[sys.argv.index("--limit") + 1])
    only = None
    if "--only" in sys.argv:
        only = sys.argv[sys.argv.index("--only") + 1]
    import time
    import concurrent.futures as cf
    groups = []
    for source_id in SOURCES:
        if only and source_id != only:
            continue
        raw_file = RAW / f"{source_id}.json"
        if not raw_file.exists():
            log(f"skip {source_id}: raw tidak ada")
            continue
        cams = json.loads(raw_file.read_text(encoding="utf-8"))
        if limit:
            cams = cams[:limit]
        groups.append((source_id, cams))
    total = sum(len(cams) for _, cams in groups)
    log(f"validasi {total} kamera...")

    def run_group(source_id, cams):
        cfg = SOURCES[source_id]
        results = []
        with cf.ThreadPoolExecutor(max_workers=cfg["workers"]) as ex:
            futs = []
            for cam in cams:
                if cfg["delay"]:
                    time.sleep(cfg["delay"])
                futs.append(ex.submit(validate_one, source_id, cam))
            for i, f in enumerate(cf.as_completed(futs), 1):
                result = f.result()
                result["_source_id"] = source_id
                results.append(result)
                if i % 50 == 0:
                    log(f"  {source_id}: {i}/{len(cams)}")
        return results

    results = []
    for source_id, cams in groups:
        log(f"mulai {source_id} ({len(cams)})")
        results.extend(run_group(source_id, cams))

    summary = {}
    for r in results:
        summary[r["status"]] = summary.get(r["status"], 0) + 1
    log(f"hasil: {summary}")

    # Merge dengan hasil run sebelumnya (run --only tidak menghapus sumber lain)
    out_file = DATA_DIR / "validated_raw.json"
    if out_file.exists():
        existing = json.loads(out_file.read_text(encoding="utf-8"))
        current_sids = {sid for sid, _ in groups}
        existing = [e for e in existing if e.get("_source_id") not in current_sids]
        results = existing + results
    save_json(out_file, results)


if __name__ == "__main__":
    main()
