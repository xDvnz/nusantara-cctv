"""Export katalog final: gabung raw + status validasi, normalisasi, dedupe, tulis
data/cameras.json (untuk seed APK + remote sync) dan data/cameras.csv.

Dedup key: (source_id, public_identifier). Kamera lintas sumber TIDAK digabung otomatis.
"""
import csv
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import DATA_DIR, ROOT, as_float, clean, log, now_wib, save_json, slug, stable_id  # noqa: E402

SOURCE_META = {
    "malangkota": {
        "source_name": "Portal CCTV Kota Malang (Diskominfo Kota Malang)",
        "source_url": "https://cctv.malangkota.go.id/sebaran-cctv",
        "operator": "Pemerintah Kota Malang",
        "province": "Jawa Timur",
        "city_regency": "Kota Malang",
        "access_type": "OFFICIAL_PORTAL",
        "auth_needed_for_stream": True,
        "bootstrap_url": "https://cctv.malangkota.go.id/sebaran-cctv",
        "referer": "https://cctv.malangkota.go.id/sebaran-cctv",
        "license_note": "Ditayangkan publik oleh Pemkot Malang untuk pantauan lalu lintas. Tidak untuk redistribusi ulang tanpa izin.",
        "confidence": 0.95,
    },
    "jogjakota": {
        "source_name": "Portal CCTV Kota Yogyakarta (Jogja Command Center)",
        "source_url": "https://cctv.jogjakota.go.id",
        "operator": "Pemerintah Kota Yogyakarta",
        "province": "DI Yogyakarta",
        "city_regency": "Kota Yogyakarta",
        "access_type": "PUBLIC_API",
        "auth_needed_for_stream": False,
        "bootstrap_url": None,
        "referer": None,
        "license_note": "Ditayangkan publik oleh Pemkot Yogyakarta untuk pantauan lalu lintas. Tidak untuk redistribusi ulang tanpa izin.",
        "confidence": 0.95,
    },
    "palembang": {
        "source_name": "CCTV Kota Palembang (Diskominfo/Dishub Palembang)",
        "source_url": "https://cctv.palembang.go.id",
        "operator": "Pemerintah Kota Palembang",
        "province": "Sumatera Selatan",
        "city_regency": "Kota Palembang",
        "access_type": "PUBLIC_API",
        "auth_needed_for_stream": False,
        "bootstrap_url": None,
        "referer": "https://cctv.palembang.go.id/",
        "license_note": "Ditayangkan publik oleh Pemkot Palembang untuk pantauan lalu lintas. Tidak untuk redistribusi ulang tanpa izin.",
        "confidence": 0.9,
    },
    "banjarmasin": {
        "source_name": "ATCS Dishub Kota Banjarmasin",
        "source_url": "https://atcs.banjarmasinkota.go.id",
        "operator": "Pemerintah Kota Banjarmasin",
        "province": "Kalimantan Selatan",
        "city_regency": "Kota Banjarmasin",
        "access_type": "PUBLIC_API",
        "auth_needed_for_stream": False,
        "bootstrap_url": None,
        "referer": "https://atcs.banjarmasinkota.go.id/",
        "license_note": "Ditayangkan publik oleh Pemkot Banjarmasin untuk pantauan lalu lintas. Tidak untuk redistribusi ulang tanpa izin.",
        "confidence": 0.9,
    },
    "bukittinggikota": {
        "source_name": "CCTV Kota Bukittinggi (Dishub Kota Bukittinggi)",
        "source_url": "https://cctv.bukittinggikota.go.id",
        "operator": "Pemerintah Kota Bukittinggi",
        "province": "Sumatera Barat",
        "city_regency": "Kota Bukittinggi",
        "access_type": "PUBLIC_API",
        "auth_needed_for_stream": False,
        "bootstrap_url": None,
        "referer": "https://cctv.bukittinggikota.go.id/",
        "license_note": "Ditayangkan publik oleh Pemkot Bukittinggi untuk pantauan lalu lintas. Tidak untuk redistribusi ulang tanpa izin.",
        "confidence": 0.9,
    },
    "kedirikota": {
        "source_name": "Portal ATCS Kota Kediri (Dishub Kota Kediri)",
        "source_url": "https://dishub.kedirikota.go.id/live-streaming-atcs/",
        "operator": "Pemerintah Kota Kediri",
        "province": "Jawa Timur",
        "city_regency": "Kota Kediri",
        "access_type": "OFFICIAL_PORTAL",
        "auth_needed_for_stream": False,
        "bootstrap_url": None,
        "referer": "https://dishub.kedirikota.go.id/",
        "license_note": "Ditayangkan publik oleh Pemkot Kediri untuk pantauan lalu lintas. Tidak untuk redistribusi ulang tanpa izin.",
        "confidence": 0.85,
    },
    "bandungkota": {
        "source_name": "ATCS Dishub Kota Bandung",
        "source_url": "https://atcs-dishub.bandung.go.id",
        "operator": "Pemerintah Kota Bandung",
        "province": "Jawa Barat",
        "city_regency": "Kota Bandung",
        "access_type": "OFFICIAL_PORTAL",
        "auth_needed_for_stream": False,
        "bootstrap_url": None,
        "referer": "https://atcs-dishub.bandung.go.id/",
        "license_note": "Ditayangkan publik oleh Dishub Kota Bandung untuk pantauan lalu lintas. Tidak untuk redistribusi ulang tanpa izin.",
        "confidence": 0.9,
    },
    "kedirikota": {
        "source_name": "Portal ATCS Kota Kediri (Dishub Kota Kediri)",
        "source_url": "https://dishub.kedirikota.go.id/live-streaming-atcs/",
        "operator": "Dinas Perhubungan Pemerintah Kota Kediri",
        "province": "Jawa Timur",
        "city_regency": "Kota Kediri",
        "access_type": "PUBLIC_DIRECT",
        "auth_needed_for_stream": False,
        "bootstrap_url": None,
        "referer": "https://dishub.kedirikota.go.id/",
        "license_note": "Ditayangkan publik oleh Dishub Kota Kediri untuk pantauan lalu lintas kota. Tidak untuk redistribusi ulang tanpa izin.",
        "confidence": 0.95,
    },
}

KODE_PROV = {
    "Jawa Timur": "JTM",
    "DI Yogyakarta": "YOY",
    "Sumatera Selatan": "SUS",
    "Kalimantan Selatan": "KLS",
    "Jawa Barat": "JBR",
    "Sumatera Barat": "SUT",
    "Sumatera Barat": "SMB",
}


def normalize(source_id, cam, validated):
    meta = SOURCE_META[source_id]
    name = (
        clean(cam.get("name"))
        or clean(cam.get("cctv_title"))
        or clean(cam.get("nama_kamera"))
        or clean(cam.get("nama_lokasi"))
        or "Kamera tanpa nama"
    )
    if source_id == "malangkota":
        pid = clean(cam.get("stream_id")) or slug(name)
        district = clean(cam.get("nama_kecamatan"))
        subdistrict = None
        location = clean(cam.get("address"))
        lat = as_float(cam.get("latitude"))
        lng = as_float(cam.get("longitude"))
    elif source_id == "jogjakota":
        pid = clean(cam.get("cctv_id")) or slug(name)
        district = clean(cam.get("kecamatan_nama"))
        subdistrict = clean(cam.get("kelurahan_nama"))
        location = clean(cam.get("cctv_desc")) or name
        lat = as_float(cam.get("cctv_latitude"))
        lng = as_float(cam.get("cctv_longitude"))
    elif source_id == "palembang":
        pid = clean(cam.get("cctv_id")) or slug(name)
        district = clean((cam.get("kecamatan") or {}).get("namaKecamatan"))
        subdistrict = clean((cam.get("kelurahan") or {}).get("namaKelurahan"))
        location = clean(cam.get("cctv_desc")) or name
        coords = (cam.get("location") or {}).get("coordinates") or [None, None]
        lng, lat = as_float(coords[0]), as_float(coords[1])
    elif source_id == "banjarmasin":
        pid = clean(cam.get("code")) or slug(name)
        district = None
        subdistrict = None
        location = name
        lat = None
        lng = None
    elif source_id == "bandungkota":
        pid = clean(cam.get("cctv_id")) or slug(name)
        district = None
        subdistrict = None
        location = clean(cam.get("desc")) or name
        lat = None
        lng = None
    elif source_id == "bukittinggikota":
        pid = clean(cam.get("stream_url")) or slug(name)
        district = None
        subdistrict = None
        location = clean(cam.get("nama_lokasi")) or name
        lat = as_float(cam.get("latitude"))
        lng = as_float(cam.get("longitude"))
    else:  # kedirikota
        pid = clean(cam.get("pid")) or slug(name)
        district = None
        subdistrict = None
        location = name
        lat = as_float(cam.get("latitude"))
        lng = as_float(cam.get("longitude"))
    stream_url = clean(cam.get("stream_url")) or ""
    status = validated.get("status", "UNKNOWN")
    checked = validated.get("last_checked")
    code = f"{KODE_PROV.get(meta['province'],'ID')}-{slug(name)[:24].upper()}"
    return {
        "id": stable_id(source_id, pid),
        "camera_name": name,
        "camera_code": code,
        "province": meta["province"],
        "city_regency": meta["city_regency"],
        "district": district,
        "subdistrict": subdistrict,
        "location_name": location or name,
        "latitude": lat,
        "longitude": lng,
        "location_accuracy": "exact" if (lat is not None and lng is not None) else "administrative_only",
        "source_id": source_id,
        "source_name": meta["source_name"],
        "source_url": meta["source_url"],
        "operator": meta["operator"],
        "access_type": meta["access_type"],
        "stream_type": "HLS",
        "stream_url": stream_url,
        "api_endpoint": meta["source_url"],
        "public_identifier": pid,
        "status": status,
        "last_checked": checked,
        "timezone": "Asia/Jakarta",
        "license": "Public monitoring feed",
        "terms_of_use": meta["license_note"],
        "confidence_score": meta["confidence"],
        "notes": "",
    }


def main():
    version_arg = sys.argv[sys.argv.index("--version") + 1] if "--version" in sys.argv else None
    # katalog versi lama: naikkan otomatis
    old = DATA_DIR / "cameras.json"
    prev_version = 0
    if old.exists():
        try:
            prev_version = json.loads(old.read_text(encoding="utf-8")).get("catalog_version", 0)
        except json.JSONDecodeError:
            prev_version = 0
    version = int(version_arg) if version_arg else prev_version + 1

    validated = {}
    vfile = DATA_DIR / "validated_raw.json"
    if not vfile.exists():
        log("validated_raw.json tidak ada — jalankan tools/validation/validate.py dulu")
        sys.exit(1)
    # validated_raw.json: list cam asli + _source_id + status; kunci dedup pakai stream/link
    for item in json.loads(vfile.read_text(encoding="utf-8")):
        sid = item.pop("_source_id", None)
        key = None
        if sid == "malangkota":
            key = item.get("stream_id")
        elif sid == "jogjakota":
            key = item.get("cctv_id")
        elif sid == "palembang":
            key = item.get("cctv_id")
        elif sid == "banjarmasin":
            key = item.get("code")
        elif sid == "bandungkota":
            key = item.get("cctv_id")
        elif sid == "bukittinggikota":
            key = item.get("stream_url")
        elif sid == "kedirikota":
            key = item.get("pid")
        if sid and key:
            validated[(sid, str(key))] = item

    sources_out = []
    for sid, meta in SOURCE_META.items():
        sources_out.append({
            "source_id": sid,
            "source_name": meta["source_name"],
            "source_url": meta["source_url"],
            "operator": meta["operator"],
            "access_type": meta["access_type"],
            "auth_needed_for_stream": meta["auth_needed_for_stream"],
            "bootstrap_url": meta["bootstrap_url"],
            "referer": meta["referer"],
            "license_note": meta["license_note"],
        })

    cameras_out = []
    seen = set()
    skipped_no_url = 0
    skipped_dup = 0
    for sid in SOURCE_META:
        raw = json.loads((ROOT / "data" / "raw" / f"{sid}.json").read_text(encoding="utf-8"))
        for cam in raw:
            if sid == "malangkota":
                pid = str(cam.get("stream_id"))
                link = f"https://cctv.malangkota.go.id/cctv-stream/streams/{pid}.m3u8"
            elif sid == "jogjakota":
                pid = str(cam.get("cctv_id"))
                link = cam.get("cctv_link")
            elif sid == "palembang":
                pid = str(cam.get("cctv_id"))
                link = cam.get("cctv_link")
            elif sid == "banjarmasin":
                pid = str(cam.get("code"))
                link = cam.get("stream")
            elif sid == "bukittinggikota":
                pid = str(cam.get("stream_url"))
                link = cam.get("stream_url")
            elif sid == "kedirikota":
                pid = str(cam.get("pid"))
                link = cam.get("stream_url")
            elif sid == "bandungkota":
                pid = str(cam.get("cctv_id"))
                link = cam.get("stream_url")
            else:  # kedirikota
                pid = str(cam.get("pid"))
                link = cam.get("stream_url")
            if not link:
                skipped_no_url += 1
                continue
            key = (sid, pid)
            if key in seen:
                skipped_dup += 1
                continue
            seen.add(key)
            v = validated.get(key, {})
            entry = normalize(sid, {**cam, "stream_url": link}, v)
            if entry["status"] == "ONLINE" or entry["status"] == "OFFLINE":
                cameras_out.append(entry)
    cameras_out.sort(key=lambda c: (c["province"], c["city_regency"], c["camera_name"]))

    catalog = {
        "catalog_version": version,
        "generated_at": now_wib(),
        "sources": sources_out,
        "cameras": cameras_out,
    }
    save_json(DATA_DIR / "cameras.json", catalog)

    csv_path = DATA_DIR / "cameras.csv"
    with csv_path.open("w", newline="", encoding="utf-8-sig") as f:
        fields = list(cameras_out[0].keys()) if cameras_out else ["id"]
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(cameras_out)

    online = sum(1 for c in cameras_out if c["status"] == "ONLINE")
    coords = sum(1 for c in cameras_out if c["latitude"] is not None)
    log(f"katalog v{version}: {len(cameras_out)} kamera ({online} online, {coords} dgn koordinat), "
        f"dup={skipped_dup}, tanpa_url={skipped_no_url}")
    log(f"output: {DATA_DIR/'cameras.json'} + cameras.csv")


if __name__ == "__main__":
    main()
