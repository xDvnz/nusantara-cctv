# METODOLOGI RISET — CCTV Publik Indonesia

Prinsip: **500 kamera valid > 50.000 kamera palsu.**

## Hierarki sumber (sesuai §5 master prompt)

1. Pemerintah pusat (Kemenhub/ATCS, portal data nasional)
2. Pemerintah provinsi (Diskominfo/Dishub provinsi, ATCS provinsi)
3. Pemerintah kabupaten/kota (portal sebaran CCTV, command center, smart city)
4. Infrastruktur publik (jalan, simpang, terminal, pelabuhan, wisata)

## Prosedur verifikasi tiap sumber

1. Probe portal (curl, UA browser, timeout 10-12s).
2. Temukan endpoint daftar kamera (inspeksi JS halaman: `fetch/ajax`, `/api/`, `getdata`, `get-cameras`).
3. Ambil daftar kamera → validasi field (nama, wilayah, koordinat, stream).
4. Validasi stream: GET manifest → HTTP 200 + content-type `mpegurl`/`mp2t` + isi `#EXTM3U`;
   uji 1 segment termanifest → magic byte TS `0x47`.
   HTTP 200 saja TIDAK cukup (§9): respons HTML/404-page dinilai INVALID_STREAM.
5. Klasifikasi akses: `PUBLIC_DIRECT` (tanpa sesi), `PUBLIC_API`/`OFFICIAL_PORTAL` (perlu cookie sesi
   publik dari bootstrap halaman — bukan login), `AUTH_REQUIRED` (login) → dikeluarkan dari katalog.
6. Koordinat: hanya dari sumber resmi. Tidak ada geocode spekulatif. `location_accuracy`
   sesuai asal data (exact = API resmi; administrative_only = hanya kecamatan).
7. Dedup: kunci `(source_id, public_identifier)`; kamera sama lintas-sumber digabung manual, bukan otomatis.

## Keyword riset yang dipakai

`sebaran cctv <kota>`, `atcs <kota>`, `cctv lalu lintas <kota>`, `command center <kota>`,
`smart city cctv <kota>`, `dishub cctv <kota>`, `live traffic camera indonesia`.

## Status validasi (rolling, update tiap eksekusi pipeline)

| Sumber | Status | Keterangan |
|---|---|---|
| cctv.malangkota.go.id | TERVERIFIKASI | API POST /api/v2/get-cameras + cookie sesi; HLS /cctv-stream/streams/<id>.m3u8 + cookie+referer |
| cctv.jogjakota.go.id | TERVERIFIKASI | GET /home/getdata JSON publik; HLS cctvjss.jogjakota.go.id/atcs/ATCS_*.stream/playlist.m3u8 (tanpa sesi) |
| lainnya | di riset | lihat hasil agent riset + DATA-SOURCES.md |

## Regenerasi dataset

```bash
python tools/discovery/run_all.py      # fetch semua sumber
python tools/validation/validate.py    # uji manifest+segment, tulis status
python tools/import_export/export.py   # dedupe + data/cameras.json + csv
```
