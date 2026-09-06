# AGENTS.md — Panduan Serah Terima Lengkap Proyek Nusantara CCTV

> **DOKUMEN INI WAJIB DIBACA SELURUHNYA oleh agent mana pun yang mengambil alih proyek ini.**
> Berisi seluruh sejarah, keputusan, jebakan teknis, dan prosedur yang sudah terbukti.
> Ditulis 2026-09-05, berlaku untuk v1.3 (versionCode 4).

---

## 1. IDENTITAS PROYEK

| Item | Nilai |
|---|---|
| Nama aplikasi | Nusantara CCTV Monitor |
| Package | `id.nusantara.cctv` |
| Repo | https://github.com/xDvnz/nusantara-cctv (public, akun `xDvnz`) |
| Root lokal | `C:\Users\Administrator\Documents\cctv` |
| Platform | Android native (BUKAN WebView) — Kotlin + Jetpack Compose + Material 3 |
| minSdk / targetSdk | 26 (Android 8.0) / 36 |
| Versi kini | 1.3 (versionCode 4) |
| Tujuan | Menampilkan CCTV publik resmi pemerintah daerah Indonesia: daftar, peta, pencarian, filter, live player, favorit, riwayat |

**Prinsip yang tidak boleh dilanggar:**
1. Legalitas sumber di atas segalanya — hanya stream publik resmi pemda. Tidak ada bypass auth, tidak ada kamera privat, tidak ada credential di repo.
2. 500 kamera valid > 50.000 kamera palsu.
3. Bukan WebView wrapper. Player native Media3.
4. Tidak ada placeholder/TODO()/fake data di production path (aturan antislop).
5. Aplikasi hanya MENAUTKAN stream; tidak merekam, tidak mirror, tidak re-encode.

---

## 2. SEJARAH PENGERJAAN (urutan kejadian nyata)

Fase-fase ini SUDAH selesai. Agent baru cukup pahami konteksnya, jangan dikerjakan ulang.

### Fase riset asal-usul (obrolan awal)
1. User menanyakan situs agregator `cctvmalang.24hour.id` → ditelusuri: player hls.js memanggil `proxy.php?id=<stream_id>` di domain sama; kebocoran error 404 Tomcat membocorkan upstream = **Ant Media Server** (`/WebRTCApp/streams/`). Kesimpulan: 24hour.id hanyalah perantara; sumber asli = cctv.malangkota.go.id.
2. Portal resmi Malang dibedah: `POST /api/v2/get-cameras` (butuh cookie sesi publik dari GET halaman + header `X-Requested-With: XMLHttpRequest`), stream `GET /cctv-stream/streams/{stream_id}.m3u8` (butuh cookie + referer).
3. Riset nasional (agent paralel + probe manual) menghasilkan daftar portal hidup/mati. Yang lolos: Jogja, Palembang, Banjarmasin, Kota Bandung. Yang ditolak + alasannya ada di §6.

### Fase build aplikasi (master prompt 32 fase)
Dikerjakan bertahap PHASE 0–9 sesuai file `MASTER PROMPT — NATIVE ANDROID INDONESIA PUBLIC CCTV MONITORING APP.md` (masih ada di root repo — jangan dihapus, itu spesifikasi asli user):
- PHASE 0 audit environment → `docs/PHASE0-ENVIRONMENT.md`
- PHASE 1 riset + data model → `docs/research/METHODOLOGY.md`, `docs/data-sources/catalog-schema.json`
- PHASE 2 pipeline Python → `tools/` (discovery/validation/export), katalog `data/cameras.json`
- PHASE 3–8 aplikasi Android lengkap
- PHASE 9 QA: 14→17 unit test, lint, uji emulator penuh (screenshot bukti di `data/shots/`, TIDAK di-commit)

### Riwayat rilis
| Versi | Isi | Catatan penting |
|---|---|---|
| v1.0 | Rilis perdana, 481 kamera, 5 sumber | APK release semula unsigned → diubah signed kunci debug agar installable |
| v1.1 | Tab Tentang, tema gelap/terang, bahasa ID/EN, riwayat tonton 2 kolom, update checker | Tab Wilayah + tombol gear DIHAPUS permanen atas permintaan user; Room migrasi v1→v2 (tabel camera_history) |
| v1.2 | Fix kamera Malang 403 (3 bug berlapis — lihat §7) | Ini bug paling berbahaya yang pernah ada; WAJIB pahami §7 |
| v1.3 | Update check andal (delay+retry) + tombol cek manual di Tentang | gh upload asset sempat gagal gara-gara jaringan — pakai retry loop |

---

## 3. ENVIRONMENT MESIN KERJA

- Windows 10 (10.0.19045), shell Git Bash. JDK 21. Python 3.11 + requests.
- Android SDK: `C:/Users/Administrator/AppData/Local/Android/Sdk` (platforms 29/34/36/36.1; build-tools 34–37).
- Gradle TIDAK ada di PATH → selalu pakai wrapper `./gradlew.bat` dari folder `android/`. Distribusi 8.11.1 sudah cache.
- **`android/local.properties` WAJIB pakai forward slash**: `sdk.dir=C:/Users/Administrator/AppData/Local/Android/Sdk` (backslash = error "filename/directory syntax is incorrect"). File ini TIDAK di-commit.
- AVD: `Pixel_API35` (dipakai QA) dan `Pixel_API29`. Emulator sering mati sendiri — nyalakan: `"C:/Users/Administrator/AppData/Local/Android/Sdk/emulator/emulator.exe" -avd Pixel_API35 -no-snapshot -no-boot-anim -gpu auto &` lalu tunggu `sys.boot_completed=1` (±60–70 dtk).
- gh CLI sudah login akun `xDvnz` (scope repo). Push kadang timeout — cukup ulangi.

### Jebakan emulator yang sudah terbukti
- Screenshot: `adb exec-out screencap -p > file.png` (bukan `adb shell screencap` — arg salah di API 35).
- Input teks ke field Compose sering GAGAL via `adb shell input text` — bukan bug app. Uji pencarian teks lewat unit test DAO, atau pakai filter chip (klik).
- Tap pertama setelah `am start` sering meleset (UI belum siap) → selalu `sleep` 12–16 dtk dan verifikasi screenshot SEBELUM tap berikutnya.
- Koordinat layar emulator 1080x2400. Screenshot kadang di-downscale — hitung ulang rasio sebelum tap.
- Downgrade install: `adb install -r -d <apk>` (tanpa `-d` ditolak karena versionCode turun).
- `adb shell getprop sys.boot_completed` → `1` baru boleh `am start` (kalau belum, error "Too early to start activity").

---

## 4. STRUKTUR REPO

```text
MASTER PROMPT — ....md          Spesifikasi asli user (32 fase) — JANGAN DIHAPUS
AGENTS.md                       Dokumen ini
README.md                       Ringkasan publik + cara build/pipeline
docs/
  PHASE0-ENVIRONMENT.md         Hasil audit environment
  PROJECT-STATUS.md             Laporan akhir v1.0 (angka validasi katalog)
  ARCHITECTURE.md (di architecture/)
  DATA-SOURCES.md (di data-sources/)  Detail semua portal + yang ditolak + alasan
  catalog-schema.json           Skema JSON katalog
  METHODOLOGY.md (di research/) Prosedur riset/verifikasi
  TESTING.md  PRIVACY.md  LICENSES.md  DEVELOPMENT.md  RELEASING.md
tools/                          Pipeline Python (lihat §5)
  common.py                     Helper: session TLS-fallback, stable_id, log
  discovery/<sumber>.py         Fetcher per kota + run_all.py
  validation/validate.py        Validasi stream (magic byte, bukan HTTP 200 saja)
  import_export/export.py       Normalisasi + dedup + output JSON/CSV
data/
  cameras.json                  KATALOG FINAL (v4: 481 kamera, 466 online) — di-commit
  cameras.csv                   Versi CSV — di-commit
  raw/, validated_raw.json, validation*.log, shots/  — TIDAK di-commit (regenerable)
android/
  app/src/main/kotlin/id/nusantara/cctv/
    CctvApp.kt                  Application + AppContainer (DI manual — lihat §8)
    MainActivity.kt             AppCompatActivity (per-app locale + tema)
    data/
      model/Models.kt           Camera, CameraSourceConfig, CatalogVersion
      catalog/CatalogDto.kt     DTO kotlinx-serialization katalog
      catalog/CatalogRepository.kt  seed, sync remote, query, favorit, riwayat
      db/CctvDatabase.kt        Room v2 (cameras, favorites, sources, catalog_meta, camera_history)
      api/SourceHttp.kt         OkHttp + CookieStore + UA konsisten + bootstrap sesi
      player/StreamEngine.kt    Adapter format (HLS/DASH/MJPEG/RTSP→Unsupported)
      player/StreamPlayerController.kt  Pemilik ExoPlayer + PlayerUi state
      player/MjpegDecoder (di StreamEngine.kt bagian bawah)
      prefs/AppPreferences.kt   DataStore: ThemeMode, AppLocale, remoteCatalogUrl
      update/UpdateChecker.kt   Cek GitHub releases/latest + VersionCompare
    ui/
      AppRoot.kt                NavHost 5 tab + UpdateDialog + OfflineBanner
      home/ search/ map/ favorites/ about/ detail/ components/ theme/
    res/values/strings.xml      Indonesia (DEFAULT)
    res/values-en/strings.xml   English — WAJIB sinkron dengan id (setiap string baru: dua bahasa!)
    res/xml/locales_config.xml  id + en
    res/xml/network_security_config.xml  Anchor intermediate GeoTrust utk malangkota
    res/raw/geotrust_tls_rsa_ca_g1.pem   Intermediate publik DigiCert (bukan secret)
    assets/catalog/cameras.json SEED katalog (salinan data/cameras.json)
  app/src/test/kotlin/...       17 unit test (parsing, DB, MJPEG, VersionCompare)
dist/NusantaraCCTV-vX.Y-*.apk   Artefak rilis — di-commit
```

---

## 5. PIPELINE DATA (regenerasi katalog)

### Prosedur lengkap (urutan WAJIB)
```bash
cd C:/Users/Administrator/Documents/cctv
python tools/discovery/run_all.py        # 1. fetch semua sumber → data/raw/*.json
python tools/validation/validate.py      # 2. uji stream semua kamera
#    varian: --only <source_id>  --limit N
python tools/import_export/export.py     # 3. dedup+normalisasi → data/cameras.json + .csv
cp data/cameras.json android/app/src/main/assets/catalog/cameras.json   # 4. seed APK
cd android && ./gradlew.bat assembleDebug   # 5. rebuild
```
`catalog_version` naik otomatis tiap regenerasi. Perluasan: fetcher baru = file `tools/discovery/<kota>.py` (fungsi `fetch_all()`) + daftar di `run_all.py::FETCHERS` + entri `SOURCE_META` di `export.py` + entri `SOURCES` di `validate.py`.

### Aturan validasi stream (SUCI — jangan dilonggarkan)
ONLINE hanya jika: manifest 200 + body `#EXTM3U` + **satu segment termanifest benar-benar video**: magic byte MPEG-TS `0x47` ATAU box fMP4 (`ftyp`/`moof`/`styp`/`moov`/`mdat`) dalam 16 byte pertama. HTTP 200 saja TIDAK cukup (halaman 404 HTML juga balas 200 dengan content-type palsu di beberapa server).
**Jebakan fMP4:** segment Banjarmasin dimulai box `moof` BUKAN `ftyp` — cek semua box, jangan hanya ftyp.

### Profil per sumber (semua sudah terverifikasi berkali-kali)
| source_id | Kamera | Endpoint | Kebutuhan akses | Quirk |
|---|---|---|---|---|
| malangkota | 253 | POST `/api/v2/get-cameras` + GET `/cctv-stream/streams/{id}.m3u8` | cookie sesi publik (bootstrap GET `/sebaran-cctv`) + header XHR + referer + **UA konsisten dengan bootstrap** | TLS tanpa intermediate; server mengikat sesi ke UA; 3-4 cookie wajib |
| jogjakota | 147 | GET `/home/getdata` | header XHR saja | Wowza TS 720p; ambil hanya `cctv_status=0` (publik) |
| palembang | 30 | GET `/api/cctv` | referer | **AGRESIF RATE-LIMIT** → validasi serial 1 rps, delay 1 dtk antar kamera |
| banjarmasin | 30 | GET `/api/public/cameras/wall` | referer (tanpa ini kadang balas `[]`) | HLS fMP4; koordinat tidak ada → administrative_only |
| bandungkota | 21 | POST `/ajax/cctv-list` (per area id dari homepage) lalu POST `/ajax/cctv-info` per kamera | header XHR | stream di port 1990 path nama kamera; tanpa koordinat; delay 0.4s antar request |

### Sumber yang DITOLAK (jangan coba lagi tanpa alasan baru)
- smartcity.jakarta.go.id, atcs.denpasarkota.go.id → API butuh token (AUTH_REQUIRED)
- Bali Satu Data → HLS hanya via embed player, host media tak resolvable publik
- dishub.bandungkab.go.id → token HLS per-load halaman (tak stabil utk katalog statis)
- cctv.salatiga.go.id → streamer internal-only (NXDOMAIN)
- cctv.medan.go.id → Cloudflare challenge
- Domain mati: scc.surabaya, komando.bandung, pso/cc.semarang, atcs.kemenhub, cctv.{makassar,tangerangkota,bekasikota,kotabogor,depok}
- 24hour.id → agregator pihak ketiga; kita pakai sumber resmi langsung

---

## 6. ARSITEKTUR ANDROID (keputusan + alasan)

- **DI manual** (`AppContainer` di `CctvApp.kt`) — graf kecil; Hilt ditolak (master prompt: jangan library cuma karena populer).
- **MVVM + repository** proporsional. ViewModel dibuat via `ui/ViewModelFactory.kt::factoryOf` + extension `CreationExtras.appContainer`.
- **Room v2** dengan `MIGRATION_1_2` non-destruktif (CREATE TABLE camera_history). JANGAN pakai fallbackToDestructiveMigration utk upgrade naik — data user (favorit/riwayat) harus selamat.
- **Mode katalog**: hybrid (Mode C). Seed aset saat DB kosong → Room. Remote sync opsional via URL di Tentang (`CatalogRepository.updateRemoteUrl` + `syncFromRemote`, transaksional, favorit aman karena key id stabil).
- **Peta osmdroid** (bukan Google Maps — butuh API key = secret di APK, dilarang). Clustering grid screen-space sendiri (`ui/map/Clusterer.kt`, cell 90dp). **Marker WAJIB `setBounds`** — tanpa itu marker tidak tergambar (bug nyata v1.0).
- **Player**: `StreamEngine.resolve(camera)` → `Playable` (Exo factory / Mjpeg Flow / Unsupported(messageRes)). `StreamPlayerController` memegang ExoPlayer + StateFlow `PlayerUi` (Idle/Loading/Playing/Error(kind,...)/MjpegFrame). Dua konsumen: detail inline & fullscreen (masing-masing controller sendiri; yang satu wajib `release()` saat navigasi).
- **RTSP tidak diputar** — `Unsupported` dengan pesan jelas (butuh relay resmi, §10 master prompt).
- **i18n**: Indonesia = `values/` DEFAULT, English = `values-en/`. Semua teks UI via `stringResource`. Error player dikodekan `PlayerError` + resource string (lihat `ui/detail/PlayerErrorText.kt`). **String baru wajib dua bahasa, jangan typo** (user menegaskan: bahasa tidak boleh salah tulis).
- **Tema**: `ThemeMode` (SYSTEM/LIGHT/DARK) di DataStore → `NusantaraTheme(themeMode)`. `MainActivity` = `AppCompatActivity` (perlu untuk per-app locale AppCompat). Bahasa via `AppCompatDelegate.setApplicationLocales` + `autoStoreLocales` service di manifest.
- **Notifikasi update**: cek `releases/latest` saat AppRoot aktif (delay 4 dtk + retry 1x) → `UpdateDialog` bila remote > lokal (VersionCompare numerik per komponen). Manual: kartu Pembaruan di Tentang.

---

## 7. ★ BUG MALANG 403 — PELAJARAN TERPENTING (3 lapis, v1.2)

Kamera Malang gagal diputar (status AUTH_REQUIRED / error Stream tidak tersedia). Diagnosa bertahap di emulator menemukan TIGA akar yang bertumpuk. Pahami semuanya sebelum menyentuh kode jaringan:

1. **Sesi cookie terikat User-Agent.** Bootstrap sesi (OkHttp default UA `okhttp/4.x`) ≠ request stream (UA mobile lain) → server tolak 403. Bukti eksperimen curl: UA sama-sama desktop=200, sama-sama mobile=200, CAMPUR=403.
   → Fix: SATU UA browser (`SourceHttp.USER_AGENT`, Chrome mobile) dipakai OkHttp (interceptor) DAN `DefaultHttpDataSource.setDefaultRequestProperties` DAN probeStatus.
2. **Cookie gabungan salah dipecah.** `java.net.CookieManager.get()` mengembalikan SATU header `"k1=v1; k2=v2; k3=v3"`; `CookieJarBridge.loadForRequest` semula parse string itu sebagai satu cookie → hanya cookie pertama terkirim. Malang butuh 3–4 cookie (`ci_session`, `NANCY_TOKEN_W/Q`, `nancy_auth_v1c`).
   → Fix: split `;` lalu parse per pasangan (file `SourceHttp.kt`).
3. **`NetworkOnMainThreadException` tertelan.** `headersFor()` dipanggil dari main thread → `ensureSession` (network blocking) melempar → ditelan `runCatching` → sesi tak pernah terbentuk, tanpa jejak log.
   → Fix: `StreamEngine.resolve()` + bootstrap jalan di `Dispatchers.IO` (dari `StreamPlayerController.start` coroutine); ExoPlayer dibangun di main SETELAH resolve selesai; guard `startedForId != camera.id` untuk batal jika user keluar saat resolve.
4. (**bonus race**) Config sumber (bootstrap/referer per sumber) dibaca dari cache `AppContainer.sourceConfigs` yang bisa kosong sebelum seed DB selesai → fix: preload SETELAH `seedFromAssetsIfNeeded()` di `CctvApp.onCreate` + fallback blocking query per source di `sourceConfigOf`.

**Aturan turunan (WAJIB):** network blocking selalu di IO; jangan pernah menelan exception network dengan runCatching tanpa log; UA sumber mana pun harus tunggal & konsisten sejak bootstrap sampai segment.

### Bug lain yang pernah ditemukan & fixnya (jangan diulang)
- Room `search()` (query non-suspend) dipanggil dari main → crash `Cannot access database on main thread`. Fix: `withContext(Dispatchers.IO)`. Semua query DAO blocking harus dibungkus.
- Field pencarian tertinggal (lag) karena terikat state ter-debounce → field pakai state lokal + `vm.onQueryChange()` tiap ketikan.
- `local.properties` backslash → error path Gradle.
- Media3 `UnsafeOptInUsageError` lint → `@OptIn(UnstableApi::class)` di kelas player.
- Import `id.nusantara.cctv.R`/`stringResource`/`BuildConfig` sering lupa saat edit manual — cek dulu saat compile error "Unresolved reference".
- Keyboard emulator tak bisa input teks (lihat §3).

---

## 8. BUILD, TEST, RILIS

### Build harian
```bash
cd android
./gradlew.bat assembleDebug          # APK debug
./gradlew.bat assembleRelease        # APK release (minify+shrink, signed kunci debug)
./gradlew.bat testDebugUnitTest      # 17 unit test — semua HARUS hijau
./gradlew.bat lint                   # 0 error — WAJIB
```
Rilis = dua-duanya hijau + QA emulator + screenshot bukti.

### QA emulator standar sebelum rilis
1. `adb install -r app-debug.apk` → launch → 0 FATAL di logcat.
2. Beranda: stat card, riwayat muncul setelah buka kamera.
3. Buka kamera (Cari → filter Provinsi → tap kamera): **kamera Malang WAJIB dites** (kasus paling rapuh — cookie) + satu kamera Jogja/Banjarmasin.
4. Peta: marker/cluster tampil, tap cluster zoom.
5. Tentang: tema ganti, bahasa ganti (teks berubah penuh), Cek pembaruan jalan.
6. Rotasi fullscreen, favorit toggle, back navigasi.

### Prosedur rilis versi baru (urutan PASTI)
1. `android/app/build.gradle.kts`: `versionName` = rilis baru, `versionCode` +1.
   **Kebijakan versi (perintah user):** fix/update kecil → naikkan minor (1.3→1.4); perubahan besar/breaking (skema katalog tak kompatibel, ganti keystore, rombak besar) → major (2.0). APK selalu dinamai `NusantaraCCTV-v<X.Y>-release.apk` — JANGAN timpa file rilis lama.
2. Build + test + lint + QA (atas).
3. Commit + tag + push:
   ```bash
   git add -A
   git commit -m "feat|fix: v<X.Y> — ringkasan"
   git tag v<X.Y>
   git push origin main --tags
   ```
4. Rilis + upload (JARINGAN KE GITHUB SERING FLAKY — pakai retry loop):
   ```bash
   cp android/app/build/outputs/apk/release/app-release.apk dist/NusantaraCCTV-v<X.Y>-release.apk
   cp android/app/build/outputs/apk/debug/app-debug.apk   dist/NusantaraCCTV-v<X.Y>-debug.apk
   git add dist && git commit -m "chore: v<X.Y> dist artifacts" && git push
   for i in 1 2 3; do gh release create v<X.Y> dist/NusantaraCCTV-v<X.Y>-release.apk dist/NusantaraCCTV-v<X.Y>-debug.apk --title "..." --notes "..." && break || sleep 8; done
   ```
5. Catatan rilis bilingual-sederhana, sebut cara install (upgrade di atas versi lama, tanpa uninstall).
6. Setelah release: pengguna versi lama otomatis dapat dialog update (fitur v1.1+). Pengguna v1.0 tidak punya checker — sebut manual di notes bila relevan.

**Signing:** release ditandatangani KUNCI DEBUG (`~/.android/debug.keystore`, storePassword/keyPassword `android`, alias `androiddebugkey`) supaya installable sideload; keystore TIDAK di-commit (`.gitignore` menutup `*.keystore/*.jks`). Bila kelak publish ke Play Store → buat keystore produksi (itu = major 2.0 karena pengguna harus uninstall/reinstall).

---

## 9. KONVENSI KODE

- Bahasa komentar/log internal: Indonesia ringkas. String UI: resource dua bahasa.
- Penamaan: `CamelCase` fungsi/variabel, jangan singkatan aneh. File Kotlin satu-class-utama.
- Error handling: fail-soft di UI (state error jelas), tapi JANGAN telan exception diam-diam — minimal log.
- Komentar hanya untuk konstrain non-obvious (contoh bagus: komentar UA-binding di SourceHttp).
- Dependency baru: hanya bila terbukti perlu (maintenance/compat/lisensi) — sikap yang sama seperti keputusan menolak Hilt.
- Test: setiap fix bug regression-test bila mungkin (contoh: VersionCompareTest lahir dari fitur update; history DAO test lahir dari fitur riwayat).

---

## 10. LIMITASI YANG DIKETAHUI (jangan dianggap bug)

1. Status kamera = snapshot validasi pipeline terakhir; app TIDAK polling massal (hanya tombol periksa per kamera, on-demand).
2. Bandung & Banjarmasin tanpa koordinat → tak tampil di peta (masih bisa dicari/diputar).
3. Stream bisa mati kapan pun (milik operator) → UI error + tombol muat ulang.
4. MJPEG adapter + decoder ada + teruji unit, tapi belum ada sumber publik yang memakainya.
5. Keyboard emulator (bukan app) — lihat §3.
6. RTSP unsupported by design.

---

## 11. KANDIDAT PENGERJAAN LANJUTAN (belum diminta — jangan kerjakan tanpa perintah user)

- Snapshot/poster kamera nyata di kartu riwayat (kini ikon videocam + status dot) — perlu fetch gambar poster (Banjarmasin punya field poster).
- GPS "kamera terdekat" (permission sudah disiapkan di manifest tapi fitur belum ada; PRIVACY.md sudah menjanjikan perilaku izin).
- Widget/shortcut favorit, notifikasi kamera favorit online.
- Tambah kota baru via pipeline (§5) — kandidat riset ulang: portal yang dulu mati bisa hidup lagi.
- Mode multi-view (beberapa kamera sekaligus) — hati-hati bandwidth & batas player.

---

## 12. MEMORY PENGGUNA (konteks soft-skill)

- User berkomunikasi bahasa Indonesia, ingin balasan super-ringkas (gaya caveman) — tapi DOKUMEN/commit message tetap normal & lengkap.
- User meminta: kerja terstruktur, audit dulu sebelum perbaiki, tidak boleh ada yang ke-skip, bug harus diperbaiki segera setelah ketemu.
- Rilis GitHub = tempat user mengunduh; pola penamaan versi adalah perintah eksplisit user (lihat §8).

---

## 13. CHECKLIST AGENT BARU (hari pertama)

- [ ] Baca dokumen ini sampai habis.
- [ ] `cd android && ./gradlew.bat assembleDebug testDebugUnitTest lint` — pastikan hijau di environment-mu.
- [ ] `adb devices` — siapkan emulator bila perlu (§3).
- [ ] Buka `docs/DATA-SOURCES.md` + §5-§7 dokumen ini — pahami quirk sumber sebelum sentuh kode jaringan.
- [ ] Jangan refactor besar-besaran tanpa alasan; struktur sekarang sudah melalui banyak iterasi bug nyata.

Selamat meneruskan. Proyek ini sudah berdiri di atas bukti kerja nyata — jaga standarnya.
