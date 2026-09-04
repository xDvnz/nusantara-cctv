package id.nusantara.cctv.ui.regions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.nusantara.cctv.data.model.Camera
import id.nusantara.cctv.ui.appContainer
import id.nusantara.cctv.ui.components.CameraCard
import id.nusantara.cctv.ui.components.EmptyState
import id.nusantara.cctv.ui.factoryOf
import androidx.compose.material.icons.filled.LocationOn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionsScreen(onCameraClick: (Camera) -> Unit) {
    val vm: RegionsViewModel = viewModel(factory = factoryOf { RegionsViewModel(it.appContainer.catalogRepository) })
    val state by vm.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TopAppBar(
                title = { Text(state.path.joinToString(" › ").ifEmpty { "Wilayah" }) },
                navigationIcon = {
                    if (state.path.isNotEmpty()) {
                        IconButton(onClick = vm::navigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
            )
        }
        if (state.showingCameras) {
            if (state.cameras.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Filled.LocationOn,
                        "Tidak ada kamera",
                        "Belum ada kamera terdaftar di wilayah ini.",
                    )
                }
            }
            items(state.cameras, key = { it.id }) { CameraCard(it, onCameraClick) }
        } else {
            items(state.nodes, key = { it.name }) { node ->
                Card(
                    onClick = { vm.selectNode(node.name) },
                    modifier = Modifier.padding(0.dp),
                ) {
                    Text(
                        node.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp, 12.dp),
                    )
                }
            }
        }
    }
}
