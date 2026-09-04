package id.nusantara.cctv.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.nusantara.cctv.R
import id.nusantara.cctv.data.player.PlayerError
import id.nusantara.cctv.data.player.PlayerUi

/** Konversi error pemutar menjadi teks terjemahan untuk UI. */
@Composable
fun playerErrorText(ui: PlayerUi.Error): String = when {
    ui.messageRes != null -> ui.messageArg?.let { stringResource(ui.messageRes, it) }
        ?: stringResource(ui.messageRes)
    else -> when (ui.kind) {
        PlayerError.NETWORK -> stringResource(R.string.connection_failed)
        PlayerError.UNAVAILABLE -> stringResource(R.string.stream_unavailable)
        PlayerError.PLAYBACK -> stringResource(R.string.play_failed, ui.detail ?: "-")
        PlayerError.SOURCE -> stringResource(R.string.source_not_playable, ui.detail ?: "-")
        PlayerError.MJPEG_INACTIVE -> stringResource(R.string.mjpeg_inactive)
        PlayerError.ENDED -> stringResource(R.string.stream_ended)
        PlayerError.UNSUPPORTED -> stringResource(R.string.source_not_playable, "-")
    }
}
