package id.nusantara.cctv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.model.Camera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val total: Int = 0,
    val online: Int = 0,
    val offline: Int = 0,
    val favorites: List<Camera> = emptyList(),
    val history: List<Camera> = emptyList(),
    val recentlyChecked: List<Camera> = emptyList(),
    val nearbyCameras: List<Pair<Camera, Double>> = emptyList(),
)

class HomeViewModel(
    private val repository: CatalogRepository,
    private val locationProvider: id.nusantara.cctv.util.LocationProvider,
) : ViewModel() {

    private val state = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = state

    /** true selama pull-to-refresh berjalan. */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    /** B1: hitung kamera terdekat; dipanggil setelah izin lokasi diminta. */
    fun loadNearby() {
        viewModelScope.launch {
            val location = withContext(kotlinx.coroutines.Dispatchers.IO) {
                locationProvider.getCurrentLocation()
            } ?: return@launch
            val cameras = repository.cameras.let { flow ->
                var latest: List<Camera> = emptyList()
                flow.collect { latest = it }
                latest
            }
            val nearby = cameras
                .filter { it.latitude != null && it.longitude != null }
                .map { cam ->
                    cam to haversineKm(
                        location.latitude, location.longitude,
                        cam.latitude!!, cam.longitude!!,
                    )
                }
                .sortedBy { it.second }
                .take(10)
            state.value = state.value.copy(nearbyCameras = nearby)
        }
    }

    init {
        viewModelScope.launch {
            repository.cameras.collect { cameras ->
                state.value = state.value.copy(
                    total = cameras.size,
                    online = cameras.count { it.status == "ONLINE" },
                    offline = cameras.count { it.status != "ONLINE" },
                    recentlyChecked = cameras
                        .filter { it.lastChecked != null }
                        .sortedByDescending { it.lastChecked }
                        .take(5),
                )
            }
        }
        viewModelScope.launch {
            repository.favorites.collect { favs ->
                state.value = state.value.copy(favorites = favs.take(5))
            }
        }
        viewModelScope.launch {
            repository.observeRecentHistory(limit = 6).collect { cams ->
                state.value = state.value.copy(history = cams)
            }
        }
    }

    /**
     * Pull-to-refresh: sinkron katalog remote bila URL dikonfigurasi; bila tidak,
     * probe ulang status kamera yang sedang tampil (favorit + riwayat + terbaru).
     */
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2).let { it * it }
        return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }

    fun refresh(engineProbe: suspend (Camera) -> String) {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            try {
                val synced = runCatching { repository.syncFromRemote() }.isSuccess
                if (!synced) {
                    val visible = buildSet {
                        addAll(state.value.history.map { it.id })
                        addAll(state.value.favorites.map { it.id })
                        addAll(state.value.recentlyChecked.map { it.id })
                    }.toList()
                    runCatching { repository.refreshVisible(visible, engineProbe) }
                }
            } finally {
                _refreshing.value = false
            }
        }
    }
}
