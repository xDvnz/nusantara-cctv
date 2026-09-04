package id.nusantara.cctv.data.player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import id.nusantara.cctv.data.api.SourceHttp
import id.nusantara.cctv.data.model.Camera
import id.nusantara.cctv.data.model.CameraSourceConfig
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request

/** Hasil resolve kamera menjadi sesuatu yang bisa diputar/ditampilkan. */
sealed interface Playable {
    /** HLS/DASH — untuk ExoPlayer. */
    data class Exo(val factory: (MediaItem) -> MediaSource, val mimeType: String) : Playable

    /** MJPEG — bitmap frames. */
    data class Mjpeg(val frames: Flow<Bitmap>) : Playable

    /** Format dikenal tapi tidak bisa diputar tanpa infrastruktur tambahan (mis. RTSP tanpa relay). */
    data class Unsupported(val reason: String) : Playable
}

/**
 * Adapter per format stream (§10). Kamera tanpa adapter yang bisa jalan -> Unsupported
 * dengan alasan jelas, bukan crash.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class StreamEngine(
    private val http: SourceHttp,
    private val sourceConfigOf: (String) -> CameraSourceConfig?,
) {

    fun resolve(camera: Camera): Playable = when (camera.streamType.uppercase()) {
        "HLS" -> Playable.Exo(
            factory = { mediaItem -> hlsSourceFor(camera, mediaItem) },
            mimeType = MimeTypes.APPLICATION_M3U8,
        )
        "DASH" -> Playable.Exo(
            factory = { mediaItem -> dashSourceFor(camera, mediaItem) },
            mimeType = MimeTypes.APPLICATION_MPD,
        )
        "MJPEG" -> Playable.Mjpeg(mjpegFrames(camera))
        "RTSP" -> Playable.Unsupported(
            "RTSP butuh relay/transcoder resmi; kamera ini tidak menyediakan endpoint publik selain RTSP."
        )
        else -> Playable.Unsupported("Format stream tidak didukung: ${camera.streamType}")
    }

    /**
     * MediaSource HLS dengan header sesi (cookie dari bootstrap + referer whitelist per sumber).
     * Bootstrap dijalankan dulu bila sumber mensyaratkannya.
     */
    private fun headersFor(camera: Camera): Map<String, String> {
        val config = sourceConfigOf(camera.sourceId)
        if (config?.bootstrapUrl != null) {
            runCatching { http.ensureSession(config.bootstrapUrl, config.referer) }
        }
        val headers = mutableMapOf<String, String>()
        config?.referer?.let { headers["Referer"] = it }
        http.cookieHeader(camera.streamUrl)?.let { headers["Cookie"] = it }
        headers["User-Agent"] = USER_AGENT
        return headers
    }

    private fun dataSourceFactory(camera: Camera): DataSource.Factory {
        val headers = headersFor(camera)
        return DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
    }

    fun hlsSourceFor(camera: Camera, mediaItem: MediaItem): MediaSource =
        HlsMediaSource.Factory(dataSourceFactory(camera))
            .createMediaSource(mediaItem)

    fun dashSourceFor(camera: Camera, mediaItem: MediaItem): MediaSource {
        val factory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory(camera))
        return factory.createMediaSource(mediaItem)
    }

    /**
     * MJPEG: parse multipart/x-mixed-replace dari [camera.streamUrl], emit bitmap (maks ~10 fps).
     */
    private fun mjpegFrames(camera: Camera): Flow<Bitmap> {
        val config = sourceConfigOf(camera.sourceId)
        if (config?.bootstrapUrl != null) {
            runCatching { http.ensureSession(config.bootstrapUrl, config.referer) }
        }
        val request = Request.Builder()
            .url(camera.streamUrl)
            .apply {
                config?.referer?.let { header("Referer", it) }
                http.cookieHeader(camera.streamUrl)?.let { header("Cookie", it) }
                header("User-Agent", USER_AGENT)
            }
            .build()
        return flow {
            http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@flow
                }
                val body = response.body ?: return@flow
                val frames = MjpegDecoder(body.byteStream())
                while (true) {
                    val frame = frames.nextFrame() ?: break
                    emit(frame)
                    kotlinx.coroutines.delay(100)
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Validasi status ringan (HEAD/GET manifest) untuk tombol "Periksa status" —
     * bukan polling massal (§14). Mengembalikan status string sesuai skema.
     */
    fun probeStatus(camera: Camera): String {
        return try {
            val config = sourceConfigOf(camera.sourceId)
            if (config?.bootstrapUrl != null) {
                http.ensureSession(config.bootstrapUrl, config.referer)
            }
            val request = Request.Builder()
                .url(camera.streamUrl)
                .apply {
                    config?.referer?.let { header("Referer", it) }
                    http.cookieHeader(camera.streamUrl)?.let { header("Cookie", it) }
                    header("User-Agent", USER_AGENT)
                }
                .build()
            http.client.newCall(request).execute().use { response ->
                when {
                    response.code in 401..403 -> "AUTH_REQUIRED"
                    response.code == 404 -> "OFFLINE"
                    !response.isSuccessful -> "OFFLINE"
                    else -> {
                        val body = response.body?.string().orEmpty()
                        if (body.lstrip().startsWith("#EXTM3U")) "ONLINE" else "INVALID_STREAM"
                    }
                }
            }
        } catch (e: java.io.IOException) {
            if (e.message?.contains("timeout", ignoreCase = true) == true) "TIMEOUT" else "OFFLINE"
        } catch (e: Exception) {
            "OFFLINE"
        }
    }

    private fun String.lstrip() = trimStart()

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36"
    }
}

/**
 * Decoder MJPEG multipart: baca frame JPEG berurutan dari stream HTTP.
 * JPEG dipisah EOI (FF D9), bukan akhir buffer — boundary multipart boleh
 * mengikuti langsung setelah frame. Sisa byte dipertahankan untuk frame berikutnya.
 */
class MjpegDecoder(private val stream: InputStream) {
    private val buffered = BufferedInputStream(stream, 64 * 1024)
    private val running = AtomicBoolean(true)
    private val pending = java.io.ByteArrayOutputStream()

    /** Blokir sampai satu frame JPEG utuh terbaca; null bila stream selesai/gagal decode. */
    fun nextFrame(): Bitmap? {
        if (!running.get()) return null
        val buffer = java.io.ByteArrayOutputStream()
        buffer.write(pending.toByteArray())
        pending.reset()

        if (!seekToSoi(buffer)) return null
        var scanned = 2 // buffer mulai dari SOI; EOI dicari dari situ
        val chunk = ByteArray(8192)
        while (running.get()) {
            val data = buffer.toByteArray()
            val eoi = findEoi(data, maxOf(0, scanned - 1))
            if (eoi >= 0) {
                val leftover = data.size - (eoi + 2)
                if (leftover > 0) pending.write(data, eoi + 2, leftover)
                return BitmapFactory.decodeByteArray(data, 0, eoi + 2)
            }
            scanned = data.size
            val n = buffered.read(chunk)
            if (n <= 0) return null
            buffer.write(chunk, 0, n)
            if (buffer.size() > MAX_FRAME_BYTES) return null
        }
        return null
    }

    fun close() {
        running.set(false)
        runCatching { stream.close() }
    }

    /** Buang byte sampai marker SOI (FF D8); buffer berisi frame mulai SOI. */
    private fun seekToSoi(buffer: java.io.ByteArrayOutputStream): Boolean {
        val data = buffer.toByteArray()
        for (i in 0..data.size - 2) {
            if (data[i] == 0xFF.toByte() && data[i + 1] == 0xD8.toByte()) {
                buffer.reset()
                buffer.write(data, i, data.size - i)
                return true
            }
        }
        var prev = if (data.isEmpty()) -1 else data[data.size - 1].toInt() and 0xFF
        while (true) {
            val b = buffered.read()
            if (b == -1) return false
            if (prev == 0xFF && b == 0xD8) {
                buffer.reset()
                buffer.write(SOI)
                return true
            }
            prev = b
        }
    }

    private fun findEoi(data: ByteArray, from: Int): Int {
        var i = maxOf(0, from)
        while (i <= data.size - 2) {
            if (data[i] == 0xFF.toByte() && data[i + 1] == 0xD9.toByte()) return i
            i++
        }
        return -1
    }

    companion object {
        private const val MAX_FRAME_BYTES = 12 * 1024 * 1024
        private val SOI = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    }
}
