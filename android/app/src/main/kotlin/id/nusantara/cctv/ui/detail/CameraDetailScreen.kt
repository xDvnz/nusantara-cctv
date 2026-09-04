package id.nusantara.cctv.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import id.nusantara.cctv.R
import id.nusantara.cctv.data.model.Camera
import id.nusantara.cctv.data.player.PlayerUi
import id.nusantara.cctv.data.player.StreamPlayerController
import id.nusantara.cctv.ui.appContainer
import id.nusantara.cctv.ui.components.StatusDot
import id.nusantara.cctv.ui.factoryOf
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraDetailViewModel(
    private val cameraId: String,
    container: id.nusantara.cctv.AppContainer,
) : ViewModel() {

    private val repository = container.catalogRepository
    private val engine = container.streamEngine
    private val probeBusy = java.util.concurrent.atomic.AtomicBoolean(false)

    val controller = StreamPlayerController(
        context = container.appContext,
        engine = engine,
        scope = viewModelScope,
    )

    val camera: Flow<Camera?> = repository.observeCamera(cameraId)
    val isFavorite: Flow<Boolean> = repository.observeIsFavorite(cameraId)
    val playerUi get() = controller.ui

    val exoPlayer: ExoPlayer? get() = controller.exoPlayer

    fun toggleFavorite() {
        viewModelScope.launch { repository.toggleFavorite(cameraId) }
    }

    fun startStream(camera: Camera) = controller.start(camera)

    fun retry(camera: Camera) = controller.retry(camera)

    fun releasePlayer() = controller.release()

    /** Tombol "Periksa status": probe ringan 1 kamera (bukan polling massal), update Room. */
    fun refreshStatus(camera: Camera) {
        if (!probeBusy.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val status = withContext(Dispatchers.IO) { engine.probeStatus(camera) }
                repository.updateStatus(camera.id, status, LocalDateTime.now().toString())
            } finally {
                probeBusy.set(false)
            }
        }
    }

    override fun onCleared() {
        controller.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraDetailScreen(
    cameraId: String,
    onBack: () -> Unit,
    onFullscreen: () -> Unit,
    onOpenMap: () -> Unit,
) {
    val vm: CameraDetailViewModel = viewModel(
        key = cameraId,
        factory = factoryOf { extras -> CameraDetailViewModel(cameraId, extras.appContainer) },
    )
    val camera by vm.camera.collectAsState(initial = null)
    val isFavorite by vm.isFavorite.collectAsState(initial = false)
    val playerUi by vm.playerUi.collectAsState()

    val cam = camera
    DisposableEffect(cam?.id) {
        cam?.let { vm.startStream(it) }
        onDispose { vm.releasePlayer() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(cam?.cameraName ?: "Detail kamera") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
            },
            actions = {
                IconButton(onClick = { if (cam != null) vm.toggleFavorite() }) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) stringResource(R.string.favorite_remove)
                        else stringResource(R.string.favorite_add),
                    )
                }
            },
        )

        if (cam == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        PlayerSurface(
            playerUi = playerUi,
            player = vm.exoPlayer,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                OutlinedButton(onClick = onFullscreen, modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = null)
                    Text("  Layar penuh")
                }
                OutlinedButton(onClick = { cam.let(vm::retry) }, modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text("  Muat ulang")
                }
                OutlinedButton(onClick = { cam.let(vm::refreshStatus) }, modifier = Modifier.height(40.dp)) {
                    Text("Periksa status")
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(cam.status)
                        Text(
                            "  ${cam.status}" + (cam.lastChecked?.let { " • dicek $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(cam.locationName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        listOfNotNull(cam.district, cam.subdistrict, cam.cityRegency, cam.province)
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (cam.latitude != null && cam.longitude != null) {
                        Text(
                            "Koordinat: %.6f, %.6f (%s)".format(
                                cam.latitude, cam.longitude, cam.locationAccuracy,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onOpenMap) {
                            Text(stringResource(R.string.open_in_map))
                        }
                    }
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.source_attribution, cam.sourceName, cam.operator),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        cam.termsOfUse,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerSurface(
    playerUi: PlayerUi,
    player: ExoPlayer?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
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
            playerUi is PlayerUi.MjpegFrame -> androidx.compose.foundation.Image(
                bitmap = playerUi.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
            playerUi is PlayerUi.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            else -> {}
        }

        val error = (playerUi as? PlayerUi.Error)?.message
        if (error != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC111111))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.error_player),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(error, color = Color(0xFFCCCCCC), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
