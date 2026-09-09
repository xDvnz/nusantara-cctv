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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onCameraClick: (Camera) -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as id.nusantara.cctv.CctvApp).container
    val vm: HomeViewModel = viewModel(factory = factoryOf { HomeViewModel(it.appContainer.catalogRepository) })
    val state by vm.uiState.collectAsState()
    val refreshing by vm.refreshing.collectAsState()

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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stringResource(R.string.stat_total), state.total.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_online), state.online.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_offline), state.offline.toString(), Modifier.weight(1f))
            }
        }
        if (state.history.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.section_history)) }
            item {
                // grid 2 kolom; maksimal 6 kamera terbaru saja
                val rows = state.history.take(6).chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                .padding(16.dp),
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
        shape = RoundedCornerShape(12.dp),
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
                        .padding(8.dp),
                )
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
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
        modifier = Modifier.padding(top = 8.dp),
    )
}
