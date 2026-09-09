# Nusantara CCTV Monitor

Pantau CCTV lalu lintas publik dari seluruh Indonesia dalam satu aplikasi Android.
Semua kamera berasal dari portal resmi pemerintah daerah — aplikasi ini hanya
menayangkan ulang tautan publiknya, tanpa login, tanpa rekaman.

**674 kamera • 10 kota/kabupaten • 7 provinsi** (semua tervalidasi tayang)

| Provinsi | Kota/Kabupaten | Kamera |
|---|---|---|
| Jawa Timur | Malang, Kediri | 270 |
| DI Yogyakarta | Yogyakarta | 145 |
| Kalimantan Selatan | Banjarmasin, Banjarbaru | 69 |
| Jawa Barat | Bandung, Kuningan | 71 |
| Jawa Tengah | Magelang | 45 |
| Sumatera Barat | Bukittinggi | 45 |
| Sumatera Selatan | Palembang | 29 |

## Unduh

Ambil APK terbaru dari [GitHub Releases](https://github.com/xDvnz/nusantara-cctv/releases):

1. Unduh `NusantaraCCTV-v<versi>-release.apk` (file `release`, bukan `debug`).
2. Buka file-nya di HP. Jika diminta, izinkan "Install dari sumber tidak dikenal".
3. Selesai — tidak perlu akun, tidak ada iklan.

Butuh Android 8.0 (Oreo) ke atas. Update di atas versi lama langsung terpasang
tanpa uninstall; favorit dan riwayat aman. Aplikasi juga otomatis memberi tahu
saat versi baru tersedia (tab **Tentang → Pembaruan → Cek pembaruan**).

## Fitur

- **Live player** — HLS native (Media3), layar penuh, muat ulang, status kamera
- **Peta** — marker + clustering, 4 basemap (OSM/Satelit/Gelap/Medan), panel
  lapisan dan status bar koordinat
- **Cari & filter** — nama/lokasi, provinsi, kota, kecamatan, status, operator
- **Favorit & riwayat** — tersimpan di perangkat, tetap terbuka saat offline
- **Tema** — Material You (ikut wallpaper), Terang, Gelap, Cyber, Monokrom
- **Bahasa** — Indonesia & English
- **Pull-to-refresh** — tarik ke bawah untuk menyegarkan status kamera

## Sumber data

Kamera bersumber dari portal publik resmi yang dikelola pemerintah daerah
(Diskominfo/Dishub), antara lain:

- [cctv.malangkota.go.id](https://cctv.malangkota.go.id/sebaran-cctv) — Kota Malang
- [cctv.jogjakota.go.id](https://cctv.jogjakota.go.id) — Kota Yogyakarta
- [cctv.palembang.go.id](https://cctv.palembang.go.id) — Kota Palembang
- [atcs.banjarmasinkota.go.id](https://atcs.banjarmasinkota.go.id) — Kota Banjarmasin
- [atcs-dishub.bandung.go.id](https://atcs-dishub.bandung.go.id) — Kota Bandung
- [cctv.bukittinggikota.go.id](https://cctv.bukittinggikota.go.id) — Kota Bukittinggi
- [dishub.kedirikota.go.id](https://dishub.kedirikota.go.id/live-streaming-atcs/) — Kota Kediri
- [cctv.banjarbarukota.go.id](https://cctv.banjarbarukota.go.id) — Kota Banjarbaru
- [cctv.kuningankab.go.id](https://cctv.kuningankab.go.id) — Kabupaten Kuningan
- [cctv.magelangkota.go.id](https://cctv.magelangkota.go.id) — Kota Magelang

Atribusi lengkap per kamera tampil di aplikasi (layar detail & tab Tentang).
Peta oleh [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors
dan penyedia tile masing-masing.

Aplikasi ini tidak berafiliasi dengan operator mana pun, tidak merekam, dan
tidak menyimpan ulang siaran. Kamera bisa tayang hitam/putus jika operator
mematikannya — gunakan tombol muat ulang atau coba kamera lain.

## Build sendiri (untuk pengembang)

```bash
git clone https://github.com/xDvnz/nusantara-cctv.git
cd nusantara-cctv/android
# buat local.properties → sdk.dir=C:/path/to/Android/Sdk (forward slash!)
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/
./gradlew testDebugUnitTest lint
```

Regenerasi katalog kamera (Python 3.11+):

```bash
python tools/discovery/run_all.py      # ambil daftar kamera dari semua portal
python tools/validation/validate.py    # uji stream (manifest + segmen video)
python tools/import_export/export.py   # gabung + dedup → data/cameras.json
```

Kebutuhan: JDK 17+, Android SDK (platform 36), Python 3.11+ dengan `requests`.
Dokumentasi teknis lengkap ada di [docs/](docs/) — mulai dari
[ARSITEKTUR](docs/architecture/ARCHITECTURE.md) dan
[SUMBER DATA](docs/data-sources/DATA-SOURCES.md).

## Legal

Feed CCTV tetap milik masing-masing pemerintah daerah dan ditayangkan untuk
kepentingan publik. Proyek ini menayangkan ulang tautan publik apa adanya tanpa
mengubah atau mendistribusikan ulang konten. Untuk penggunaan komersial atau
redistribusi, hubungi operator terkait. Lisensi kode dan dependensi:
[LICENSES](docs/LICENSES.md).
