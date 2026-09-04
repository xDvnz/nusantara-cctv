package id.nusantara.cctv.data.update

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LatestRelease(val tag_name: String = "", val html_url: String = "")

data class UpdateInfo(val version: String, val url: String)

/** Bandingkan versi semver sederhana: "1.10" > "1.9", "2.0" > "1.9". */
object VersionCompare {
    fun isRemoteNewer(remote: String, local: String): Boolean {
        val r = parse(remote)
        val l = parse(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parse(version: String): List<Int> =
        version.removePrefix("v").substringBefore('-').split('.')
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
}

/**
 * Cek rilis terbaru dari GitHub Releases (endpoint publik, tanpa auth).
 * Gagal jaringan/parse = null, tanpa exception ke UI.
 */
class UpdateChecker(
    private val repoOwner: String = "xDvnz",
    private val repoName: String = "nusantara-cctv",
) {

    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = okhttp3.Request.Builder()
                .url("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
                .newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val body = response.body?.string().orEmpty()
                    val release = Json { ignoreUnknownKeys = true }
                        .decodeFromString(LatestRelease.serializer(), body)
                    if (VersionCompare.isRemoteNewer(release.tag_name, currentVersion)) {
                        UpdateInfo(
                            version = release.tag_name.removePrefix("v"),
                            url = release.html_url,
                        )
                    } else {
                        null
                    }
                }
        }.getOrNull()
    }
}
