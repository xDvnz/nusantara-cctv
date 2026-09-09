package id.nusantara.cctv.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.nusantara.cctv.ui.theme.Spacing
import id.nusantara.cctv.ui.theme.Shapes
import id.nusantara.cctv.R
import id.nusantara.cctv.data.model.Camera
import id.nusantara.cctv.ui.appContainer
import id.nusantara.cctv.ui.components.CameraCard
import id.nusantara.cctv.ui.components.CameraCardSkeleton
import id.nusantara.cctv.ui.components.EmptyState
import id.nusantara.cctv.ui.factoryOf

private val STATUS_OPTIONS = listOf("ONLINE", "OFFLINE", "TIMEOUT", "AUTH_REQUIRED", "INVALID_STREAM", "UNKNOWN")
private val STREAM_TYPES = listOf("HLS", "DASH", "MJPEG", "RTSP")

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)
@Composable
fun SearchScreen(onCameraClick: (Camera) -> Unit) {
    val vm: SearchViewModel = viewModel(factory = factoryOf {
        SearchViewModel(
            it.appContainer.database.cameraDao(),
            it.appContainer.catalogRepository,
            it.appContainer.preferencesRepository,
        )
    })
    val state by vm.state.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val listState = rememberLazyListState()

    // Field memakai state lokal agar tidak tertinggal debounce; VM tetap menerima setiap ketikan.
    var fieldText by remember { mutableStateOf(vm.initialQuery()) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 4 && listState.layoutInfo.totalItemsCount > 0
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) vm.loadMore()
    }

    val activeFilters = listOfNotNull(
        state.filters.province, state.filters.city, state.filters.district,
        state.filters.status, state.filters.streamType, state.filters.operator,
    ).size

    Column(modifier = Modifier.fillMaxSize()) {
        // --- Search bar modern (pill) ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            shape = Shapes.pill,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.padding(start = Spacing.lg),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BasicTextField(
                    value = fieldText,
                    onValueChange = {
                        fieldText = it
                        vm.onQueryChange(it)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.md, vertical = 14.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { vm.onSearchSubmit(fieldText) },
                    ),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (fieldText.isEmpty()) {
                                Text(
                                    stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
                AnimatedVisibility(
                    visible = fieldText.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    IconButton(onClick = {
                        fieldText = ""
                        vm.onQueryChange("")
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear))
                    }
                }
                Spacer(Modifier.size(8.dp))
            }
        }

        // --- Suggestions (B2) ---
        if (state.matchingCameras.isNotEmpty() || state.matchingLocations.isNotEmpty() ||
            (fieldText.isBlank() && (state.recentSearches.isNotEmpty()))) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(vertical = Spacing.xs)) {
                    if (fieldText.isBlank() && state.recentSearches.isNotEmpty()) {
                        SuggestSection(stringResource(R.string.suggestions_recent)) {
                            state.recentSearches.forEach { q ->
                                SuggestRow(Icons.Filled.History, q) {
                                    fieldText = q
                                    vm.onQueryChange(q)
                                    vm.onSearchSubmit(q)
                                }
                            }
                        }
                    }
                    if (state.matchingLocations.isNotEmpty()) {
                        SuggestSection(stringResource(R.string.suggestions_locations)) {
                            state.matchingLocations.forEach { loc ->
                                SuggestRow(Icons.Filled.LocationCity, loc) {
                                    fieldText = loc
                                    vm.onQueryChange(loc)
                                }
                            }
                        }
                    }
                    if (state.matchingCameras.isNotEmpty()) {
                        SuggestSection(stringResource(R.string.suggestions_cameras)) {
                            state.matchingCameras.forEach { cam ->
                                SuggestRow(Icons.Filled.Videocam, "${cam.cameraName}  (${cam.cityRegency})") {
                                    fieldText = ""
                                    vm.onQueryChange("")
                                    onCameraClick(cam)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Chips filter ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            FilterDropdown(
                label = state.filters.province ?: stringResource(R.string.filter_province),
                options = state.provinces,
                selected = state.filters.province,
                onSelect = { vm.onFiltersChange(state.filters.copy(province = it, city = null, district = null)) },
            )
            FilterDropdown(
                label = state.filters.city ?: stringResource(R.string.filter_city),
                options = state.cities,
                selected = state.filters.city,
                enabled = state.filters.province != null,
                onSelect = { vm.onFiltersChange(state.filters.copy(city = it, district = null)) },
            )
            FilterDropdown(
                label = state.filters.district ?: stringResource(R.string.filter_district),
                options = state.districts,
                selected = state.filters.district,
                enabled = state.filters.city != null,
                onSelect = { vm.onFiltersChange(state.filters.copy(district = it)) },
            )
            FilterDropdown(
                label = state.filters.status ?: stringResource(R.string.filter_status),
                options = STATUS_OPTIONS,
                selected = state.filters.status,
                onSelect = { vm.onFiltersChange(state.filters.copy(status = it)) },
            )
            FilterDropdown(
                label = state.filters.streamType ?: stringResource(R.string.filter_stream),
                options = STREAM_TYPES,
                selected = state.filters.streamType,
                onSelect = { vm.onFiltersChange(state.filters.copy(streamType = it)) },
            )
            FilterDropdown(
                label = state.filters.operator ?: stringResource(R.string.filter_operator),
                options = state.operators,
                selected = state.filters.operator,
                onSelect = { vm.onFiltersChange(state.filters.copy(operator = it)) },
            )
        }

        // --- Baris hasil + hapus filter ---
        AnimatedVisibility(visible = activeFilters > 0 || state.query.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.search_result_count, state.results.size, activeFilters),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (activeFilters > 0) {
                    TextButton(onClick = { vm.onFiltersChange(SearchFilters()) }) {
                        Text(stringResource(R.string.search_clear_filters))
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (state.results.isEmpty() && isLoading) {
                    items(8) { CameraCardSkeleton() }
                }
                if (state.results.isEmpty() && !isLoading) {
                    item {
                        val hasQuery = state.query.isNotBlank()
                        EmptyState(
                            Icons.Filled.Search,
                            if (hasQuery) stringResource(R.string.search_no_result_title)
                            else stringResource(R.string.search_empty_title),
                            if (hasQuery) stringResource(R.string.search_no_result_hint)
                            else stringResource(R.string.search_empty_hint),
                        )
                    }
                }
                // D3: hasil dikelompokkan per provinsi dengan sticky header
                state.groupedResults.forEach { (province, cams) ->
                    stickyHeader(key = "hdr-$province") {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
                            Text(
                                "$province (${cams.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                            )
                        }
                    }
                    items(cams, key = { it.id }) { CameraCard(it, onCameraClick) }
                }
                if (state.loadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    options: List<String>,
    selected: String?,
    enabled: Boolean = true,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    FilterChip(
        selected = selected != null,
        enabled = enabled,
        onClick = { expanded = !expanded },
        label = { Text(label) },
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        if (selected != null) {
            DropdownMenuItem(text = { Text(stringResource(R.string.filter_all)) }, onClick = {
                expanded = false
                onSelect(null)
            })
        }
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                    expanded = false
                    onSelect(option)
                },
            )
        }
    }
}


@Composable
private fun SuggestSection(title: String, content: @Composable () -> Unit) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
    )
    content()
}

@Composable
private fun SuggestRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}
