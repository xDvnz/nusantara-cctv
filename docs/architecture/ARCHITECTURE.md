# ARCHITECTURE — Nusantara CCTV Monitor

Aplikasi Android native pemantau CCTV publik Indonesia.

## Tech Stack & Alasan

| Komponen | Pilihan | Alasan |
|---|---|---|
| Bahasa | Kotlin | Standar Android modern, null-safety, coroutine |
| UI | Jetpack Compose + Material 3 | Native (bukan WebView), dark mode bawaan, deklaratif |
| Pola | MVVM + repository | Proporsional untuk skala aplikasi ini; clean architecture penuh (use-case layer) overkill |
| DI | Manual (AppContainer di Application) | Graf dependensi kecil (±8 objek); Hilt/KSP menambah kompleksitas build tanpa keuntungan nyata di sini. Keputusan diambil sesuai §4 master prompt: "jangan gunakan library hanya karena populer" |
| Async | Coroutines + Flow | Lifecycle-aware, pagination lazy load |
| DB | Room 2.6 + KSP | Katalog offline, favorite, status cache |
| Networking | OkHttp 4.12 + Retrofit 2.11 (kotlinx-serialization) | OkHttp CookieJar penting: sumber Malang Kota butuh cookie sesi |
| Player | Media3 / ExoPlayer 1.5.x + modul HLS | Player native resmi; HLS = format dominan sumber; MJPEG via ImageRequest khusus; RTSP tidak didukung tanpa relay (§10) — arsitektur adapter disiapkan |
| Peta | osmdroid 6.1.18 (OSM) | Tanpa API key (Google Maps butuh key = secret di APK, dilarang §20). Clustering implementasi sendiri (grid screen-space) — hindari dependensi bonuspack eksternal |
| Sync katalog | Mode C hybrid: seed JSON bundel + update remote versi (URL opsional) + Room cache | Offline-first, dataset bisa tumbuh tanpa update APK |
| Serialization | kotlinx-serialization-json | Multiplatform, tidak reflect-heavy |

## Struktur Modul

```text
android/
  app/                       # single module (proporsional)
    src/main/kotlin/id/nusantara/cctv/
      CctvApp.kt             # Application + AppContainer (DI manual)
      data/
        db/                  # Room: entities, dao, database
        catalog/             # seed loader, remote sync, repository
        api/                 # OkHttp/Retrofit client + CookieJar
        player/              # StreamEngine (adapter per format)
      ui/
        theme/               # Material3, dark mode
        home/ map/ regions/ search/ detail/ player/ favorites/ settings/
        components/          # shared composables
      util/
  src/test/                  # unit test JVM (parser, filter, status, repo)
tools/                       # pipeline Python (discovery/validation/export)
data/                        # output katalog: cameras.json, cameras.csv
docs/                        # riset, arsitektur, sumber, dsb.
```

## Stream Adapter

```text
StreamEngine (resolve Camera -> playable MediaItem)
  ├── HlsStreamAdapter      (HLS .m3u8 — mayoritas sumber)
  ├── MjpegStreamAdapter    (multipart/x-mixed-replace)
  ├── DashStreamAdapter     (.mpd)
  └── UnsupportedAdapter    (RTSP/API-only -> state error jelas)
RTSP: tidak ada relay resmi -> katalog menandai RTSP sebagai tidak diputar;
      arsitektur relay HLS disiapkan lewat konfigurasi remote (hanya stream berizin).
```

Cookie & referer: sumber yang butuh sesi (mis. malangkota.go.id) dilayani lewat
`SessionHttpDataSource` — wrapper `DefaultHttpDataSource` yang menyuntik cookie dari
OkHttp CookieJar bersama + header referer per-domain (whitelist, bukan global).

## Data Flow

```text
discovery (Python, tools/) -> data/cameras.json (validated)
  -> seed: app assets
  -> Room (first launch)
  -> UI (Flow<List<Camera>>)
Remote sync (opsional, URL katalog + versi): fetch -> verify hash -> replace Room (transaksional, aman offline)
Status kamera: HEAD/GET manifest on-demand saat detail dibuka (bukan polling massal — §14)
```

## Keamanan & Legalitas

- Hanya sumber publik resmi pemda/lembaga; klasifikasi per sumber di DATA-SOURCES.md.
- Tidak ada secret di APK. Tidak ada bypass auth. Sumber AUTH_REQUIRED tidak masuk katalog.
- HTTPS preferred; timeout ketat; input terbatas ke field katalog tervalidasi.
- Attribution operator + portal di layar detail & Settings.
