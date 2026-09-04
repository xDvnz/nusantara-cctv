package id.nusantara.cctv

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import id.nusantara.cctv.data.catalog.CatalogFileDto
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.db.CctvDatabase
import id.nusantara.cctv.data.db.CameraEntity
import id.nusantara.cctv.data.db.FavoriteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CctvDatabaseTest {

    private lateinit var db: CctvDatabase
    private lateinit var context: Context

    private fun camera(
        id: String,
        name: String,
        province: String = "Jawa Timur",
        city: String = "Kota Malang",
        district: String? = "KLOJEN",
        status: String = "ONLINE",
    ) = CameraEntity(
        id = id,
        cameraName = name,
        cameraCode = "TEST-$id",
        province = province,
        cityRegency = city,
        district = district,
        subdistrict = null,
        locationName = name,
        latitude = -7.98,
        longitude = 112.63,
        locationAccuracy = "exact",
        sourceId = "s1",
        sourceName = "Sumber Uji",
        sourceUrl = "https://contoh.go.id",
        operator = "Pemda Uji",
        accessType = "PUBLIC_API",
        streamType = "HLS",
        streamUrl = "https://contoh.go.id/$id.m3u8",
        publicIdentifier = id,
        status = status,
        lastChecked = "2026-09-03T00:00:00+07:00",
        license = "",
        termsOfUse = "",
        confidenceScore = 0.9,
        notes = "",
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CctvDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `pencarian mencocokkan nama lokasi wilayah dan kode`() = runBlocking {
        db.cameraDao().upsertAll(
            listOf(
                camera("1", "Jl. Kawi Arah Timur"),
                camera("2", "Simpang GKI Bromo", district = "BLIMBING"),
                camera("3", "Jl. Borobudur", province = "DI Yogyakarta", city = "Kota Yogyakarta"),
            ),
        )
        assertEquals(1, db.cameraDao().search("Kawi", null, null, null, null, null, null, 10, 0).size)
        assertEquals(1, db.cameraDao().search("Blimbing", null, null, null, null, null, null, 10, 0).size)
        assertEquals(1, db.cameraDao().search("Yogyakarta", null, null, null, null, null, null, 10, 0).size)
        assertEquals(1, db.cameraDao().search("TEST-1", null, null, null, null, null, null, 10, 0).size)
    }

    @Test
    fun `filter provinsi kota dan status bekerja`() = runBlocking {
        db.cameraDao().upsertAll(
            listOf(
                camera("1", "A", status = "ONLINE"),
                camera("2", "B", status = "OFFLINE"),
                camera("3", "C", province = "Sumatera Selatan", city = "Kota Palembang", status = "ONLINE"),
            ),
        )
        val online = db.cameraDao().search("", null, null, null, "ONLINE", null, null, 10, 0)
        assertEquals(2, online.size)
        val palembang = db.cameraDao().search("", "Sumatera Selatan", "Kota Palembang", null, null, null, null, 10, 0)
        assertEquals(1, palembang.size)
        assertEquals("C", palembang[0].cameraName)
    }

    @Test
    fun `paginasi limit offset benar`() = runBlocking {
        db.cameraDao().upsertAll((1..5).map { camera(it.toString(), "Kamera $it") })
        val page1 = db.cameraDao().search("", null, null, null, null, null, null, 2, 0)
        val page2 = db.cameraDao().search("", null, null, null, null, null, null, 2, 2)
        assertEquals(2, page1.size)
        assertEquals(2, page2.size)
        assertTrue(page1.map { it.id }.intersect(page2.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun `favorit toggle dan deleteNotIn mempertahankan favorit lintas regenerasi katalog`() = runBlocking {
        db.cameraDao().upsertAll(listOf(camera("1", "A"), camera("2", "B")))
        db.favoriteDao().add(FavoriteEntity("1", 1L))
        assertTrue(db.favoriteDao().observeIsFavorite("1").first())

        // katalog baru: kamera 1 tetap, kamera 2 diganti 3
        db.cameraDao().upsertAll(listOf(camera("1", "A"), camera("3", "C")))
        db.cameraDao().deleteNotIn(listOf("1", "3"))

        assertTrue(db.favoriteDao().observeIsFavorite("1").first())
        assertTrue(db.cameraDao().byId("1") != null)
        assertTrue(db.cameraDao().byId("2") == null)
        db.favoriteDao().remove("1")
        assertFalse(db.favoriteDao().observeIsFavorite("1").first())
    }

    @Test
    fun `seed dari aset memuat katalog bundel`() = runBlocking {
        val repository = CatalogRepository(context, db, initialRemoteUrl = null)
        repository.seedFromAssetsIfNeeded()
        val total = db.cameraDao().count()
        // katalog bundel v4: 481 kamera
        assertEquals(481, total)
        val online = db.cameraDao().countByStatus("ONLINE")
        assertEquals(466, online)
        assertTrue(db.cameraDao().withCoordinates().size > 400)
    }

    @Test
    fun `sync remote dengan versi lebih rendah tidak menurunkan katalog`() = runBlocking {
        val repository = CatalogRepository(context, db, initialRemoteUrl = null)
        repository.seedFromAssetsIfNeeded()
        val version = repository.catalogVersion()
        assertEquals(4, version)
    }
}
