package id.nusantara.cctv

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.catalog.CatalogUrlError
import id.nusantara.cctv.data.catalog.CatalogUrlResult
import id.nusantara.cctv.data.db.CctvDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogEndpointPolicyTest {
    private lateinit var context: Context
    private lateinit var db: CctvDatabase
    private lateinit var repository: CatalogRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CctvDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CatalogRepository(context, db, initialRemoteUrl = null)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `blank override uses official catalog`() {
        assertEquals(CatalogUrlResult.Accepted, repository.setAlternateCatalogUrl(""))
        assertEquals(CatalogRepository.OFFICIAL_CATALOG_URL, repository.activeCatalogUrl())
    }

    @Test
    fun `non HTTPS override is rejected`() {
        assertEquals(
            CatalogUrlResult.Rejected(CatalogUrlError.NON_HTTPS),
            repository.setAlternateCatalogUrl("http://example.test/cameras.json"),
        )
        assertEquals(CatalogRepository.OFFICIAL_CATALOG_URL, repository.activeCatalogUrl())
    }

    @Test
    fun `reset restores official catalog`() {
        assertEquals(
            CatalogUrlResult.Accepted,
            repository.setAlternateCatalogUrl("https://example.test/cameras.json"),
        )
        repository.resetCatalogUrl()
        assertEquals(CatalogRepository.OFFICIAL_CATALOG_URL, repository.activeCatalogUrl())
    }
}
