package id.nusantara.cctv.data.api

import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.CookieStore
import java.util.concurrent.TimeUnit
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Klien HTTP bersama untuk semua akses sumber (bootstrap sesi, validasi status, remote catalog).
 *
 * Beberapa portal pemda (mis. cctv.malangkota.go.id) mensyaratkan cookie sesi publik yang
 * diperoleh dari bootstrap GET halaman portal (bukan login). CookieManager in-memory bersama
 * dipakai OkHttp DAN StreamEngine (header Cookie untuk Media3), sehingga satu kali bootstrap
 * dipakai semua request berikutnya.
 *
 * Semua fungsi blocking; panggil dari Dispatchers.IO.
 */
class SourceHttp {
    val cookieStore: CookieStore = CookieManager().cookieStore

    private val cookieHandler = CookieManager(cookieStore, CookiePolicy.ACCEPT_ORIGINAL_SERVER)

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(CookieJarBridge(cookieHandler))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Cookie untuk domain [url] dalam bentuk header "k=v; k2=v2", atau null bila kosong. */
    fun cookieHeader(url: String): String? {
        val httpUrl = url.toHttpUrlOrNull() ?: return null
        val cookies = cookieStore.get(httpUrl.toUri())
        if (cookies.isNullOrEmpty()) return null
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }

    /** Bootstrap sesi publik: GET [bootstrapUrl] bila belum ada cookie untuk host-nya. */
    fun ensureSession(bootstrapUrl: String?, referer: String?) {
        if (bootstrapUrl.isNullOrBlank()) return
        val host = bootstrapUrl.toHttpUrlOrNull()?.host ?: return
        val hasCookie = cookieStore.getCookies().any { host.endsWith(it.domain.removePrefix(".")) }
        if (hasCookie) return
        val request = Request.Builder()
            .url(bootstrapUrl)
            .apply { referer?.let { header("Referer", it) } }
            .build()
        client.newCall(request).execute().use { /* sesi tersimpan di cookieStore */ }
    }
}

/** Jembatan CookieHandler java.net <-> CookieJar okhttp; cookie terlihat oleh Media3 juga. */
private class CookieJarBridge(
    private val handler: CookieHandler,
) : CookieJar {
    override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
        val headers = HashMap<String, MutableList<String>>()
        cookies.forEach { cookie ->
            headers.getOrPut("Set-Cookie") { mutableListOf() }.add(cookie.toString())
        }
        runCatching { handler.put(url.toUri(), headers) }
    }

    override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
        val headers = runCatching {
            handler.get(url.toUri(), emptyMap<String, List<String>>())
        }.getOrNull().orEmpty()
        val result = mutableListOf<okhttp3.Cookie>()
        headers.forEach { (_, values) ->
            values.forEach { value ->
                okhttp3.Cookie.parse(url, value)?.let { result.add(it) }
            }
        }
        return result
    }
}
