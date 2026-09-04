# DEVELOPMENT

## Setup

1. JDK 17/21, Android SDK (platform 36, build-tools 35), Python 3.11 + requests.
2. `android/local.properties` → `sdk.dir=C:/path/to/Android/Sdk` (pakai forward slash di Windows).
3. Tidak ada API key/secret yang diperlukan (peta = osmdroid/OSM).

## Perintah harian

```bash
cd android
./gradlew assembleDebug          # build APK debug
./gradlew installDebug           # install ke device/emulator aktif
./gradlew testDebugUnitTest      # unit test (14 test)
./gradlew lint                   # lint (0 error)
./gradlew assembleRelease        # APK release (unsigned, minify+shrink)
adb shell am start -n id.nusantara.cctv/.MainActivity
adb logcat --pid=$(adb shell pidof id.nusantara.cctv)
```

Regenerasi katalog data: lihat README bagian pipeline.

## Keputusan penting (jangan dilanggar tanpa alasan)

- **DI manual** (AppContainer) — graf kecil; Hilt tidak menambah nilai.
- **Cookie sesi publik**: sumber malangkota butuh bootstrap GET halaman sebelum API/stream.
  Semua lewat `SourceHttp` bersama; Media3 dapat cookie via `DefaultHttpDataSource`
  dengan header dari `cookieStore` yang sama.
- **TLS malangkota**: server tak kirim intermediate (GeoTrust TLS RSA CA G1) →
  intermediate publik di-bundle `res/raw` + `network_security_config.xml` domain-config.
  Bukan secret; sertifikat publik DigiCert.
- **RTSP tidak diputar** — tanpa relay resmi, katalog menandai unsupported (§10 master prompt).
- **Tidak ada polling massal** — status kamera hanya diperiksa saat user membuka detail
  atau menekan "Periksa status" (satu kamera sekali probe).
- **Palembang rate-limit** — pipeline validasi proses serial 1 rps untuk sumber ini.
- Jangan menambah kamera tanpa melewati `tools/validation` (HTTP 200 saja tidak cukup).

## Struktur Kotlin (android/app/src/main/kotlin/id/nusantara/cctv)

```text
CctvApp.kt                  Application + AppContainer (DI manual)
data/model/Models.kt        Camera, CameraSourceConfig, CatalogVersion
data/catalog/CatalogDto.kt  DTO kotlinx-serialization untuk cameras.json
data/catalog/...Repository  seed aset, sync remote, query, favorit
data/db/CctvDatabase.kt     Room: cameras, favorites, sources, catalog_meta + mapper
data/api/SourceHttp.kt      OkHttp + CookieStore bersama + bootstrap sesi
data/player/StreamEngine    resolve format -> Playable (Exo/Mjpeg/Unsupported)
data/player/...Controller   pemilik ExoPlayer + UI state (detail & fullscreen)
ui/*                        Compose: home, map, regions, search, detail, favorites, settings
```

## Troubleshooting build

- `The filename, directory name, or volume label syntax is incorrect` →
  `local.properties` memakai backslash; ganti forward slash.
- `Cannot access database on the main thread` → query Room non-suspend wajib
  dibungkus `withContext(Dispatchers.IO)`.
- Media3 `UnsafeOptInUsageError` → anotasi `@OptIn(UnstableApi::class)` di kelas player.
