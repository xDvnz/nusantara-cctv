"""Discovery: CCTV resmi Kota Kediri (dishub.kedirikota.go.id/live-streaming-atcs/).

Terverifikasi 2026-09-07:
- Server MediaMTX resmi Dishub Kota Kediri di pplterpadu.kedirikota.go.id:8888
- Format: HLS H.264/AAC MPEG-TS (sync byte 0x47)
- Operator: Dinas Perhubungan Pemerintah Kota Kediri (Jawa Timur)
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import RAW_DIR, log, save_json  # noqa: E402

KEDIRI_CAMERAS = [
    {
        "id": "kediri_jetis",
        "name": "Simpang Jetis",
        "slug": "jetis",
        "latitude": -7.8427,
        "longitude": 112.0165,
        "location": "Simpang Jetis, Kota Kediri",
    },
    {
        "id": "kediri_tosaren",
        "name": "Simpang Tosaren",
        "slug": "tosaren",
        "latitude": -7.8436,
        "longitude": 112.0402,
        "location": "Simpang Tosaren, Pesantren, Kota Kediri",
    },
    {
        "id": "kediri_baruna",
        "name": "Simpang Baruna",
        "slug": "baruna",
        "latitude": -7.8225,
        "longitude": 112.0236,
        "location": "Simpang Baruna, Kota Kediri",
    },
    {
        "id": "kediri_alun_alun",
        "name": "Alun-Alun Kota Kediri",
        "slug": "alun_alun",
        "latitude": -7.8203,
        "longitude": 112.0116,
        "location": "Alun-Alun Kota Kediri",
    },
    {
        "id": "kediri_bandar_ngalim",
        "name": "Jembatan Bandar Ngalim",
        "slug": "bandar_ngalim",
        "latitude": -7.8251,
        "longitude": 112.0084,
        "location": "Jembatan Bandar Ngalim, Mojoroto, Kota Kediri",
    },
    {
        "id": "kediri_mrican",
        "name": "Simpang Mrican",
        "slug": "mrican",
        "latitude": -7.7785,
        "longitude": 112.0018,
        "location": "Simpang Mrican, Mojoroto, Kota Kediri",
    },
    {
        "id": "kediri_iskandar_muda",
        "name": "Simpang Iskandar Muda",
        "slug": "iskandar_muda",
        "latitude": -7.7915,
        "longitude": 112.0055,
        "location": "Simpang Iskandar Muda, Mojoroto, Kota Kediri",
    },
    {
        "id": "kediri_muning",
        "name": "Simpang Muning",
        "slug": "muning",
        "latitude": -7.8344,
        "longitude": 112.0028,
        "location": "Simpang Muning, Mojoroto, Kota Kediri",
    },
    {
        "id": "kediri_semampir",
        "name": "Simpang Semampir",
        "slug": "semampir",
        "latitude": -7.8016,
        "longitude": 112.0093,
        "location": "Simpang Semampir, Kota Kediri",
    },
    {
        "id": "kediri_nabatiasa",
        "name": "Simpang Nabatiasa",
        "slug": "nabatiasa",
        "latitude": -7.8182,
        "longitude": 112.0195,
        "location": "Simpang Nabatiasa, Kota Kediri",
    },
    {
        "id": "kediri_dandangan",
        "name": "Simpang Dandangan",
        "slug": "dandangan",
        "latitude": -7.8080,
        "longitude": 112.0163,
        "location": "Simpang Dandangan, Kota Kediri",
    },
    {
        "id": "kediri_water_torn",
        "name": "Water Torn (Tandon Air)",
        "slug": "water_torn",
        "latitude": -7.8122,
        "longitude": 112.0152,
        "location": "Water Torn, Jl. PK Bangsa, Kota Kediri",
    },
    {
        "id": "kediri_tamanan",
        "name": "Terminal Tamanan",
        "slug": "tamanan",
        "latitude": -7.8285,
        "longitude": 111.9877,
        "location": "Terminal Tamanan, Mojoroto, Kota Kediri",
    },
    {
        "id": "kediri_a_yani_utara",
        "name": "Jl. Ahmad Yani Utara",
        "slug": "a_yani_utara",
        "latitude": -7.8128,
        "longitude": 112.0289,
        "location": "Jl. Ahmad Yani Utara, Kota Kediri",
    },
    {
        "id": "kediri_kawi",
        "name": "Simpang Kawi",
        "slug": "kawi",
        "latitude": -7.8389,
        "longitude": 111.9965,
        "location": "Simpang Kawi, Mojoroto, Kota Kediri",
    },
    {
        "id": "kediri_sukorame",
        "name": "Simpang Sukorame",
        "slug": "sukorame",
        "latitude": -7.8205,
        "longitude": 111.9928,
        "location": "Simpang Sukorame, Mojoroto, Kota Kediri",
    },
    {
        "id": "kediri_a_yani_selatan",
        "name": "Jl. Ahmad Yani Selatan",
        "slug": "a_yani_selatan",
        "latitude": -7.8222,
        "longitude": 112.0285,
        "location": "Jl. Ahmad Yani Selatan, Kota Kediri",
    },
]


def fetch_all():
    cameras = []
    for c in KEDIRI_CAMERAS:
        slug = c["slug"]
        stream_url = f"https://pplterpadu.kedirikota.go.id:8888/{slug}/index.m3u8"
        cameras.append({
            "id": c["id"],
            "name": c["name"],
            "slug": slug,
            "location": c["location"],
            "latitude": c["latitude"],
            "longitude": c["longitude"],
            "stream_url": stream_url,
        })

    log(f"kedirikota: {len(cameras)} kamera terdaftar")
    save_json(RAW_DIR / "kedirikota.json", cameras)
    return cameras


if __name__ == "__main__":
    fetch_all()
