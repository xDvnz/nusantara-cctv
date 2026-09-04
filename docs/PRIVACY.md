# PRIVACY

Prinsip (§19 master prompt): tidak mengumpulkan data user yang tidak dibutuhkan.

## Data yang diproses aplikasi

| Data | Dipakai untuk | Disimpan? |
|---|---|---|
| Katalog kamera (publik) | daftar/peta/pencarian | Lokal, Room DB di perangkat |
| Kamera favorit | daftar favorit offline | Lokal (Room), tidak keluar perangkat |
| Cookie sesi portal pemda | memutar stream yang butuh sesi publik | In-memory saja, hilang saat proses mati |
| Lokasi GPS (opsional) | fitur "kamera terdekat" | Tidak disimpan; hanya dipakai sesaat saat user memberi izin |
| Log/crash/analytics | — | Tidak ada. Aplikasi tidak mengirim telemetri ke mana pun |

## Permintaan izin

- `INTERNET`, `ACCESS_NETWORK_STATE` — inti pemutar stream.
- `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` — hanya diminta saat user
  memakai fitur jarak/terdekat; ditolak = fitur hilang, aplikasi tetap jalan.
- Tidak ada izin penyimpanan, kamera, mikrofon, kontak.

## Jaringan

- Aplikasi hanya berkomunikasi dengan: server portal pemda terdaftar di katalog
  (via HTTPS), tile peta OpenStreetMap (osmdroid), dan URL katalog remote bila
  diisi user di Pengaturan.
- Tanpa cleartext HTTP (`usesCleartextTraffic="false"`).
- Tidak ada SDK pihak ketiga iklan/analitik.
