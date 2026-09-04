package id.nusantara.cctv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.model.Camera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val total: Int = 0,
    val online: Int = 0,
    val offline: Int = 0,
    val provinces: List<Pair<String, Int>> = emptyList(),
    val favorites: List<Camera> = emptyList(),
    val recentlyChecked: List<Camera> = emptyList(),
)

class HomeViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val state = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = state

    val catalogVersion = repository.version().stateIn(
        viewModelScope, SharingStarted.Lazily, null,
    )

    init {
        viewModelScope.launch {
            repository.cameras.collect { cameras ->
                val byProvince = cameras.groupBy { it.province }
                    .map { (p, list) -> p to list.size }
                    .sortedByDescending { it.second }
                state.value = state.value.copy(
                    total = cameras.size,
                    online = cameras.count { it.status == "ONLINE" },
                    offline = cameras.count { it.status != "ONLINE" },
                    provinces = byProvince,
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
    }
}
