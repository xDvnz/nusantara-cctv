package id.nusantara.cctv.data.catalog

import android.content.Context
import id.nusantara.cctv.data.db.CatalogMetaEntity
import id.nusantara.cctv.data.db.CctvDatabase
import id.nusantara.cctv.data.db.CameraEntity
import id.nusantara.cctv.data.db.CameraHistoryEntity
import id.nusantara.cctv.data.db.FavoriteEntity
import id.nusantara.cctv.data.db.SourceEntity
import id.nusantara.cctv.data.db.toModel
import id.nusantara.cctv.data.model.Camera
import id.nusantara.cctv.data.model.CatalogVersion
import id.nusantara.cctv.data.model.CameraSourceConfig
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import androidx.room.withTransaction

private const val META_VERSION = "catalog_version"
private const val META_GENERATED = "catalog_generated_at"
private const val META_SEEDED = "catalog_seeded_at"

/** Error sinkronisasi katalog yang bisa ditampilkan apa adanya di UI. */
class CatalogSyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

class CatalogRepository(
    private val context: Context,
    private val db: CctvDatabase,
    initialRemoteUrl: String?,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    private var remoteCatalogUrl: String? = initialRemoteUrl

    fun updateRemoteUrl(url: String?) {
        remoteCatalogUrl = url?.takeIf { it.isNotBlank() }
    }

    val cameras: Flow<List<Camera>> = db.cameraDao().observeAll().map { list -> list.map { it.toModel() } }

    val provinces: Flow<List<String>> = db.cameraDao().observeProvinces()

    val operators: Flow<List<String>> = db.cameraDao().observeOperators()

    suspend fun citiesOf(province: String): List<String> =
        db.cameraDao().observeCities(province).first()

    suspend fun districtsOf(province: String, city: String): List<String> =
        db.cameraDao().observeDistricts(province, city).first()

    fun version(): Flow<CatalogVersion?> = db.catalogMetaDao().observe(META_VERSION)
        .map { v -> v?.toIntOrNull()?.let { CatalogVersion(it, "") } }

    suspend fun catalogVersion(): Int = db.catalogMetaDao().get(META_VERSION)?.toIntOrNull() ?: 0

    suspend fun catalogGeneratedAt(): String = db.catalogMetaDao().get(META_GENERATED).orEmpty()

    private fun CameraDto.toEntity() = CameraEntity(
        id = id,
        cameraName = camera_name,
        cameraCode = camera_code,
        province = province,
        cityRegency = city_regency,
        district = district,
        subdistrict = subdistrict,
        locationName = location_name,
        latitude = latitude,
        longitude = longitude,
        locationAccuracy = location_accuracy,
        sourceId = source_id,
        sourceName = source_name,
        sourceUrl = source_url,
        operator = operator,
        accessType = access_type,
        streamType = stream_type,
        streamUrl = stream_url,
        publicIdentifier = public_identifier,
        status = status,
        lastChecked = last_checked,
        license = license,
        termsOfUse = terms_of_use,
        confidenceScore = confidence_score,
        notes = notes,
    )

    fun observeCamera(id: String): Flow<Camera?> = db.cameraDao().observeById(id).map { it?.toModel() }

    suspend fun camera(id: String): Camera? = db.cameraDao().byId(id)?.toModel()

    fun observeIsFavorite(id: String): Flow<Boolean> = db.favoriteDao().observeIsFavorite(id)

    val favorites: Flow<List<Camera>> = db.favoriteDao().observeAll()
        .map { favs ->
            val ids = favs.map { it.cameraId }
            if (ids.isEmpty()) emptyList() else db.cameraDao().byIds(ids).map { it.toModel() }
        }

    suspend fun toggleFavorite(id: String) {
        val dao = db.favoriteDao()
        val exists = dao.observeIsFavorite(id).first()
        if (exists) dao.remove(id) else dao.add(FavoriteEntity(id, System.currentTimeMillis()))
    }

    /** Catat kamera sebagai "baru ditonton" (riwayat beranda); dipanggil saat stream dibuka. */
    suspend fun recordView(cameraId: String) {
        db.cameraHistoryDao().upsert(CameraHistoryEntity(cameraId, System.currentTimeMillis()))
    }

    /** Riwayat terbaru, terbatas [limit], hanya kamera yang masih ada di katalog. */
    fun observeRecentHistory(limit: Int = 12): Flow<List<Camera>> =
        db.cameraHistoryDao().recent(limit).map { entries ->
            val ids = entries.map { it.cameraId }
            if (ids.isEmpty()) emptyList() else db.cameraDao().byIds(ids).map { it.toModel() }
                .sortedBy { cams -> entries.indexOfFirst { it.cameraId == cams.id } }
        }

    suspend fun sourceConfig(sourceId: String): CameraSourceConfig? {
        val entity = db.sourceDao().byId(sourceId) ?: return null
        return CameraSourceConfig(
            sourceId = entity.sourceId,
            sourceName = entity.sourceName,
            sourceUrl = entity.sourceUrl,
            operator = entity.operator,
            authNeededForStream = entity.authNeededForStream,
            bootstrapUrl = entity.bootstrapUrl,
            referer = entity.referer,
            licenseNote = entity.licenseNote,
        )
    }

    fun observeSources(): Flow<List<CameraSourceConfig>> = db.sourceDao().observeAll()
        .map { list -> list.map { it.toConfig() } }

    /** Baca sekali (non-Flow) — dipakai preload startup agar playback awal punya config. */
    suspend fun sourcesOnce(): List<CameraSourceConfig> =
        db.sourceDao().observeAll().first().map { it.toConfig() }

    private fun SourceEntity.toConfig() = CameraSourceConfig(
        sourceId = sourceId,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        operator = operator,
        authNeededForStream = authNeededForStream,
        bootstrapUrl = bootstrapUrl,
        referer = referer,
        licenseNote = licenseNote,
    )

    suspend fun updateStatus(id: String, status: String, checkedAt: String) {
        db.cameraDao().updateStatus(id, status, checkedAt)
    }

    /**
     * Seed pertama kali dari aset bundel. Idempoten: hanya jalan bila DB kosong.
     * Gagal seed (aset rusak) -> lempar; aplikasi menampilkan state error, bukan crash.
     */
    suspend fun seedFromAssetsIfNeeded() = withContext(Dispatchers.IO) {
        if (db.cameraDao().count() > 0) return@withContext
        val raw = context.assets.open("catalog/cameras.json").use { it.readBytes().decodeToString() }
        val dto = json.decodeFromString(CatalogFileDto.serializer(), raw)
        applyCatalog(dto)
        db.catalogMetaDao().put(CatalogMetaEntity(META_SEEDED, dto.generated_at))
    }

    /** Hapus riwayat kamera yang sudah tak ada di katalog (panggilan periodik aplikasi). */
    suspend fun pruneHistory() {
        db.cameraHistoryDao().pruneOrphaned()
    }

    /**
     * §8/PHASE 8: sinkron katalog remote bila [remoteCatalogUrl] dikonfigurasi.
     * Gagal jaringan/parse tidak merusak DB lokal — error dibungkus CatalogSyncException.
     */
    suspend fun syncFromRemote() = withContext(Dispatchers.IO) {
        val url = remoteCatalogUrl?.takeIf { it.isNotBlank() }
            ?: throw CatalogSyncException("URL katalog remote belum dikonfigurasi di Settings.")
        val current = db.catalogMetaDao().get(META_VERSION)?.toIntOrNull() ?: 0
        val request = okhttp3.Request.Builder().url(url).build()
        try {
            okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
                .newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw CatalogSyncException("Server menjawab HTTP ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    val dto = json.decodeFromString(CatalogFileDto.serializer(), body)
                    if (dto.catalog_version <= current) {
                        return@withContext SyncResult.UP_TO_DATE
                    }
                    applyCatalog(dto)
                    SyncResult.UPDATED
                }
        } catch (e: IOException) {
            throw CatalogSyncException("Jaringan gagal: ${e.message}", e)
        } catch (e: kotlinx.serialization.SerializationException) {
            throw CatalogSyncException("Katalog remote tidak valid.", e)
        }
    }

    enum class SyncResult { UPDATED, UP_TO_DATE }

    /** Transaksional: replace katalog penuh, favorite dipertahankan lewat id. */
    private suspend fun applyCatalog(dto: CatalogFileDto) {
        val entities = dto.cameras.map { it.toEntity() }
        val sources = dto.sources.map {
            SourceEntity(
                sourceId = it.source_id,
                sourceName = it.source_name,
                sourceUrl = it.source_url,
                operator = it.operator,
                authNeededForStream = it.auth_needed_for_stream,
                bootstrapUrl = it.bootstrap_url,
                referer = it.referer,
                licenseNote = it.license_note,
            )
        }
        db.withTransaction {
            db.cameraDao().upsertAll(entities)
            db.cameraDao().deleteNotIn(entities.map { it.id })
            db.sourceDao().upsertAll(sources)
            db.catalogMetaDao().put(CatalogMetaEntity(META_VERSION, dto.catalog_version.toString()))
            db.catalogMetaDao().put(CatalogMetaEntity(META_GENERATED, dto.generated_at))
        }
    }
}
