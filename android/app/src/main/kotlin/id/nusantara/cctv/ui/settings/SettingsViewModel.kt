package id.nusantara.cctv.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.catalog.CatalogSyncException
import id.nusantara.cctv.data.model.CameraSourceConfig
import id.nusantara.cctv.data.model.CatalogVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.settingsDataStore by preferencesDataStore("settings")
private val KEY_CATALOG_URL = stringPreferencesKey("remote_catalog_url")

data class SettingsUiState(
    val catalogUrl: String = "",
    val syncing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class SettingsViewModel(
    private val context: Context,
    private val repository: CatalogRepository,
) : ViewModel() {

    val version: StateFlow<CatalogVersion?> = repository.version()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val sources: StateFlow<List<CameraSourceConfig>> = repository.observeSources()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        viewModelScope.launch {
            val saved = context.settingsDataStore.data.first()[KEY_CATALOG_URL].orEmpty()
            _state.value = _state.value.copy(catalogUrl = saved)
        }
    }

    fun onCatalogUrlChange(url: String) {
        _state.value = _state.value.copy(catalogUrl = url, message = null)
    }

    fun saveAndSync() {
        val url = _state.value.catalogUrl.trim()
        viewModelScope.launch {
            _state.value = _state.value.copy(syncing = true, message = null)
            context.settingsDataStore.edit { it[KEY_CATALOG_URL] = url }
            try {
                repository.updateRemoteUrl(url)
                val result = repository.syncFromRemote()
                _state.value = _state.value.copy(
                    syncing = false,
                    message = if (result == CatalogRepository.SyncResult.UPDATED)
                        "Katalog diperbarui dari server."
                    else "Katalog sudah versi terbaru.",
                )
            } catch (e: CatalogSyncException) {
                _state.value = _state.value.copy(syncing = false, message = e.message, isError = true)
            }
        }
    }
}
