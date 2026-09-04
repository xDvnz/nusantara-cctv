package id.nusantara.cctv.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.model.Camera
import id.nusantara.cctv.ui.appContainer
import id.nusantara.cctv.ui.components.CameraCard
import id.nusantara.cctv.ui.components.EmptyState
import id.nusantara.cctv.ui.factoryOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(repository: CatalogRepository) : ViewModel() {
    val favorites: StateFlow<List<Camera>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@Composable
fun FavoritesScreen(onCameraClick: (Camera) -> Unit) {
    val vm: FavoritesViewModel = viewModel(factory = factoryOf {
        FavoritesViewModel(it.appContainer.catalogRepository)
    })
    val favorites by vm.favorites.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                stringResource(id.nusantara.cctv.R.string.favorites_title),
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            )
        }
        if (favorites.isEmpty()) {
            item {
                EmptyState(
                    Icons.Filled.FavoriteBorder,
                    stringResource(id.nusantara.cctv.R.string.favorites_empty_title),
                    stringResource(id.nusantara.cctv.R.string.favorites_empty_hint),
                )
            }
        }
        items(favorites, key = { it.id }) { CameraCard(it, onCameraClick) }
    }
}
