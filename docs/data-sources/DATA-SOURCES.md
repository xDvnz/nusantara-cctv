# DATA-SOURCES — Sumber Data CCTV Publik

Prinsip: hanya sumber yang **ditayangkan resmi untuk publik** oleh pemda/lembaga.
Tidak ada kredensial privat, tidak ada bypass auth, tidak ada kamera privat.
Seluruh katalog diambil read-only dengan interval terbatas.

## Sumber DI KATALOG (validated)

| source_id | Portal | Operator | Provinsi | Akses | Kamera (v4) | Catatan teknis |
|---|---|---|---|---|---|---|
| malangkota | cctv.malangkota.go.id/sebaran-cctv | Diskominfo Kota Malang | Jawa Timur | OFFICIAL_PORTAL + sesi cookie publik | 253 (233 online v4) | API POST /api/v2/get-cameras (perlu cookie sesi dari bootstrap halaman + header XHR); HLS /cctv-stream/streams/{id}.m3u8 (cookie+referer). TLS: server tak kirim intermediate GeoTrust TLS RSA CA G1 — intermediate publik di-bundle di network security config. Koordinat exact. |
| jogjakota | cctv.jogjakota.go.id | Jogja Command Center, Pemkot Yogyakarta | DI Yogyakarta | PUBLIC_API | 147 online | GET /home/getdata (XHR) → JSON lengkap dgn koordinat, kecamatan, kelurahan; HLS di cctvjss.jogjakota.go.id (Wowza, 1280x720, tanpa sesi). Kamera status private/maintenance tidak diambil. |
| palembang | cctv.palembang.go.id | Diskominfo/Dishub Kota Palembang | Sumatera Selatan | PUBLIC_API | 30 online | GET /api/cctv → GeoJSON-ish dgn koordinat exact, kecamatan, kelurahan; HLS stream.palembang.go.id (fMP4 HLS). **Server agresif throttl** → pipeline proses serial 1 rps. Koordinat exact. |
| banjarmasin | atcs.banjarmasinkota.go.id | Dishub Kota Banjarmasin | Kalimantan Selatan | PUBLIC_API | 30 online | GET /api/public/cameras/wall → [{code,name,stream,poster}]; HLS /stream/{code}/index.m3u8 1280x720, **fMP4** (EXT-X-MAP + segment .mp4). Tanpa koordinat → administrative_only. |
| bandungkota | atcs-dishub.bandung.go.id | Dishub Kota Bandung | Jawa Barat | OFFICIAL_PORTAL | 21 online | Area id dari homepage; POST /ajax/cctv-list (XHR) → id kamera; POST /ajax/cctv-info → src HLS :1990/{Nama}/index.m3u8 (704x576). Tanpa koordinat → administrative_only. |

## Sumber teridentifikasi TIDAK masuk katalog (dengan alasan)

| Sumber | Alasan |
|---|---|
| smartcity.jakarta.go.id /api/v1/cctv | API butuh token; WAF menolak akses non-browser → AUTH_REQUIRED |
| atcs.denpasarkota.go.id | API butuh Authorization header → AUTH_REQUIRED |
| balisatudata.baliprov.go.id (Bali Satu Data) | Metadata terbuka (±297 kamera) tetapi HLS hanya lewat EMBED player; server media tidak resolvable publik → tidak dapat diputar native |
| dishub.bandungkab.go.id | HLS token per-load halaman (short-lived) — butuh scraping HTML tiap sesi; tidak stabil utk katalog statis |
| cctv.salatiga.go.id | streamer.salatiga.go.id tidak resolvable publik (internal-only) |
| cctv.medan.go.id | Cloudflare bot challenge → tidak terverifikasi |
| scc.surabaya.go.id, komando.bandung.go.id, pso.semarangkota.go.id, atcs.kemenhub.go.id, cctv.{makassar,tangerangkota,bekasikota,kotabogor,depok}.go.id | domain mati / tidak terjangkau saat audit |
| 24hour.id (agregator Malang) | sumber pihak ketiga, bukan operator resmi; katalog memakai portal resmi Pemkot Malang langsung |

## Kandidat riset lanjutan

- Portal pemda baru (cek periodik): cctv subdomain kota lain, ATCS Dishub kota.
- Bali: minta akses HLS resmi ke operator transcode.baliprov.go.id.
- Jakarta: minta akses API CCTV Smart City (login mitra).

## Lisensi / ketentuan

Tiap sumber dicatat `license` + `terms_of_use` di katalog. Ringkas: feed untuk
pantauan publik; tidak menyatakan izin redistribusi ulang. Aplikasi ini hanya
**menautkan & memutar langsung dari server resmi** (tanpa mirror/re-encode).
