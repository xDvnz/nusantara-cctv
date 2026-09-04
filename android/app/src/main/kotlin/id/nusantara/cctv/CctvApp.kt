package id.nusantara.cctv

import android.app.Application
import androidx.room.Room
import id.nusantara.cctv.data.api.SourceHttp
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.db.CctvDatabase
import id.nusantara.cctv.data.model.CameraSourceConfig
import id.nusantara.cctv.data.player.StreamEngine
import id.nusantara.cctv.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * DI manual (AppContainer): graf kecil, tanpa generator kode.
 * URL katalog remote bisa disuntik lewat resource overlay BuildConfig-style
 * (resValue R.string.remote_catalog_url) — kosong = sync dimatikan.
 */
class CctvApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch {
            runCatching { container.catalogRepository.seedFromAssetsIfNeeded() }
                .onFailure { android.util.Log.e("CctvApp", "seed gagal", it) }
        }
        container.appScope.launch {
            runCatching { container.catalogRepository.pruneHistory() }
        }
    }
}

class AppContainer(private val app: Application) {

    val appContext: android.content.Context = app.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: CctvDatabase = Room.databaseBuilder(
        app,
        CctvDatabase::class.java,
        "nusantara_cctv.db",
    )
        .addMigrations(CctvDatabase.MIGRATION_1_2)
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()

    val sourceHttp = SourceHttp()

    val networkMonitor = NetworkMonitor(app).also { it.start() }

    val preferencesRepository = id.nusantara.cctv.data.prefs.AppPreferencesRepository(app)

    val updateChecker = id.nusantara.cctv.data.update.UpdateChecker()

    private val remoteCatalogUrl: String = BuildConfig.REMOTE_CATALOG_URL

    val catalogRepository = CatalogRepository(app, database, remoteCatalogUrl)

    // Config sumber preloaded: dipakai StreamEngine sinkron saat playback, tanpa runBlocking.
    private val sourceConfigs = java.util.concurrent.ConcurrentHashMap<String, CameraSourceConfig>()

    val streamEngine = StreamEngine(
        http = sourceHttp,
        sourceConfigOf = { sourceId -> sourceConfigs[sourceId] },
    )

    init {
        appScope.launch {
            catalogRepository.observeSources().collect { list ->
                list.forEach { sourceConfigs[it.sourceId] = it }
            }
        }
    }
}
