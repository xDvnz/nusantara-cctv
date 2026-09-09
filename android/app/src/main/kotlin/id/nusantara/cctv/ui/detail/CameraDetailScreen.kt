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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Surface
import id.nusantara.cctv.ui.theme.Shapes
import id.nusantara.cctv.ui.theme.Spacing
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

    /** Catat ke riwayat beranda (baru ditonton). */
    fun recordView() {
        viewModelScope.launch { repository.recordView(cameraId) }
    }

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
        cam?.let {
            vm.startStream(it)
            vm.recordView()
        }
        onDispose { vm.releasePlayer() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(cam?.cameraName ?: stringResource(R.string.detail_title_fallback)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                .verticalScroll(rememberScrollState()),
        ) {
            // ===== Quick actions row (D4): Save | Share | Fullscreen | Refresh =====
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    onClick = { if (cam != null) vm.toggleFavorite() },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(if (isFavorite) R.string.action_saved else R.string.action_save), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { cam?.let { shareCamera(it, context) } },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.action_share), maxLines = 1)
                }
                OutlinedButton(
                    onClick = onFullscreen,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.fullscreen_button), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { cam?.let(vm::refreshStatus) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.action_refresh), maxLines = 1)
                }
            }

            // ===== Essential info (selalu tampil) =====
            Column(
                modifier = Modifier.padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(cam.status)
                    Text(
                        "  ${cam.status}" + (cam.lastChecked?.let {
                            " • " + stringResource(R.string.checked_at, it)
                        } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(cam.cameraName, style = MaterialTheme.typography.titleLarge)
                if (!cam.district.isNullOrBlank()) {
                    Text(
                        cam.district,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("${cam.cityRegency} • ${cam.province}", style = MaterialTheme.typography.bodyMedium)
                Surface(
                    shape = Shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        cam.operator,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    )
                }
            }

            // ===== Technical details (collapsible, B3) =====
            var showTechnical by remember { mutableStateOf(false) }
            Column(Modifier.padding(horizontal = Spacing.lg)) {
                TextButton(
                    onClick = { showTechnical = !showTechnical },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (showTechnical) R.string.hide_technical_details
                            else R.string.show_technical_details,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                AnimatedVisibility(visible = showTechnical) {
                    Column(
                        modifier = Modifier.padding(top = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        MetadataRow(stringResource(R.string.metadata_stream_type), cam.streamType)
                        MetadataRow(
                            stringResource(R.string.metadata_stream_url),
                            cam.streamUrl.take(50) + if (cam.streamUrl.length > 50) "…" else "",
                        )
                        MetadataRow(stringResource(R.string.metadata_source), cam.sourceName)
                        MetadataRow(stringResource(R.string.metadata_portal_url), cam.sourceUrl)
                        if (cam.latitude != null && cam.longitude != null) {
                            MetadataRow(
                                stringResource(R.string.metadata_coordinates),
                                "%.6f, %.6f".format(cam.latitude, cam.longitude),
                            )
                        }
                        if (cam.confidenceScore > 0) {
                            MetadataRow(
                                stringResource(R.string.metadata_confidence),
                                "%.0f%%".format(cam.confidenceScore * 100),
                            )
                        }
                        if (cam.publicIdentifier.isNotBlank()) {
                            MetadataRow(stringResource(R.string.metadata_camera_id), cam.publicIdentifier)
                        }
                        Text(
                            cam.termsOfUse,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ===== Buka di peta =====
            if (cam.latitude != null && cam.longitude != null) {
                OutlinedButton(
                    onClick = onOpenMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.open_in_map))
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

private fun shareCamera(camera: Camera, context: android.content.Context) {
    val shareText = buildString {
        appendLine("CCTV: ${camera.cameraName}")
        appendLine("${camera.cityRegency}, ${camera.province}")
        appendLine(camera.streamUrl)
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "CCTV: ${camera.cameraName}")
        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(android.content.Intent.createChooser(intent, null))
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
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

        val error = (playerUi as? PlayerUi.Error)
        if (error != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC111111))
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.error_player),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    playerErrorText(error),
                    color = Color(0xFFCCCCCC),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
