package id.nusantara.cctv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.model.Camera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val total: Int = 0,
    val online: Int = 0,
    val offline: Int = 0,
    val favorites: List<Camera> = emptyList(),
    val history: List<Camera> = emptyList(),
    val recentlyChecked: List<Camera> = emptyList(),
)

class HomeViewModel(private val repository: CatalogRepository) : ViewModel() {

    private val state = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = state

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
}
