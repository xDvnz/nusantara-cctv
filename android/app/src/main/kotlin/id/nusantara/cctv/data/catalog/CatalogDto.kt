package id.nusantara.cctv.data.catalog

import kotlinx.serialization.Serializable

@Serializable
data class CatalogFileDto(
    val catalog_version: Int,
    val generated_at: String,
    val sources: List<SourceDto> = emptyList(),
    val cameras: List<CameraDto> = emptyList(),
)

@Serializable
data class SourceDto(
    val source_id: String,
    val source_name: String,
    val source_url: String,
    val operator: String,
    val access_type: String,
    val auth_needed_for_stream: Boolean = false,
    val bootstrap_url: String? = null,
    val referer: String? = null,
    val license_note: String = "",
)

@Serializable
data class CameraDto(
    val id: String,
    val camera_name: String,
    val camera_code: String = "",
    val province: String = "",
    val city_regency: String = "",
    val district: String? = null,
    val subdistrict: String? = null,
    val location_name: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_accuracy: String = "none",
    val source_id: String,
    val source_name: String = "",
    val source_url: String = "",
    val operator: String = "",
    val access_type: String = "",
    val stream_type: String = "HLS",
    val stream_url: String,
    val api_endpoint: String? = null,
    val public_identifier: String = "",
    val status: String = "UNKNOWN",
    val last_checked: String? = null,
    val timezone: String = "Asia/Jakarta",
    val license: String = "",
    val terms_of_use: String = "",
    val confidence_score: Double = 0.0,
    val notes: String = "",
)
