package id.nusantara.cctv

import id.nusantara.cctv.data.catalog.CatalogFileDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogDtoParsingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `parse katalog lengkap dengan field wajib`() {
        val raw = """
        {
          "catalog_version": 3,
          "generated_at": "2026-09-03T23:00:00+07:00",
          "sources": [{
            "source_id": "malangkota",
            "source_name": "Portal CCTV Kota Malang",
            "source_url": "https://cctv.malangkota.go.id/sebaran-cctv",
            "operator": "Pemkot Malang",
            "access_type": "OFFICIAL_PORTAL",
            "auth_needed_for_stream": true,
            "bootstrap_url": "https://cctv.malangkota.go.id/sebaran-cctv",
            "referer": "https://cctv.malangkota.go.id/sebaran-cctv",
            "license_note": "publik"
          }],
          "cameras": [{
            "id": "abc123",
            "camera_name": "Jl. Kawi Arah Timur",
            "province": "Jawa Timur",
            "city_regency": "Kota Malang",
            "district": "KLOJEN",
            "latitude": -7.98,
            "longitude": 112.63,
            "location_accuracy": "exact",
            "source_id": "malangkota",
            "stream_type": "HLS",
            "stream_url": "https://cctv.malangkota.go.id/cctv-stream/streams/x.m3u8",
            "status": "ONLINE"
          }]
        }
        """.trimIndent()

        val dto = json.decodeFromString(CatalogFileDto.serializer(), raw)

        assertEquals(3, dto.catalog_version)
        assertEquals(1, dto.sources.size)
        assertTrue(dto.sources[0].auth_needed_for_stream)
        assertEquals(1, dto.cameras.size)
        assertEquals("Jl. Kawi Arah Timur", dto.cameras[0].camera_name)
        assertEquals(-7.98, dto.cameras[0].latitude!!, 1e-9)
        assertEquals("ONLINE", dto.cameras[0].status)
    }

    @Test
    fun `field opsional hilang tetap parse`() {
        val raw = """
        {
          "catalog_version": 1,
          "generated_at": "2026-01-01T00:00:00+07:00",
          "cameras": [{
            "id": "x1",
            "camera_name": "Kamera minim metadata",
            "source_id": "s1",
            "stream_url": "https://contoh.go.id/live.m3u8"
          }]
        }
        """.trimIndent()

        val dto = json.decodeFromString(CatalogFileDto.serializer(), raw)

        assertEquals(1, dto.cameras.size)
        assertNull(dto.cameras[0].district)
        assertNull(dto.cameras[0].latitude)
        assertEquals("HLS", dto.cameras[0].stream_type) // default
        assertEquals("UNKNOWN", dto.cameras[0].status) // default
    }

    @Test
    fun `field tak dikenal diabaikan tanpa error`() {
        val raw = """
        {
          "catalog_version": 2,
          "generated_at": "2026-02-02T00:00:00+07:00",
          "field_asing_masa_depan": {"nested": true},
          "cameras": [{
            "id": "x2",
            "camera_name": "Kamera",
            "source_id": "s1",
            "stream_url": "https://contoh.go.id/live.m3u8",
            "label_baru_2027": "nilai"
          }]
        }
        """.trimIndent()

        val dto = json.decodeFromString(CatalogFileDto.serializer(), raw)
        assertEquals(2, dto.catalog_version)
        assertEquals(1, dto.cameras.size)
    }
}
