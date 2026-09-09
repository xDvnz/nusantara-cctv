package id.nusantara.cctv.ui.detail

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import id.nusantara.cctv.AppContainer
import id.nusantara.cctv.data.player.PlayerUi
import id.nusantara.cctv.data.player.StreamPlayerController
import id.nusantara.cctv.ui.appContainer
import id.nusantara.cctv.ui.factoryOf

class FullscreenPlayerViewModel(
    private val cameraId: String,
    container: AppContainer,
) : ViewModel() {

    private val repository = container.catalogRepository

    val controller = StreamPlayerController(
        context = container.appContext,
        engine = container.streamEngine,
        scope = viewModelScope,
    )

    val playerUi get() = controller.ui
    val exoPlayer get() = controller.exoPlayer

    suspend fun camera() = repository.camera(cameraId)

    override fun onCleared() {
        controller.release()
    }
}

@Composable
fun FullscreenPlayerScreen(cameraId: String, onBack: () -> Unit) {
    val vm: FullscreenPlayerViewModel = viewModel(
        key = "fullscreen-$cameraId",
        factory = factoryOf { extras -> FullscreenPlayerViewModel(cameraId, extras.appContainer) },
    )
    val playerUi by vm.playerUi.collectAsState()
    val context = LocalContext.current

    // paksa landscape selama layar ini aktif; kembalikan saat keluar
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            vm.controller.release()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        vm.camera()?.let { vm.controller.start(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val player = vm.exoPlayer
        val ui = playerUi
        when {
            player != null -> AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                update = { it.player = player },
            )
            ui is PlayerUi.MjpegFrame -> androidx.compose.foundation.Image(
                bitmap = ui.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
            else -> {}
        }

        val error = (ui as? PlayerUi.Error)
        if (error != null) {
            androidx.compose.material3.Text(
                playerErrorText(error),
                color = Color.White,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        }

        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = androidx.compose.ui.res.stringResource(id.nusantara.cctv.R.string.back),
                tint = Color.White,
            )
        }
    }
}
