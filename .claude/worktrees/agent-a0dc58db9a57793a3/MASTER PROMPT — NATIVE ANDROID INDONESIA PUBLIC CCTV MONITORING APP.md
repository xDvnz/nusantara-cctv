# MASTER PROMPT — NATIVE ANDROID INDONESIA PUBLIC CCTV MONITORING APP

Kamu adalah **senior Android engineer + data engineer + OSINT/public-data researcher + QA engineer** yang bertanggung jawab membangun aplikasi Android native dari **nol sampai benar-benar terinstall dan berjalan di perangkat Android**.

## 1. TUJUAN UTAMA

Bangun aplikasi **native Android**, BUKAN WebView, untuk memantau sebanyak mungkin **CCTV publik di seluruh Indonesia** yang dapat diakses secara legal melalui sumber yang memang dipublikasikan untuk masyarakat atau diberikan oleh pemilik/operator secara resmi.

Target aplikasi:

- Indonesia sebagai cakupan utama.
- Kamera dikelompokkan berdasarkan:
  - Provinsi
  - Kabupaten/Kota
  - Kecamatan
  - Nama lokasi
  - Koordinat latitude/longitude jika tersedia
- Menampilkan kamera pada:
  - daftar
  - pencarian
  - filter wilayah
  - peta
  - halaman live monitoring
- Mendukung berbagai format stream yang memang tersedia secara publik, misalnya:
  - HLS
  - DASH
  - MJPEG
  - RTSP melalui arsitektur yang sesuai
  - API resmi yang memberikan URL stream
- Aplikasi harus mampu menangani kamera offline/error tanpa membuat aplikasi crash.

PRINSIP UTAMA:

> Jangan membuat WebView wrapper. Buat aplikasi Android native yang benar-benar menggunakan komponen Android native dan player native.

---

# 2. ATURAN KEAMANAN DAN LEGALITAS

Saat melakukan discovery CCTV:

### BOLEH

Cari sumber seperti:

- portal pemerintah
- Dishub
- Command Center
- Diskominfo
- portal smart city
- open data pemerintah
- API publik
- dokumentasi resmi
- halaman CCTV yang memang ditujukan untuk publik
- URL stream yang memang dipublikasikan
- dataset/open-data berisi lokasi kamera
- sumber yang memberikan izin penggunaan data/stream

Jika sebuah sumber memberikan:

- URL HLS publik
- URL MJPEG publik
- endpoint API publik
- identifier kamera publik
- metadata lokasi publik

maka dokumentasikan dan gunakan sesuai ketentuan sumber tersebut.

### DILARANG

Jangan:

- bypass login
- brute force password
- mengambil password CCTV
- mencuri session/cookie
- mengekstrak credential privat
- menebak API key rahasia
- mengeksploitasi vulnerability CCTV
- bypass authentication/authorization
- menggunakan kamera yang jelas bukan untuk konsumsi publik
- melakukan scanning agresif terhadap alamat IP/port internet untuk mencari kamera
- menggunakan credential bocor
- mengakses kamera yang membutuhkan autentikasi jika tidak ada izin penggunaan
- melakukan tindakan yang mengubah konfigurasi kamera/server.

Fokuskan discovery pada **publicly documented/publicly exposed feeds intended for public viewing**.

Jika menemukan sumber yang membutuhkan API key:

1. cek apakah key tersebut memang merupakan public client key yang didokumentasikan;
2. jangan mengambil secret dari aplikasi/server secara ilegal;
3. gunakan API resmi sesuai dokumentasinya;
4. jika membutuhkan kredensial privat, tandai sumber tersebut sebagai `AUTH_REQUIRED` dan jangan digunakan.

---

# 3. SEBELUM MENULIS KODE

Jangan langsung membuat aplikasi.

Pertama lakukan audit proyek.

## 3.1 Audit environment

Periksa laptop tempat kamu bekerja:

- OS
- Android Studio
- Android SDK
- Java/JDK
- Kotlin
- Gradle
- Android emulator
- perangkat Android yang terhubung melalui ADB
- Git
- Python/Node.js jika diperlukan
- tools networking yang relevan
- tools data-processing
- tools testing
- environment variables
- repository saat ini

## 3.2 WAJIB AUDIT SEMUA SKILL YANG SUDAH TERINSTALL

Cari dan baca skill/agent/tooling yang tersedia di environment.

Jangan mengasumsikan nama skill.

Cari seluruh skill yang tersedia, termasuk namun tidak terbatas pada:

- Ponytail
- Antislop
- UI/UX skill
- Android development skill
- coding skill
- testing skill
- debugging skill
- research skill
- browser/search skill
- agent orchestration skill
- Git/GitHub skill
- documentation skill
- security skill
- QA skill
- deployment skill

Gunakan skill yang relevan sepanjang proses.

Jika terdapat skill yang berfungsi sebagai:

- planner
- researcher
- Android specialist
- UI/UX specialist
- code reviewer
- tester
- debugger
- anti-slop/code-quality reviewer

integrasikan ke workflow.

Jangan hanya mengklaim skill digunakan. Benar-benar gunakan capability yang tersedia.

---

# 4. ARCHITECTURE FIRST

Sebelum implementasi, tentukan architecture terbaik.

Default yang diprioritaskan:

- Kotlin
- Jetpack Compose
- Material 3
- MVVM / Clean Architecture yang proporsional
- Coroutines
- Flow
- Hilt
- Retrofit/OkHttp
- Room
- Media3 / ExoPlayer
- Google Maps atau alternatif map provider
- WorkManager bila membutuhkan background synchronization

Namun jangan menggunakan library hanya karena populer.

Evaluasi dependency berdasarkan:

- maintenance
- compatibility
- performance
- security
- Android version support
- license
- build stability

Dokumentasikan alasan pemilihan stack.

---

# 5. RESEARCH CCTV INDONESIA

Buat pipeline discovery data.

Target:

> Mengumpulkan sebanyak mungkin CCTV publik yang dapat ditemukan secara legal di seluruh wilayah Indonesia.

Jangan menganggap jumlah tertentu sebagai target bila memang tidak tersedia.

Prioritaskan kualitas dan validitas data.

## 5.1 Hierarki sumber

Research secara bertingkat:

### Level 1 — Pemerintah pusat

Cari sumber resmi dari:

- kementerian
- lembaga
- portal data nasional
- portal open data
- layanan transportasi nasional
- portal informasi publik

### Level 2 — Pemerintah daerah

Untuk setiap provinsi, lakukan discovery:

- website pemerintah provinsi
- Diskominfo
- Dishub
- Command Center
- smart city
- open data

### Level 3 — Kabupaten/Kota

Cari:

- CCTV lalu lintas
- public traffic monitoring
- command center
- smart city camera
- public CCTV
- live camera

### Level 4 — Infrastruktur publik

Cari sumber resmi yang menyediakan kamera pada:

- jalan
- persimpangan
- terminal
- pelabuhan
- kawasan wisata
- fasilitas publik
- transportasi

---

# 6. STRATEGI RESEARCH

Jangan melakukan pencarian hanya dengan satu keyword.

Gunakan variasi seperti:

- CCTV [nama daerah]
- CCTV lalu lintas [nama daerah]
- live CCTV [nama daerah]
- traffic camera [nama daerah]
- command center [nama daerah]
- smart city CCTV [nama daerah]
- Dishub CCTV [nama daerah]
- public camera [nama daerah]
- live camera [nama daerah]
- CCTV streaming [nama daerah]

Lakukan discovery dalam skala:

Indonesia
→ Provinsi
→ Kabupaten/Kota
→ Kecamatan
→ lokasi kamera.

Jika dataset resmi menyediakan koordinat, gunakan koordinat asli.

Jangan mengarang koordinat.

---

# 7. DATABASE CCTV

Buat schema database yang rapi.

Minimal field:

```text
id
camera_name
camera_code
province
city_regency
district
subdistrict
location_name

latitude
longitude

source_name
source_url
operator
access_type

stream_type
stream_url

api_endpoint
public_identifier

status
last_checked

timezone

license
terms_of_use

confidence_score
notes

created_at
updated_at
```

Tambahkan field lain apabila diperlukan.

`access_type` misalnya:

```text
PUBLIC_DIRECT
PUBLIC_API
PUBLIC_EMBED
OFFICIAL_PORTAL
AUTH_REQUIRED
UNAVAILABLE
UNKNOWN
```

Jangan memasukkan `AUTH_REQUIRED` ke katalog publik aplikasi kecuali aplikasi memiliki izin dan mekanisme autentikasi resmi.

---

# 8. GEOLOCATION

Untuk setiap kamera:

1. Gunakan latitude/longitude resmi bila tersedia.
2. Jika hanya tersedia alamat:
   - geocode dengan sumber yang legal.
3. Simpan confidence score.
4. Jangan mengarang lokasi.
5. Bedakan:
   - exact coordinate
   - approximate coordinate
   - administrative location only

Contoh:

```json
{
  "latitude": -7.983908,
  "longitude": 112.621391,
  "location_accuracy": "exact"
}
```

---

# 9. STREAM VALIDATION

Setiap sumber harus divalidasi.

Validasi:

- URL dapat diakses
- HTTP response valid
- content type benar
- stream benar-benar video
- codec didukung
- resolusi
- frame rate
- latency jika dapat diukur
- kestabilan
- apakah stream membutuhkan authentication
- apakah sumber masih aktif

Gunakan status:

```text
ONLINE
OFFLINE
TIMEOUT
AUTH_REQUIRED
INVALID_STREAM
MOVED
UNKNOWN
```

Jangan menandai kamera ONLINE hanya karena HTTP 200.

Pastikan benar-benar dapat diputar.

---

# 10. STREAM ADAPTER ARCHITECTURE

Jangan membuat player dengan asumsi semua CCTV menggunakan format yang sama.

Buat abstraction:

```text
CameraSource
    ├── HLSCameraSource
    ├── DashCameraSource
    ├── MjpegCameraSource
    ├── RtspCameraSource
    └── ApiCameraSource
```

Untuk RTSP, pahami bahwa Android client biasa tidak selalu dapat memutarnya secara native melalui Media3 tanpa konfigurasi/dukungan tambahan.

Jika diperlukan, buat arsitektur:

```text
RTSP
   ↓
Authorized relay/transcoder
   ↓
HLS
   ↓
Android Media3
```

Relay hanya boleh digunakan untuk stream yang memang boleh diproses/ditransmisikan ulang.

Jangan melakukan transcoding terhadap stream yang dilarang oleh operator atau terms sumber.

---

# 11. BACKEND / DATA CATALOG

Evaluasi apakah aplikasi lebih baik:

### Mode A

Database CCTV dibundel ke aplikasi.

### Mode B

Aplikasi mengambil catalog dari server.

### Mode C

Hybrid:

- baseline catalog lokal
- update catalog dari server
- cache lokal dengan Room

Pilih arsitektur berdasarkan:

- jumlah kamera
- bandwidth
- update frequency
- reliability
- offline support
- scalability.

Buat sistem versioning database agar aplikasi dapat mendeteksi update katalog.

---

# 12. FITUR APLIKASI

Aplikasi minimal memiliki:

## Home

Menampilkan:

- jumlah kamera
- kamera online
- kamera offline
- wilayah populer
- kamera terbaru
- favorite camera

## Indonesia Map

Map dengan marker CCTV.

Marker:

- cluster jika terlalu banyak
- warna/status berdasarkan online/offline
- tap marker membuka camera detail

## Province

Daftar provinsi.

Contoh:

```text
Jawa Timur
Jawa Barat
Jawa Tengah
DKI Jakarta
Bali
Sumatera Utara
...
```

## Kabupaten/Kota

Setelah memilih provinsi:

```text
Malang
Surabaya
Kediri
Blitar
...
```

## Kecamatan

Tampilkan jika metadata tersedia.

## Search

Cari berdasarkan:

- nama kamera
- lokasi
- provinsi
- kabupaten
- kecamatan
- kode kamera

## Camera Detail

Tampilkan:

- nama
- lokasi
- wilayah
- koordinat
- source/operator
- status
- live video
- refresh
- favorite
- buka lokasi pada map

## Fullscreen Player

Fitur:

- fullscreen
- orientation
- pause/play
- reconnect
- loading state
- error state
- retry
- network status

---

# 13. UI/UX

Gunakan UI Android modern.

Jangan membuat UI generik atau template kosong.

Gunakan skill UI/UX yang tersedia di environment.

Audit:

- typography
- spacing
- hierarchy
- loading state
- empty state
- error state
- accessibility
- dark mode
- touch target
- animation
- navigation

Pastikan aplikasi terasa seperti aplikasi Android native modern.

---

# 14. PERFORMA

Aplikasi harus mempertimbangkan kemungkinan ribuan kamera.

Jangan:

- load seluruh stream sekaligus
- membuat player untuk setiap marker
- melakukan polling semua kamera setiap detik
- menyimpan object besar di memory
- melakukan network request di UI thread

Gunakan:

- lazy loading
- pagination
- clustering
- caching
- coroutine
- lifecycle-aware player
- connection management.

---

# 15. FAVORITE

User dapat menyimpan kamera favorit.

Room:

```text
FavoriteCamera
```

Favorite tetap tersedia walaupun internet sementara mati.

---

# 16. SEARCH DAN FILTER

Implementasikan filter:

```text
Provinsi
Kabupaten/Kota
Kecamatan
Status
Jenis stream
Operator
```

Sorting:

```text
Distance
Name
Province
Online status
Recently added
```

Jika user memberikan lokasi GPS:

> tampilkan kamera terdekat.

GPS hanya digunakan setelah meminta permission Android yang sesuai.

---

# 17. OFFLINE BEHAVIOR

Aplikasi tetap bisa membuka:

- daftar kamera
- metadata
- favorite
- lokasi

ketika offline.

Live stream tentu tidak dapat diputar tanpa koneksi.

Tampilkan pesan yang jelas.

---

# 18. ERROR HANDLING

Tidak boleh ada crash ketika:

- stream mati
- URL berubah
- timeout
- server menolak request
- codec tidak didukung
- internet putus
- API gagal
- database corrupt
- GPS tidak tersedia
- permission ditolak.

Semua error harus mempunyai UI state yang dapat dipahami user.

---

# 19. PRIVACY

Jangan mengumpulkan data user yang tidak dibutuhkan.

Jika menggunakan:

- GPS
- analytics
- crash reporting

pastikan penggunaannya jelas.

Jangan menyimpan lokasi user kecuali memang diperlukan dan telah dirancang demikian.

---

# 20. SECURITY

Implementasikan:

- HTTPS jika tersedia
- certificate/security best practice
- network timeout
- input validation
- secure local storage jika menyimpan credential resmi
- jangan hardcode secret
- jangan expose backend secret di APK
- jangan menyimpan API secret server-side credential di repository.

Jika ada public API key yang memang aman untuk client, dokumentasikan statusnya.

---

# 21. DEVELOPMENT PHASE

Kerjakan dalam fase berikut.

## PHASE 0 — ENVIRONMENT AUDIT

Output:

```text
Android Studio:
JDK:
Gradle:
Kotlin:
SDK:
ADB:
Device:
Installed Skills:
Relevant Skills:
```

Identifikasi semua skill yang dapat membantu proyek.

---

## PHASE 1 — RESEARCH & DATA MODEL

Buat:

```text
docs/research/
docs/data-sources/
docs/architecture/
```

Hasil:

- daftar sumber
- legal/access classification
- database schema
- data pipeline
- architecture decision

Jangan coding fitur besar sebelum fase ini selesai.

---

## PHASE 2 — DATA COLLECTION ENGINE

Buat tool/script untuk mengumpulkan catalog.

Contoh struktur:

```text
tools/
  discovery/
  validation/
  geocoding/
  import/
  export/
```

Output:

```text
data/cameras.json
data/cameras.csv
```

Lakukan deduplication.

Satu kamera tidak boleh muncul berkali-kali hanya karena memiliki beberapa sumber.

---

## PHASE 3 — ANDROID FOUNDATION

Buat project native Android.

Target:

- build berhasil
- app install berhasil
- app launch berhasil.

---

## PHASE 4 — DATABASE

Implement:

- Room
- entities
- DAO
- repository
- migrations
- seed data.

---

## PHASE 5 — UI

Implement:

- Home
- Map
- Region Explorer
- Search
- Camera Detail
- Player
- Favorites
- Settings.

---

## PHASE 6 — STREAM ENGINE

Implement Media3/player abstraction.

Test semua format yang ditemukan.

---

## PHASE 7 — MAP

Implement:

- map
- clustering
- camera marker
- region filter
- camera detail navigation.

---

## PHASE 8 — SYNC SYSTEM

Implement:

```text
Remote catalog
      ↓
validation
      ↓
version check
      ↓
download
      ↓
verify
      ↓
Room
```

Jangan merusak database lokal jika remote update gagal.

---

## PHASE 9 — TESTING

Lakukan:

### Unit test

- parser
- repository
- database
- filtering
- search
- status calculation

### Integration test

- API
- Room
- stream playback
- catalog update

### UI test

- navigation
- search
- filtering
- player
- rotation
- dark mode

### Device test

Install pada Android nyata menggunakan ADB.

---

# 22. QA LOOP

Setiap kali menemukan error:

1. reproduksi
2. identifikasi root cause
3. perbaiki
4. build
5. test
6. regression test.

Jangan hanya menambal error berdasarkan symptom.

Setelah memperbaiki bug, cek apakah perubahan merusak fitur lain.

---

# 23. ANTI-SLOP REQUIREMENT

Gunakan skill Antislop atau code-quality skill yang tersedia.

Review kode untuk mendeteksi:

- TODO palsu
- placeholder implementation
- fake data
- hardcoded camera list yang tidak tervalidasi
- function kosong
- exception yang di-ignore
- `try/catch` yang menyembunyikan bug
- duplicate code
- overengineering
- unnecessary dependency
- dead code
- naming buruk
- architecture inconsistency.

Tidak boleh ada:

```kotlin
TODO()
```

atau:

```kotlin
throw NotImplementedError()
```

di production path.

---

# 24. PONYTAIL / DEVELOPMENT SKILL

Jika Ponytail atau development workflow skill tersedia:

- gunakan untuk menjaga struktur pekerjaan
- gunakan untuk review perubahan
- gunakan untuk meningkatkan kualitas implementation
- ikuti workflow skill tersebut sesuai dokumentasinya.

Jangan mengarang cara kerja skill.

Baca skill terlebih dahulu dan ikuti instruksi yang benar-benar tersedia di environment.

Hal yang sama berlaku untuk semua skill lain.

---

# 25. BUILD VALIDATION

Sebelum dianggap selesai:

```bash
./gradlew clean
./gradlew build
./gradlew test
./gradlew lint
```

Gunakan command yang sesuai dengan project aktual.

Tidak boleh ada:

- compile error
- lint error penting
- failing unit test
- broken resource
- unresolved dependency.

---

# 26. REAL DEVICE TEST

Pastikan Android device terdeteksi:

```bash
adb devices
```

Kemudian:

```bash
./gradlew installDebug
```

Launch aplikasi melalui ADB.

Tes:

1. startup
2. map
3. search
4. filter
5. buka camera
6. play stream
7. fullscreen
8. rotate device
9. background/foreground
10. kehilangan internet
11. internet kembali
12. favorite
13. restart app.

Ambil log:

```bash
adb logcat
```

Perbaiki semua crash yang ditemukan.

---

# 27. FINAL ACCEPTANCE TEST

Jangan menyatakan proyek selesai hanya karena APK berhasil dibuat.

Proyek baru selesai jika:

### Build

- [ ] Gradle build berhasil
- [ ] unit tests berhasil
- [ ] lint berhasil
- [ ] release/debug APK berhasil dibuat

### Android

- [ ] APK terinstall pada perangkat nyata
- [ ] aplikasi launch
- [ ] tidak crash
- [ ] navigation bekerja
- [ ] map bekerja
- [ ] search bekerja
- [ ] filter bekerja
- [ ] favorites bekerja

### CCTV

- [ ] catalog dapat dimuat
- [ ] metadata valid
- [ ] lokasi valid apabila tersedia
- [ ] status camera dapat diverifikasi
- [ ] player bekerja untuk format stream yang didukung
- [ ] offline stream ditangani dengan benar

### Quality

- [ ] tidak ada placeholder
- [ ] tidak ada fake implementation
- [ ] tidak ada secret yang di-hardcode
- [ ] tidak ada bypass authentication
- [ ] tidak ada sumber privat yang dimasukkan
- [ ] source attribution tersedia
- [ ] license/terms dicatat jika relevan

---

# 28. OUTPUT FINAL

Pada akhir proyek berikan laporan:

```text
PROJECT STATUS
==============

Application:
Package Name:
Version:
Android Minimum SDK:
Target SDK:

Build:
Debug APK:
Release APK:

CCTV DATA
=========

Total discovered:
Total validated:
Online:
Offline:
Auth required:
Invalid:
Unknown:

Geographic coverage:
Province:
Regency/City:
District:
Coordinates available:

STREAM TYPES
============

HLS:
DASH:
MJPEG:
RTSP:
Other:

ANDROID TEST
============

Device:
Android Version:
Install:
Launch:
Map:
Search:
Filtering:
Player:
Fullscreen:
Favorite:
Offline:
Crash:

KNOWN LIMITATIONS
=================

...

SOURCE & ATTRIBUTION
====================

...
```

---

# 29. DOKUMENTASI

Buat dokumentasi:

```text
README.md
ARCHITECTURE.md
DATA-SOURCES.md
DEVELOPMENT.md
TESTING.md
PRIVACY.md
LICENSES.md
```

README harus menjelaskan cara:

1. clone repository
2. setup environment
3. menjalankan discovery pipeline
4. menghasilkan database
5. menjalankan Android app
6. build APK
7. install ke Android.

---

# 30. ATURAN KERJA AGENT

Kamu harus bekerja seperti software engineering team, bukan sekadar generator kode.

Workflow:

```text
AUDIT
  ↓
RESEARCH
  ↓
ARCHITECTURE
  ↓
DATA MODEL
  ↓
DATA COLLECTION
  ↓
VALIDATION
  ↓
ANDROID FOUNDATION
  ↓
FEATURE IMPLEMENTATION
  ↓
TESTING
  ↓
DEBUGGING
  ↓
REAL DEVICE TEST
  ↓
FINAL QA
  ↓
DOCUMENTATION
```

Jangan melompati tahap penting.

Gunakan skill yang tersedia di laptop apabila relevan.

Setiap fase harus menghasilkan artefak yang dapat diverifikasi.

Jangan mengatakan "selesai" berdasarkan asumsi.

Verifikasi dengan command/tool nyata.

Jika menemukan masalah, lanjutkan melakukan diagnosis dan perbaikan sampai solusi yang masuk akal tercapai.

---

# 31. PRIORITAS

Urutan prioritas:

1. Legalitas dan validitas sumber CCTV
2. Data accuracy
3. Reliability
4. Android native architecture
5. Stability
6. Performance
7. UX
8. Scalability
9. Documentation

Lebih baik memiliki **500 kamera publik yang benar-benar valid** daripada 50.000 kamera yang URL-nya palsu, mati, privat, atau tidak jelas asal-usulnya.

---

# 32. START NOW

Mulai dari **PHASE 0 — ENVIRONMENT AUDIT**.

Jangan langsung membuat UI.

Pertama:

1. audit seluruh environment
2. temukan seluruh skill yang terinstall
3. baca skill yang relevan
4. tentukan tech stack
5. tentukan workflow
6. buat dokumen architecture
7. mulai discovery sumber CCTV publik Indonesia
8. bangun dataset terstruktur
9. validasi dataset
10. baru mulai implementasi Android.

Pada setiap fase, laporkan:

```text
PHASE:
STATUS:
WHAT WAS DONE:
FILES CREATED:
TESTS:
RESULT:
BLOCKERS:
NEXT STEP:
```

Tujuan akhir bukan sekadar menghasilkan source code.

Tujuan akhir adalah:

> **aplikasi Android native yang benar-benar bisa di-build, di-install ke perangkat Android nyata, dibuka tanpa crash, menampilkan katalog CCTV publik Indonesia yang tervalidasi, menampilkan lokasi kamera pada map, dan memutar stream yang memang tersedia secara publik/resmi.**