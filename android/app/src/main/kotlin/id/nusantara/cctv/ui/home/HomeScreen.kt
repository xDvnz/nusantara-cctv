package id.nusantara.cctv.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.nusantara.cctv.R
import id.nusantara.cctv.data.model.Camera
import id.nusantara.cctv.ui.appContainer
import id.nusantara.cctv.ui.components.CameraCard
import id.nusantara.cctv.ui.components.StatusDot
import id.nusantara.cctv.ui.factoryOf
import id.nusantara.cctv.ui.theme.Shapes
import id.nusantara.cctv.ui.theme.Spacing

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onCameraClick: (Camera) -> Unit, onSettingsClick: () -> Unit = {}) {
    val context = LocalContext.current
    val container = (context.applicationContext as id.nusantara.cctv.CctvApp).container
    val vm: HomeViewModel = viewModel(factory = factoryOf {
        HomeViewModel(it.appContainer.catalogRepository, it.appContainer.locationProvider)
    })
    val state by vm.uiState.collectAsState()
    val refreshing by vm.refreshing.collectAsState()

    // B1: minta izin lokasi sekali; ditolak = section tersembunyi, app tetap jalan
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val ctx = context
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        vm.loadNearby()
    }

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            vm.refresh { cam ->
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    container.streamEngine.probeStatus(cam)
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                StatCard(stringResource(R.string.stat_total), state.total.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_online), state.online.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_offline), state.offline.toString(), Modifier.weight(1f))
            }
        }
        // B1: Near You — tampil hanya bila izin lokasi diberikan & ada hasil
        if (state.nearbyCameras.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.section_near_you)) }
            items(state.nearbyCameras.take(3), key = { "near-${it.first.id}" }) { (camera, distance) ->
                Column {
                    CameraCard(camera, onCameraClick)
                    Text(
                        stringResource(R.string.distance_km, distance),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
                    )
                }
            }
        }
        if (state.history.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.section_history)) }
            item {
                // grid 2 kolom; maksimal 6 kamera terbaru saja
                val rows = state.history.take(6).chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            row.forEach { cam ->
                                HistoryTile(cam, Modifier.weight(1f), onCameraClick)
                            }
                            if (row.size == 1) {
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        if (state.favorites.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.section_favorites)) }
            items(state.favorites, key = { "fav-${it.id}" }) { CameraCard(it, onCameraClick) }
        }
        item { SectionTitle(stringResource(R.string.section_recent_checked)) }
        items(state.recentlyChecked, key = { "recent-${it.id}" }) { CameraCard(it, onCameraClick) }
    }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Kartu mini riwayat 2 kolom: panel visual placeholder + nama + wilayah. */
@Composable
private fun HistoryTile(camera: Camera, modifier: Modifier = Modifier, onClick: (Camera) -> Unit) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick(camera) },
        shape = Shapes.medium,
    ) {
        Column {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                StatusDot(
                    camera.status,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.sm),
                )
            }
            Column(Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)) {
                Text(
                    camera.cameraName,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    camera.cityRegency,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = Spacing.sm),
    )
}
