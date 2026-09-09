# TESTING

## Unit test (JVM + Robolectric)

```bash
cd android
./gradlew testDebugUnitTest
```

Cakupan:

| Test | Yang diuji |
|---|---|
| `CatalogDtoParsingTest` | Parsing katalog JSON: field wajib, field opsional hilang, field tak dikenal (forward-compat) |
| `CctvDatabaseTest` | Room in-memory: pencarian (nama/lokasi/wilayah/kode), filter provinsi/kota/status, paginasi, favorit lintas regenerasi katalog (deleteNotIn tidak menghapus favorit), seed aset bundel (481 kamera / 466 online) |
| `MjpegDecoderTest` | Parser multipart MJPEG: 2 frame berurutan, stream tanpa SOI, stream kosong, frame korup tanpa EOI (null, bukan crash) |

## Validasi stream (pipeline Python, §9)

```bash
python tools/validation/validate.py
```

Aturan: HTTP 200 saja tidak cukup. ONLINE hanya bila manifest `#EXTM3U` + satu
segment termanifest punya magic byte video (TS 0x47 atau box fMP4: ftyp/moof/styp/moov/mdat).

## Device test (emulator / perangkat)

```bash
adb devices
./gradlew installDebug
adb shell am start -n id.nusantara.cctv/.MainActivity
adb logcat --pid=$(adb shell pidof id.nusantara.cctv)
```

Checklist manual yang dijalankan saat QA (hasil di laporan akhir):
startup, peta, pencarian, filter, buka kamera, play stream, fullscreen,
rotasi, background/foreground, putus internet, internet kembali, favorit, restart.

## Lint

```bash
cd android
./gradlew lint
```

Laporan: `android/app/build/reports/lint-results-debug.html`.
