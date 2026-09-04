package id.nusantara.cctv.ui.regions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.model.Camera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RegionNode(
    val name: String,
    val count: Int,
)

data class RegionsUiState(
    val path: List<String> = emptyList(),
    val nodes: List<RegionNode> = emptyList(),
    val cameras: List<Camera> = emptyList(),
    val showingCameras: Boolean = false,
)

/** Drilldown Provinsi -> Kota/Kab -> Kecamatan -> daftar kamera. */
class RegionsViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val all = MutableStateFlow<List<Camera>>(emptyList())
    private val _state = MutableStateFlow(RegionsUiState())
    val state: StateFlow<RegionsUiState> = _state

    init {
        viewModelScope.launch {
            repository.cameras.collect { cameras ->
                all.value = cameras
                drillDown(_state.value.path)
            }
        }
    }

    fun selectNode(name: String) {
        val nextPath = if (_state.value.showingCameras) _state.value.path else _state.value.path + name
        drillDown(nextPath)
    }

    fun navigateUp() {
        val path = _state.value.path.dropLast(1)
        drillDown(path)
    }

    private fun drillDown(path: List<String>) {
        val cameras = all.value
        when (path.size) {
            0 -> {
                val nodes = cameras.groupBy { it.province }
                    .map { (name, list) -> RegionNode(name, list.size) }
                    .sortedByDescending { it.count }
                _state.value = RegionsUiState(path = emptyList(), nodes = nodes)
            }
            1 -> {
                val inProvince = cameras.filter { it.province == path[0] }
                val nodes = inProvince.groupBy { it.cityRegency }
                    .map { (name, list) -> RegionNode(name, list.size) }
                    .sortedByDescending { it.count }
                _state.value = RegionsUiState(path = path, nodes = nodes)
            }
            2 -> {
                val inCity = cameras.filter { it.province == path[0] && it.cityRegency == path[1] }
                val withDistrict = inCity.filter { !it.district.isNullOrBlank() }
                val nodes = withDistrict.groupBy { it.district.orEmpty() }
                    .map { (name, list) -> RegionNode(name, list.size) }
                    .sortedBy { it.name }
                _state.value = RegionsUiState(path = path, nodes = nodes)
            }
            else -> {
                val inDistrict = cameras.filter {
                    it.province == path[0] && it.cityRegency == path[1] && it.district == path[2]
                }
                _state.value = RegionsUiState(
                    path = path,
                    nodes = emptyList(),
                    cameras = inDistrict.sortedBy { it.cameraName },
                    showingCameras = true,
                )
            }
        }
    }
}
