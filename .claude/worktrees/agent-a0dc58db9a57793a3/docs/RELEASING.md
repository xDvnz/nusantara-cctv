# RELEASING — Pola Rilis APK

Repo: https://github.com/xDvnz/nusantara-cctv
Semua rilis dipublikasikan sebagai **GitHub Release** di tag `vX.Y` dengan APK terlampir.

## Aturan versi

| Jenis perubahan | Versi | Contoh |
|---|---|---|
| Rilis perdana | 1.0 | v1.0 — selesai 2026-09-04 |
| **Update kecil** (fix bug, kamera baru via katalog, perbaikan UI kecil) | **1.1, 1.2, …** | fix crash search → v1.1 |
| **Major update** (fitur baru besar, perubahan skema DB/format katalog, rombak UI/arsitektur) | **2.0, 3.0, …** | tambah MJPEG viewer + GPS terdekat → v2.0 |

- `versionName` di `android/app/build.gradle.kts` mengikuti tag: `1.0` → `1.1` → `2.0`.
- `versionCode` selalu **naik +1** setiap rilis (berapapun jenisnya) — dipakai Android
  untuk urutan upgrade.

## Checklist rilis (jalankan berurutan)

```bash
# 1. Naikkan versi di android/app/build.gradle.kts
#    versionName "1.1", versionCode += 1

# 2. Build + uji
cd android
./gradlew assembleDebug testDebugUnitTest lint
./gradlew assembleRelease

# 3. Uji install release di emulator/perangkat
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n id.nusantara.cctv/.MainActivity
#    buka home, map, detail kamera → tidak crash, 0 FATAL di logcat

# 4. Commit + tag + push
cd ..
git add -A
git commit -m "chore(release): v1.1 — <ringkasan perubahan>"
git tag v1.1
git push origin main --tags

# 5. GitHub Release + upload APK (nama file wajib memuat versi)
cp android/app/build/outputs/apk/release/app-release.apk dist/NusantaraCCTV-v1.1-release.apk
gh release create v1.1 dist/NusantaraCCTV-v1.1-release.apk \
  --title "v1.1 — <judul>" \
  --notes "<ringkasan perubahan + catatan install>"
```

## Aturan penamaan file

`NusantaraCCTV-v<versi>-release.apk` (+ opsional `-debug`).
Versi di nama file = versi tag = `versionName`. Jangan pernah menimpa file rilis lama.

## Catatan

- APK release ditandatangani **kunci debug** (installable sideload). Kunci debug
  tidak di-commit; bila nanti pindah ke Play Store, buat keystore produksi terpisah.
- Upgrade di perangkat: `versionCode` yang menentukan; signature harus sama
  (kunci debug sama → upgrade mulus tanpa uninstall).
