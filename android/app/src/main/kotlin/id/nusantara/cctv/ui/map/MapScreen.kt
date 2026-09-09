package id.nusantara.cctv.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    private val all = MutableStateFlow<List<Camera>>(emptyList())

    init {
        viewModelScope.launch {
            repository.cameras.collect { list ->
                all.value = list
                camerasFlow.value = list.filter { it.latitude != null && it.longitude != null }.map {
                    MapCameraItem(it.id, it.cameraName, it.latitude!!, it.longitude!!, it.status)
                }
            }
        }
    }

    fun setMapLayer(layer: MapLayer) {
        viewModelScope.launch { prefs.setMapLayer(layer.name) }
    }

    fun cameraById(id: String): Camera? = all.value.firstOrNull { it.id == id }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onCameraClick: (Camera) -> Unit) {
    val vm: MapViewModel = viewModel(factory = factoryOf {
        MapViewModel(it.appContainer.catalogRepository, it.appContainer.preferencesRepository)
    })
    val items by vm.cameras.collectAsState()
    val mapLayer by vm.mapLayer.collectAsState()
    val latestItems = remember { mutableStateOf(items) }
    latestItems.value = items

    var mapRef by remember { mutableStateOf<MapView?>(null) }
    var layerSheetOpen by remember { mutableStateOf(false) }
    var selectedCamera by remember { mutableStateOf<Camera?>(null) }
    val selectCamera: (Camera) -> Unit = { selectedCamera = it }

    // status bar terminal (gaya OSIRIS): pusat peta + jumlah entitas — murah, update saat rebuild
    var centerText by remember { mutableStateOf("--.----, ---.----") }

    Box(Modifier.fillMaxSize()) {
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
                    attachClusterListener(this) {
                        rebuildMarkers(this, latestItems.value, vm, selectCamera)
                        centerText = "%.4f, %.4f".format(
                            this.mapCenter.latitude, this.mapCenter.longitude,
                        )
                    }
                }
            },
            update = { map ->
                val wanted = MapLayers.tileSource(mapLayer)
                if (map.tileProvider.tileSource !== wanted) map.setTileSource(wanted)
                rebuildMarkers(map, latestItems.value, vm, selectCamera)
                centerText = "%.4f, %.4f".format(map.mapCenter.latitude, map.mapCenter.longitude)
            },
        )

        // ===== Panel LAYERS (gaya OSIRIS): melayang kiri-atas =====
        LayerPanel(
            selected = mapLayer,
            onSelect = vm::setMapLayer,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        )

        // ===== Status bar bawah: LIVE / ENTITIES / koordinat =====
        StatusBar(
            entityCount = latestItems.value.size,
            centerText = centerText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
        )
    }

    if (layerSheetOpen) {
        ModalBottomSheet(onDismissRequest = { layerSheetOpen = false }) {
            Text(
                stringResource(R.string.map_layer_title),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            MapLayer.entries.forEach { layer ->
                LayerRow(layer, selected = layer == mapLayer) {
                    vm.setMapLayer(layer)
                    layerSheetOpen = false
                }
            }
        }
    }

    selectedCamera?.let { camera ->
        ModalBottomSheet(onDismissRequest = { selectedCamera = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(camera.cameraName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${camera.locationName}, ${camera.cityRegency}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "${camera.sourceName} · ${camera.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { onCameraClick(camera) }) {
                        Text(stringResource(R.string.map_open_camera))
                    }
                }
            }
        }
    }

    LaunchedEffect(items) {
        mapRef?.let { map ->
            fitBounds(map, latestItems.value)
            rebuildMarkers(map, latestItems.value, vm, selectCamera)
            centerText = "%.4f, %.4f".format(map.mapCenter.latitude, map.mapCenter.longitude)
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

/**
 * Panel melayang berisi tombol LAYERS (buka sheet pilihan basemap).
 * Gaya referensi: kartu kecil bergaya terminal di atas peta.
 */
@Composable
private fun LayerPanel(
    selected: MapLayer,
    onSelect: (MapLayer) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // tombol utama
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .clickable { open = !open }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(R.string.map_osiris_layers).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                )
            }
        }

        // pilihan basemap inline (muncul saat panel dibuka)
        AnimatedVisibility(visible = open, enter = fadeIn(), exit = fadeOut()) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                ),
            ) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    MapLayer.entries.forEach { layer ->
                        Row(
                            modifier = Modifier
                                .clickable { onSelect(layer) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(width = 34.dp, height = 22.dp)
                                    .background(layerPreviewColor(layer), RoundedCornerShape(4.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(layer.labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                            )
                            if (layer == selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Status bar bawah gaya terminal: LIVE + jumlah entitas + koordinat pusat. */
@Composable
private fun StatusBar(
    entityCount: Int,
    centerText: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.map_osiris_live).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF4CAF50),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.map_osiris_entities, entityCount),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
            Text(
                centerText,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LayerRow(layer: MapLayer, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { role = Role.RadioButton }
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(width = 52.dp, height = 36.dp)
                .background(layerPreviewColor(layer), RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(16.dp))
        Text(stringResource(layer.labelRes), modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.map_layer_selected))
    }
}

private fun layerPreviewColor(layer: MapLayer): Color = when (layer) {
    MapLayer.DEFAULT -> Color(0xFF7FAF67)
    MapLayer.SATELLITE -> Color(0xFF557B45)
    MapLayer.DARK -> Color(0xFF30363D)
    MapLayer.TERRAIN -> Color(0xFF927A50)
}

/** Gambar ulang marker/cluster sesuai zoom-viewport sekarang. */
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
