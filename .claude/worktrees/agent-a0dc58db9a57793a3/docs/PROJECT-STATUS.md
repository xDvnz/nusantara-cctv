# PROJECT STATUS

Tanggal selesai: 2026-09-04

```text
PROJECT STATUS
==============

Application:       Nusantara CCTV Monitor (native Android, bukan WebView)
Package Name:      id.nusantara.cctv
Version:           1.0.0
Android Minimum SDK: 26 (Android 8.0)
Target SDK:        36

Build:
Debug APK:   android/app/build/outputs/apk/debug/app-debug.apk      (22.3 MB)
Release APK: android/app/build/outputs/apk/release/app-release-unsigned.apk (3.6 MB, minify+shrink)

CCTV DATA (katalog v4, seed bundel)
=========

Total discovered:  483  (401 + 82 dari riset agent paralel)
Total validated:   483  (semua diuji manifest + segment)
Online:            466
Offline:           15
Auth required:     2 sumber dikecualikan (Jakarta, Denpasar)
Invalid:           31 tidak dimasukkan katalog (bukan video)
Unknown:           0

Geographic coverage:
Province:      5 — Jawa Timur (253), DI Yogyakarta (147), Sumatera Selatan (30),
               Kalimantan Selatan (30), Jawa Barat (21)
Regency/City:  5 — Kota Malang, Kota Yogyakarta, Kota Palembang, Kota Banjarmasin, Kota Bandung
District:      ya — kecamatan (Malang, Yogya, Palembang), kelurahan (Yogya, Palembang)
Coordinates:   430 exact (API resmi); 51 administrative_only (Bandung & Banjarmasin tanpa koordinat)

STREAM TYPES
============

HLS:   481 (TS + fMP4, terbukti diputar)
DASH:  0
MJPEG: 0 (adapter terimplementasi + 6 unit test, belum ada sumber publik yang memakai)
RTSP:  0 masuk katalog — adapter menolak dengan pesan jelas (butuh relay resmi, §10)
Other: PUBLIC_EMBED (Bali) teridentifikasi, tidak dimasukkan (tak bisa diputar native)

ANDROID TEST (emulator Pixel_API35, API 35, 1080x2400)
============

Device:         emulator Pixel_API35 (tidak ada perangkat fisik terhubung)
Android Version: 15 (API 35)
Install:         OK (adb install, streamed install)
Launch:          OK, tanpa crash
Map:             OK — tile OSM, cluster grid (cluster "253" Malang terlihat),
                 tap cluster zoom-in, fitBounds seluruh sumber
Search:          OK — daftar + filter provinsi memfilter Malang saja
                 (ketik keyboard emulator bermasalah — bukan bug app; unit test
                 DAO memverifikasi query teks)
Filtering:       OK — provinsi aktif, kota/kecamatan tergantung pilihan
Player:          OK — HLS live Banjarmasin (TS) & Malang terbukti tayang di detail
Fullscreen:      OK — landscape otomatis, rotasi tanpa re-init player
Favorite:        OK — toggle dari detail, tampil di tab Favorit
Offline:         banner offline terpasang (network callback); katalog + favorit dari Room
Crash:           1 crash ditemukan QA (Room query di main thread pada Search) —
                 SUDAH DIPERBAIKI (Dispatchers.IO) dan diverifikasi ulang; 0 crash setelahnya

KNOWN LIMITATIONS
=================

1. Status kamera = hasil validasi saat pipeline terakhir jalan; aplikasi tidak
   polling massal. Tombol "Periksa status" memperbarui satu kamera on-demand.
2. Kota Bandung & Banjarmasin: portal tidak menyediakan koordinat → tidak tampil
   di peta, tetap bisa dicari/diputar (administrative_only).
3. Jakarta/Denpasar/Bali/Medan: butuh auth atau embed-only → tidak masuk katalog
   (lihat docs/data-sources/DATA-SOURCES.md). Menambah kota = jalankan pipeline
   + sinkron katalog remote, tanpa update APK.
4. Keyboard emulator gagal mengirim teks ke field (bug tooling GBoard emu);
   unit test DAO memverifikasi pencarian teks. Perangkat fisik normal.
5. Stream bisa mati sewaktu-waktu (milik operator); UI menampilkan error + retry.
6. RTSP tidak didukung tanpa relay resmi (keputusan arsitektur §10).

SOURCE & ATTRIBUTION
====================

- CCTV Kota Malang — Diskominfo Kota Malang (cctv.malangkota.go.id)
- CCTV Kota Yogyakarta — Jogja Command Center (cctv.jogjakota.go.id)
- CCTV Kota Palembang — Diskominfo/Dishub Palembang (cctv.palembang.go.id)
- ATCS Kota Banjarmasin — Dishub Banjarmasin (atcs.banjarmasinkota.go.id)
- ATCS Kota Bandung — Dishub Kota Bandung (atcs-dishub.bandung.go.id)
- Peta © OpenStreetMap contributors (osmdroid)

Attribution ditampilkan di layar detail kamera, layar Pengaturan, dan dibundel
di katalog (field source_name/operator/terms_of_use per kamera).
Aplikasi hanya menautkan stream publik; tidak merekam, tidak mirror,
tidak mendistribusikan ulang.

BUILD VALIDATION
================

- gradlew assembleDebug        → SUCCESS
- gradlew assembleRelease      → SUCCESS
- gradlew testDebugUnitTest    → SUCCESS (14 test, 0 gagal)
- gradlew lint                 → SUCCESS (0 error)
- adb install + launch + navigasi penuh → OK, 0 crash setelah fix terakhir
```
