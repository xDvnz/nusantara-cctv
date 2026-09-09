package id.nusantara.cctv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.nusantara.cctv.data.catalog.CatalogRepository
import id.nusantara.cctv.data.db.CameraDao
import id.nusantara.cctv.data.db.toModel
import id.nusantara.cctv.data.model.Camera
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class SearchFilters(
    val province: String? = null,
    val city: String? = null,
    val district: String? = null,
    val status: String? = null,
    val streamType: String? = null,
    val operator: String? = null,
)

data class SearchUiState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val results: List<Camera> = emptyList(),
    val groupedResults: Map<String, List<Camera>> = emptyMap(),
    val recentSearches: List<String> = emptyList(),
    val matchingCameras: List<Camera> = emptyList(),
    val matchingLocations: List<String> = emptyList(),
    val provinces: List<String> = emptyList(),
    val cities: List<String> = emptyList(),
    val districts: List<String> = emptyList(),
    val operators: List<String> = emptyList(),
    val endReached: Boolean = false,
    val loadingMore: Boolean = false,
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val cameraDao: CameraDao,
    repository: CatalogRepository,
    private val prefsRepository: id.nusantara.cctv.data.prefs.AppPreferencesRepository,
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 60
    }

    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(SearchFilters())
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Pull-to-refresh: muat ulang halaman pertama hasil saat ini. */
    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            try {
                reload()
            } finally {
                _refreshing.value = false
            }
        }
    }

    private var loadedCount = 0

    init {
        viewModelScope.launch {
            repository.provinces.collect { _state.value = _state.value.copy(provinces = it) }
        }
        viewModelScope.launch {
            repository.operators.collect { _state.value = _state.value.copy(operators = it) }
        }
        viewModelScope.launch {
            query.debounce(250).collect { q ->
                _state.value = _state.value.copy(query = q)
                if (q.isBlank()) {
                    _state.value = _state.value.copy(matchingCameras = emptyList(), matchingLocations = emptyList())
                } else {
                    val cams = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        cameraDao.searchSuggestions(q, 5)
                    }
                    val locs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        cameraDao.searchLocations(q, 5)
                    }
                    _state.value = _state.value.copy(
                        matchingCameras = cams.map { it.toModel() },
                        matchingLocations = locs,
                    )
                }
                reload()
            }
        }
        viewModelScope.launch {
            filters.collect { f ->
                val cities = f.province?.let { repository.citiesOf(it) }.orEmpty()
                val districts = if (f.province != null && f.city != null) {
                    repository.districtsOf(f.province, f.city)
                } else emptyList()
                _state.value = _state.value.copy(filters = f, cities = cities, districts = districts)
                reload()
            }
        }
    }

    fun onQueryChange(q: String) {
        query.value = q
    }

    /** Nilai query awal untuk field pencarian (state lokal, bukan yang ter-debounce). */
    fun initialQuery(): String = _state.value.query

    fun onFiltersChange(f: SearchFilters) {
        filters.value = f
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.endReached) return
        _state.value = s.copy(loadingMore = true)
        viewModelScope.launch {
            val page = queryDb(PAGE_SIZE, loadedCount)
            loadedCount += page.size
            val merged = _state.value.groupedResults.toMutableMap()
            page.groupBy { it.province }.forEach { (province, cams) ->
                merged[province] = (merged[province] ?: emptyList()) + cams
            }
            _state.value = _state.value.copy(
                results = _state.value.results + page,
                groupedResults = merged,
                loadingMore = false,
                endReached = page.size < PAGE_SIZE,
            )
        }
    }

    private suspend fun reload() {
        loadedCount = 0
        val page = queryDb(PAGE_SIZE, 0)
        _isLoading.value = false
        _state.value = _state.value.copy(
            results = page,
            groupedResults = page.groupBy { it.province },
            endReached = page.size < PAGE_SIZE,
        )
    }

    fun onSearchSubmit(query: String) {
        viewModelScope.launch {
            prefsRepository.addRecentSearch(query)
            _state.value = _state.value.copy(recentSearches = prefsRepository.getRecentSearches())
        }
    }

    private suspend fun queryDb(limit: Int, offset: Int): List<Camera> {
        val s = _state.value
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            cameraDao.search(
                query = s.query,
                province = s.filters.province,
                city = s.filters.city,
                district = s.filters.district,
                status = s.filters.status,
                streamType = s.filters.streamType,
                operator = s.filters.operator,
                limit = limit,
                offset = offset,
            ).map { it.toModel() }
        }
    }
}
