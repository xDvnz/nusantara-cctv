package id.nusantara.cctv.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.nusantara.cctv.data.model.Camera
import id.nusantara.cctv.ui.appContainer
import id.nusantara.cctv.ui.components.CameraCard
import id.nusantara.cctv.ui.components.EmptyState
import id.nusantara.cctv.ui.factoryOf
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2

private val STATUS_OPTIONS = listOf("ONLINE", "OFFLINE", "TIMEOUT", "AUTH_REQUIRED", "INVALID_STREAM", "UNKNOWN")
private val STREAM_TYPES = listOf("HLS", "DASH", "MJPEG", "RTSP")

@Composable
fun SearchScreen(onCameraClick: (Camera) -> Unit) {
    val vm: SearchViewModel = viewModel(factory = factoryOf {
        SearchViewModel(it.appContainer.database.cameraDao(), it.appContainer.catalogRepository)
    })
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 4 && listState.layoutInfo.totalItemsCount > 0
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) vm.loadMore()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Cari nama kamera, lokasi, wilayah...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterDropdown(
                label = state.filters.province ?: "Provinsi",
                options = state.provinces,
                selected = state.filters.province,
                onSelect = { vm.onFiltersChange(state.filters.copy(province = it, city = null, district = null)) },
            )
            FilterDropdown(
                label = state.filters.city ?: "Kota/Kab",
                options = state.cities,
                selected = state.filters.city,
                enabled = state.filters.province != null,
                onSelect = { vm.onFiltersChange(state.filters.copy(city = it, district = null)) },
            )
            FilterDropdown(
                label = state.filters.district ?: "Kecamatan",
                options = state.districts,
                selected = state.filters.district,
                enabled = state.filters.city != null,
                onSelect = { vm.onFiltersChange(state.filters.copy(district = it)) },
            )
            FilterDropdown(
                label = state.filters.status ?: "Status",
                options = STATUS_OPTIONS,
                selected = state.filters.status,
                onSelect = { vm.onFiltersChange(state.filters.copy(status = it)) },
            )
            FilterDropdown(
                label = state.filters.streamType ?: "Stream",
                options = STREAM_TYPES,
                selected = state.filters.streamType,
                onSelect = { vm.onFiltersChange(state.filters.copy(streamType = it)) },
            )
            FilterDropdown(
                label = state.filters.operator ?: "Operator",
                options = state.operators,
                selected = state.filters.operator,
                onSelect = { vm.onFiltersChange(state.filters.copy(operator = it)) },
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.results.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Filled.Inventory2,
                        if (state.query.isBlank()) "Mulai mencari" else "Tidak ditemukan",
                        if (state.query.isBlank())
                            "Ketik nama kamera, jalan, kecamatan, atau kota. Gunakan chip filter untuk mempersempit."
                        else
                            "Coba kata kunci lain atau longgarkan filter.",
                    )
                }
            }
            items(state.results, key = { it.id }) { CameraCard(it, onCameraClick) }
            if (state.loadingMore) {
                item { Text("Memuat...", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    options: List<String>,
    selected: String?,
    enabled: Boolean = true,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    FilterChip(
        selected = selected != null,
        enabled = enabled,
        onClick = { expanded = !expanded },
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.padding(0.dp)) },
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        if (selected != null) {
            DropdownMenuItem(text = { Text("(semua)") }, onClick = {
                expanded = false
                onSelect(null)
            })
        }
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                    expanded = false
                    onSelect(option)
                },
            )
        }
    }
}
