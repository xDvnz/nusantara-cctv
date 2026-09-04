package id.nusantara.cctv.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Database
import id.nusantara.cctv.data.model.Camera
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cameras", primaryKeys = ["id"])
data class CameraEntity(
    val id: String,
    val cameraName: String,
    val cameraCode: String,
    val province: String,
    val cityRegency: String,
    val district: String?,
    val subdistrict: String?,
    val locationName: String,
    val latitude: Double?,
    val longitude: Double?,
    val locationAccuracy: String,
    val sourceId: String,
    val sourceName: String,
    val sourceUrl: String,
    val operator: String,
    val accessType: String,
    val streamType: String,
    val streamUrl: String,
    val publicIdentifier: String,
    val status: String,
    val lastChecked: String?,
    val license: String,
    val termsOfUse: String,
    val confidenceScore: Double,
    val notes: String,
)

@Entity(tableName = "favorites", primaryKeys = ["cameraId"])
data class FavoriteEntity(
    val cameraId: String,
    val addedAt: Long,
)

@Entity(tableName = "catalog_meta", primaryKeys = ["key"])
data class CatalogMetaEntity(
    val key: String,
    val value: String,
)

@Entity(tableName = "sources", primaryKeys = ["sourceId"])
data class SourceEntity(
    val sourceId: String,
    val sourceName: String,
    val sourceUrl: String,
    val operator: String,
    val authNeededForStream: Boolean,
    val bootstrapUrl: String?,
    val referer: String?,
    val licenseNote: String,
)

@Dao
interface CameraDao {
    @Query("SELECT * FROM cameras ORDER BY province, cityRegency, cameraName")
    fun observeAll(): Flow<List<CameraEntity>>

    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun byId(id: String): CameraEntity?

    @Query("SELECT * FROM cameras WHERE id = :id")
    fun observeById(id: String): Flow<CameraEntity?>

    @Query("SELECT * FROM cameras WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<CameraEntity>

    @Query(
        """SELECT * FROM cameras WHERE
        (:province IS NULL OR province = :province) AND
        (:city IS NULL OR cityRegency = :city) AND
        (:district IS NULL OR district = :district) AND
        (:status IS NULL OR status = :status) AND
        (:streamType IS NULL OR streamType = :streamType) AND
        (:operator IS NULL OR operator = :operator) AND
        (:query = '' OR cameraName LIKE '%' || :query || '%'
            OR locationName LIKE '%' || :query || '%'
            OR province LIKE '%' || :query || '%'
            OR cityRegency LIKE '%' || :query || '%'
            OR district LIKE '%' || :query || '%'
            OR cameraCode LIKE '%' || :query || '%')
        ORDER BY province, cityRegency, cameraName
        LIMIT :limit OFFSET :offset"""
    )
    fun search(
        query: String,
        province: String?,
        city: String?,
        district: String?,
        status: String?,
        streamType: String?,
        operator: String?,
        limit: Int,
        offset: Int,
    ): List<CameraEntity>

    @Query("SELECT * FROM cameras WHERE status = :status")
    suspend fun byStatus(status: String): List<CameraEntity>

    @Query("SELECT DISTINCT province FROM cameras ORDER BY province")
    fun observeProvinces(): Flow<List<String>>

    @Query("SELECT DISTINCT cityRegency FROM cameras WHERE province = :province ORDER BY cityRegency")
    fun observeCities(province: String): Flow<List<String>>

    @Query("SELECT DISTINCT district FROM cameras WHERE province = :province AND cityRegency = :city AND district IS NOT NULL ORDER BY district")
    fun observeDistricts(province: String, city: String): Flow<List<String>>

    @Query("SELECT DISTINCT operator FROM cameras WHERE operator IS NOT NULL ORDER BY operator")
    fun observeOperators(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM cameras")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM cameras WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query("SELECT * FROM cameras WHERE latitude IS NOT NULL AND longitude IS NOT NULL")
    suspend fun withCoordinates(): List<CameraEntity>

    @Query("UPDATE cameras SET status = :status, lastChecked = :checkedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, checkedAt: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cameras: List<CameraEntity>)

    @Query("DELETE FROM cameras WHERE id NOT IN (:keepIds)")
    suspend fun deleteNotIn(keepIds: List<String>)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE cameraId = :cameraId)")
    fun observeIsFavorite(cameraId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE cameraId = :cameraId")
    suspend fun remove(cameraId: String)
}

@Dao
interface CatalogMetaDao {
    @Query("SELECT value FROM catalog_meta WHERE key = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM catalog_meta WHERE key = :key")
    fun observe(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(meta: CatalogMetaEntity)
}

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY sourceId")
    fun observeAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE sourceId = :sourceId")
    suspend fun byId(sourceId: String): SourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sources: List<SourceEntity>)
}

@Database(
    entities = [CameraEntity::class, FavoriteEntity::class, CatalogMetaEntity::class, SourceEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CctvDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun catalogMetaDao(): CatalogMetaDao
    abstract fun sourceDao(): SourceDao
}

fun CameraEntity.toModel() = Camera(
    id = id,
    cameraName = cameraName,
    cameraCode = cameraCode,
    province = province,
    cityRegency = cityRegency,
    district = district,
    subdistrict = subdistrict,
    locationName = locationName,
    latitude = latitude,
    longitude = longitude,
    locationAccuracy = locationAccuracy,
    sourceId = sourceId,
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    operator = operator,
    accessType = accessType,
    streamType = streamType,
    streamUrl = streamUrl,
    publicIdentifier = publicIdentifier,
    status = status,
    lastChecked = lastChecked,
    license = license,
    termsOfUse = termsOfUse,
    confidenceScore = confidenceScore,
    notes = notes,
)
