package id.nusantara.cctv.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import id.nusantara.cctv.R
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.model.Camera
import id.nusantara.cctv.ui.appContainer
import id.nusantara.cctv.ui.factoryOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapViewModel(
    private val repository: CatalogRepository,
    private val prefs: id.nusantara.cctv.data.prefs.AppPreferencesRepository,
) : ViewModel() {

    private val camerasFlow = MutableStateFlow<List<MapCameraItem>>(emptyList())
    val cameras: StateFlow<List<MapCameraItem>> = camerasFlow

    val mapLayer: StateFlow<MapLayer> = prefs.preferences
        .map { runCatching { MapLayer.valueOf(it.mapLayer) }.getOrDefault(MapLayer.DEFAULT) }
        .stateIn(viewModelScope, SharingStarted.Lazily, MapLayer.DEFAULT)

    fun setMapLayer(layer: MapLayer) {
        viewModelScope.launch { prefs.setMapLayer(layer.name) }
    }

    private val all = MutableStateFlow<List<Camera>>(emptyList())

    init {
        viewModelScope.launch {
            repository.cameras.collect { list ->
                all.value = list
                camerasFlow.value = list
                    .filter { it.latitude != null && it.longitude != null }
                    .map {
                        MapCameraItem(
                            id = it.id,
                            name = it.cameraName,
                            lat = it.latitude!!,
                            lng = it.longitude!!,
                            status = it.status,
                        )
                    }
            }
        }
    }

    fun cameraById(id: String): Camera? = all.value.firstOrNull { it.id == id }
}

@Composable
fun MapScreen(onCameraClick: (Camera) -> Unit) {
    val vm: MapViewModel = viewModel(factory = factoryOf {
        MapViewModel(it.appContainer.catalogRepository, it.appContainer.preferencesRepository)
    })
    val items by vm.cameras.collectAsState()
    val mapLayer by vm.mapLayer.collectAsState()

    // holder agar listener zoom/pan selalu membaca daftar terbaru
    val latestItems = remember { mutableStateOf(items) }
    latestItems.value = items

    var mapRef by remember { mutableStateOf<MapView?>(null) }
    var layerMenuOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().userAgentValue = "id.nusantara.cctv"
                MapView(ctx).apply {
                    setTileSource(MapLayers.tileSource(mapLayer))
                    setMultiTouchControls(true)
                    controller.setZoom(4.8)
                    controller.setCenter(GeoPoint(-2.5, 118.0))
                    mapRef = this
                    attachClusterListener(this) { rebuildMarkers(this, latestItems.value, vm, onCameraClick) }
                }
            },
            update = { map ->
                val wanted = MapLayers.tileSource(mapLayer)
                if (map.tileProvider.tileSource !== wanted) {
                    map.setTileSource(wanted)
                }
                rebuildMarkers(map, latestItems.value, vm, onCameraClick)
            },
        )

        // Tombol pilihan layer (kanan atas) — seperti pemilih peta pada aplikasi peta populer
        androidx.compose.material3.FloatingActionButton(
            onClick = { layerMenuOpen = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Filled.Layers,
                contentDescription = stringResource(R.string.map_layer_title),
            )
            androidx.compose.material3.DropdownMenu(
                expanded = layerMenuOpen,
                onDismissRequest = { layerMenuOpen = false },
            ) {
                MapLayer.entries.forEach { layer ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(layer.labelRes)) },
                        trailingIcon = {
                            if (layer == mapLayer) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = {
                            vm.setMapLayer(layer)
                            layerMenuOpen = false
                        },
                    )
                }
            }
        }
    }

    LaunchedEffect(items) {
        mapRef?.let { map ->
            fitBounds(map, latestItems.value)
            rebuildMarkers(map, latestItems.value, vm, onCameraClick)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapRef?.overlays?.clear()
            mapRef?.onDetach()
            mapRef = null
        }
    }
}

private fun rebuildMarkers(
    map: MapView,
    items: List<MapCameraItem>,
    vm: MapViewModel,
    onCameraClick: (Camera) -> Unit,
) {
    map.overlays.removeAll { it is Marker }
    if (items.isEmpty()) {
        map.invalidate()
        return
    }
    val density = map.resources.displayMetrics.density
    val clusterer = CameraClusterer()
    val groups = clusterer.cluster(items, map.projection, cellPxOverride = (90 * density).toInt())
    for (group in groups) {
        val marker = Marker(map)
        marker.position = GeoPoint(group.centerLat, group.centerLng)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        if (group.isCluster) {
            marker.icon = MarkerIcons.cluster(group.items.size)
            marker.title = "${group.items.size} kamera"
            marker.setOnMarkerClickListener { _, _ ->
                map.controller.animateTo(marker.position)
                map.controller.setZoom(map.zoomLevelDouble + 2)
                true
            }
        } else {
            val item = group.items.first()
            marker.icon = MarkerIcons.dot(item.status)
            marker.title = item.name
            marker.setOnMarkerClickListener { _, _ ->
                vm.cameraById(item.id)?.let { camera ->
                    onCameraClick(camera)
                    true
                } ?: false
            }
        }
        map.overlays.add(marker)
    }
    map.invalidate()
}
