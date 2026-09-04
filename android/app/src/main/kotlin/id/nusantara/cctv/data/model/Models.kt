package id.nusantara.cctv.data.model

data class Camera(
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

data class CameraSourceConfig(
    val sourceId: String,
    val sourceName: String,
    val sourceUrl: String,
    val operator: String,
    val authNeededForStream: Boolean,
    val bootstrapUrl: String?,
    val referer: String?,
    val licenseNote: String,
)

data class CatalogVersion(val version: Int, val generatedAt: String)
