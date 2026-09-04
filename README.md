# Nusantara CCTV Monitor

Aplikasi **Android native** untuk memantau CCTV publik Indonesia yang ditayangkan
resmi oleh pemerintah daerah/lembaga. Bukan WebView — Jetpack Compose + Media3
ExoPlayer + osmdroid.

> 481 kamera tervalidasi (466 online saat validasi terakhir), 5 provinsi,
> 430 dengan koordinat exact dari API resmi pemda.

## Sumber data (lihat [docs/data-sources/DATA-SOURCES.md](docs/data-sources/DATA-SOURCES.md))

- Kota Malang — Diskominfo Kota Malang
- Kota Yogyakarta — Jogja Command Center
- Kota Palembang — Diskominfo/Dishub
- Kota Banjarmasin — ATCS Dishub
- Kota Bandung — ATCS Dishub

Hanya stream publik resmi. Tanpa bypass auth. Tanpa re-host.

## Cara build & install

### Prasyarat
- JDK 17/21
- Android SDK (platform 36, build-tools 35)
- Perangkat Android 8.0+ (minSdk 26) atau emulator

### Langkah

```bash
# 1. Clone / masuk folder proyek
cd android

# 2. (opsional) sesuaikan sdk.dir di local.properties

# 3. Build APK debug
./gradlew assembleDebug

# 4. Install ke perangkat yang terhubung
./gradlew installDebug
# atau
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. Launch
adb shell am start -n id.nusantara.cctv/.MainActivity
```

### Menjalankan pipeline data (regenerasi katalog)

```bash
# 1. Ambil daftar kamera dari semua portal resmi (read-only, one-shot)
python tools/discovery/run_all.py

# 2. Validasi stream: manifest HLS + segment benar-benar video (TS/fMP4)
python tools/validation/validate.py            # semua sumber
python tools/validation/validate.py --only banjarmasin   # satu sumber
python tools/validation/validate.py --limit 10 # smoke test

# 3. Dedup + normalisasi + export katalog (JSON utk APK + CSV)
python tools/import_export/export.py
```

Output katalog: `data/cameras.json` + `data/cameras.csv`.
Katalog dipakai sebagai **seed bundel** di `android/app/src/main/assets/catalog/cameras.json`.
Salin ulang setelah regenerasi:

```bash
cp data/cameras.json android/app/src/main/assets/catalog/cameras.json
```

### Update katalog tanpa update APK (PHASE 8)

Host `data/cameras.json` di server statis, lalu isi URL-nya di **Pengaturan →
URL katalog remote → Simpan & sinkron**. Aplikasi membandingkan
`catalog_version`, mengganti DB secara transaksional, dan mempertahankan favorit.

## Struktur proyek

```text
docs/           arsitektur, metodologi riset, sumber data, laporan fase
tools/          pipeline Python (discovery / validation / export)
data/           katalog hasil pipeline + log validasi
android/        proyek Android (Kotlin, Compose, Media3, Room, osmdroid)
```

Dokumentasi lain: [ARCHITECTURE](docs/architecture/ARCHITECTURE.md) ·
[DATA-SOURCES](docs/data-sources/DATA-SOURCES.md) ·
[METHODOLOGY](docs/research/METHODOLOGY.md) · [TESTING](docs/TESTING.md) ·
[PRIVACY](docs/PRIVACY.md) · [LICENSES](docs/LICENSES.md)

## Legal

Feed berasal dari portal publik resmi pemda untuk pantauan lalu lintas.
Aplikasi memutar langsung dari server sumber tanpa menyimpan/mendistribusikan ulang.
Attribusi operator ditampilkan di setiap halaman kamera dan di Pengaturan.
Redistribusi ulang stream memerlukan izin operator masing-masing.
