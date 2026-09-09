package id.nusantara.cctv.data.player

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import id.nusantara.cctv.data.model.Camera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Jenis error pemutar; teks tampil di-resolve UI via resource string. */
enum class PlayerError { NETWORK, UNAVAILABLE, PLAYBACK, SOURCE, MJPEG_INACTIVE, ENDED, UNSUPPORTED }

/** UI state pemutar; dipakai layar detail (inline) dan layar fullscreen. */
sealed interface PlayerUi {
    data object Idle : PlayerUi
    data object Loading : PlayerUi
    data object Playing : PlayerUi

    /** @param messageRes khusus UNSUPPORTED: resource string pesan yang tepat. */
    data class Error(
        val kind: PlayerError,
        val detail: String? = null,
        val messageRes: Int? = null,
        val messageArg: String? = null,
    ) : PlayerUi

    data class MjpegFrame(val bitmap: Bitmap) : PlayerUi
}

/**
 * Pemilik ExoPlayer/MJPEG subscribe untuk satu kamera.
 * Satu instance per ViewModel. Panggil [release] di onCleared/onDispose.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class StreamPlayerController(
    private val context: Context,
    private val engine: StreamEngine,
    private val scope: CoroutineScope,
) {

    private val _ui = MutableStateFlow<PlayerUi>(PlayerUi.Idle)
    val ui: StateFlow<PlayerUi> = _ui

    var exoPlayer: ExoPlayer? = null
        private set

    private var startedForId: String? = null
    private var mjpegJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> _ui.value = PlayerUi.Loading
                Player.STATE_READY -> _ui.value = PlayerUi.Playing
                Player.STATE_ENDED -> _ui.value = PlayerUi.Error(PlayerError.ENDED)
                else -> {}
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _ui.value = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                -> PlayerUi.Error(PlayerError.NETWORK)
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                -> PlayerUi.Error(PlayerError.UNAVAILABLE)
                else -> PlayerUi.Error(PlayerError.PLAYBACK, detail = error.errorCodeName)
            }
        }
    }

    /** Mulai memutar [camera]; no-op bila sudah memutar kamera yang sama. */
    fun start(camera: Camera) {
        if (startedForId == camera.id && (exoPlayer != null || mjpegJob != null)) return
        startedForId = camera.id
        release()
        _ui.value = PlayerUi.Loading
        // resolve + bootstrap sesi portal berjalan di IO (network blocking);
        // ExoPlayer dibuat di thread utama setelahnya.
        scope.launch {
            val playable = withContext(Dispatchers.IO) { engine.resolve(camera) }
            if (startedForId != camera.id) return@launch // dibatalkan release() saat resolve berjalan
            when (playable) {
                is Playable.Exo -> {
                    val player = ExoPlayer.Builder(context).build()
                    exoPlayer = player
                    player.addListener(listener)
                    val mediaItem = MediaItem.fromUri(camera.streamUrl)
                    val source = runCatching { playable.factory(mediaItem) }.getOrElse { e ->
                        _ui.value = PlayerUi.Error(PlayerError.SOURCE, detail = e.message)
                        return@launch
                    }
                    player.setMediaSource(source)
                    player.prepare()
                    player.playWhenReady = true
                }
                is Playable.Mjpeg -> {
                    mjpegJob = scope.launch {
                        var emitted = false
                        try {
                            playable.frames.collect { bitmap ->
                                emitted = true
                                _ui.value = PlayerUi.MjpegFrame(bitmap)
                            }
                        } finally {
                            if (!emitted) _ui.value = PlayerUi.Error(PlayerError.MJPEG_INACTIVE)
                        }
                    }
                }
                is Playable.Unsupported -> _ui.value = PlayerUi.Error(
                    PlayerError.UNSUPPORTED,
                    messageRes = playable.messageRes,
                    messageArg = playable.arg,
                )
            }
        }
    }

    fun retry(camera: Camera) {
        startedForId = null
        release()
        start(camera)
    }

    fun release() {
        mjpegJob?.cancel()
        mjpegJob = null
        exoPlayer?.removeListener(listener)
        exoPlayer?.release()
        exoPlayer = null
        _ui.value = PlayerUi.Idle
    }
}
