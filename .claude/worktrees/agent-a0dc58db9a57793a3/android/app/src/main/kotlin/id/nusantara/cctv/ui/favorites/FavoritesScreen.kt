package id.nusantara.cctv.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import id.nusantara.cctv.CctvApp
import id.nusantara.cctv.R
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.model.Camera
import id.nusantara.cctv.ui.appContainer
import id.nusantara.cctv.ui.components.CameraCard
import id.nusantara.cctv.ui.components.EmptyState
import id.nusantara.cctv.ui.factoryOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(private val repository: CatalogRepository) : ViewModel() {
    val favorites: StateFlow<List<Camera>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    /** Pull-to-refresh: probe ulang status kamera favorit. */
    fun refresh(engineProbe: suspend (Camera) -> String) {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            try {
                repository.refreshVisible(favorites.value.map { it.id }, engineProbe)
            } finally {
                _refreshing.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(onCameraClick: (Camera) -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as CctvApp).container
    val vm: FavoritesViewModel = viewModel(factory = factoryOf {
        FavoritesViewModel(it.appContainer.catalogRepository)
    })
    val favorites by vm.favorites.collectAsState()
    val refreshing by vm.refreshing.collectAsState()

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            vm.refresh { cam ->
                withContext(Dispatchers.IO) { container.streamEngine.probeStatus(cam) }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.favorites_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            if (favorites.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Filled.FavoriteBorder,
                        stringResource(R.string.favorites_empty_title),
                        stringResource(R.string.favorites_empty_hint),
                    )
                }
            }
            items(favorites, key = { it.id }) { CameraCard(it, onCameraClick) }
        }
    }
}
