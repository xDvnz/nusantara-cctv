package id.nusantara.cctv.ui.about

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.catalog.CatalogSyncException
import id.nusantara.cctv.data.model.CameraSourceConfig
import id.nusantara.cctv.data.model.CatalogVersion
import id.nusantara.cctv.data.prefs.AppLocale
import id.nusantara.cctv.data.prefs.AppPreferencesRepository
import id.nusantara.cctv.data.prefs.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AboutUiState(
    val catalogUrl: String = "",
    val syncing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val updateChecking: Boolean = false,
    val updateChecked: Boolean = false,
    val updateAvailable: id.nusantara.cctv.data.update.UpdateInfo? = null,
)

class AboutViewModel(
    private val context: Context,
    private val prefs: AppPreferencesRepository,
    private val repository: CatalogRepository,
    private val updateChecker: id.nusantara.cctv.data.update.UpdateChecker,
) : ViewModel() {

    val version: StateFlow<CatalogVersion?> = repository.version()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val sources: StateFlow<List<CameraSourceConfig>> = repository.observeSources()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val cameraCount: StateFlow<Int> = repository.cameras
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val themeMode: StateFlow<ThemeMode> = prefs.preferences
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Lazily, ThemeMode.SYSTEM)

    val locale: StateFlow<AppLocale> = prefs.preferences
        .map { it.locale }
        .stateIn(viewModelScope, SharingStarted.Lazily, AppLocale.ID)

    private val _state = MutableStateFlow(AboutUiState())
    val state: StateFlow<AboutUiState> = _state

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(catalogUrl = prefs.snapshot().remoteCatalogUrl)
        }
    }

    fun onCatalogUrlChange(url: String) {
        _state.value = _state.value.copy(catalogUrl = url, message = null)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setLocale(locale: AppLocale) {
        viewModelScope.launch { prefs.setLocale(locale) }
    }

    fun saveAndSync() {
        val url = _state.value.catalogUrl.trim()
        viewModelScope.launch {
            _state.value = _state.value.copy(syncing = true, message = null)
            prefs.setRemoteCatalogUrl(url)
            repository.updateRemoteUrl(url)
            try {
                val result = repository.syncFromRemote()
                _state.value = _state.value.copy(
                    syncing = false,
                    message = if (result == CatalogRepository.SyncResult.UPDATED) {
                        context.getString(id.nusantara.cctv.R.string.sync_updated)
                    } else {
                        context.getString(id.nusantara.cctv.R.string.sync_up_to_date)
                    },
                )
            } catch (e: CatalogSyncException) {
                _state.value = _state.value.copy(syncing = false, message = e.message, isError = true)
            }
        }
    }

    /** Cek pembaruan manual dari layar Tentang. */
    fun checkForUpdate() {
        if (_state.value.updateChecking) return
        viewModelScope.launch {
            _state.value = _state.value.copy(updateChecking = true, updateChecked = false)
            val info = updateChecker.check(id.nusantara.cctv.BuildConfig.VERSION_NAME)
            _state.value = _state.value.copy(
                updateChecking = false,
                updateChecked = true,
                updateAvailable = info,
            )
        }
    }
}
