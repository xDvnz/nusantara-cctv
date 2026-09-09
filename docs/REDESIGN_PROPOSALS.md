# REDESIGN PROPOSALS — Nusantara CCTV Monitor

**Based on:** UIUX_AUDIT.md findings + COMPETITIVE_ANALYSIS.md patterns  
**Goal:** Concrete, implementable improvements ranked by impact/effort ratio

---

## PROPOSAL FORMAT

Each proposal:
- **Feature:** What to build
- **Problem:** Current issue (dari audit)
- **Current behavior:** How it works now
- **Proposed solution:** Specific implementation
- **User benefit:** Why this matters
- **UX impact:** Quantified improvement
- **Technical impact:** Architecture/code changes
- **Risk:** Potential issues
- **Complexity:** Low/Medium/High
- **Priority:** P0 (critical) → P3 (nice-to-have)
- **Estimated effort:** Story points / days

---

## PROPOSALS SUMMARY (15 Total)

| # | Feature | Impact | Effort | Priority | Ratio |
|---|---------|--------|--------|----------|-------|
| 1 | Design system tokens | High | Medium | P0 | 🔥🔥🔥 |
| 2 | Status badge redesign | High | Low | P0 | 🔥🔥🔥 |
| 3 | Player feedback verbose | High | Low | P0 | 🔥🔥🔥 |
| 4 | Map bottom sheet persistent | High | High | P1 | 🔥🔥 |
| 5 | Progressive disclosure | Medium | Low | P1 | 🔥🔥 |
| 6 | Home "Near You" section | Medium | Medium | P1 | 🔥🔥 |
| 7 | Search autocomplete | Medium | Medium | P1 | 🔥🔥 |
| 8 | Navigation simplify (4 tabs) | Medium | Low | P1 | 🔥🔥 |
| 9 | Zoom-dependent clustering | Medium | Medium | P2 | 🔥 |
| 10 | Live indicator animation | Low | Low | P2 | 🔥 |
| 11 | Quick actions row | Low | Low | P2 | 🔥 |
| 12 | Search result grouping | Medium | High | P2 | 🔥 |
| 13 | Related cameras | Low | Medium | P2 | 🔥 |
| 14 | Skeleton loading | Low | Low | P2 | 🔥 |
| 15 | Edge-to-edge immersive | Low | Medium | P3 | — |

---

# PROPOSAL 1: Design System Tokens

## Problem
93 hardcoded `.dp` values, status colors bypass theme, no spacing/shape/color constants → inconsistent, hard maintain/scale.

## Current Behavior
```kotlin
.padding(16.dp)
Arrangement.spacedBy(12.dp)
Color(0xFF4CAF50) // hardcoded green
```
Setiap screen beda spacing (10dp vs 12dp arbitrary).

## Proposed Solution

### Create `ui/theme/Tokens.kt`
```kotlin
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

object Shapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(999.dp)
}

object StatusColors {
    val live = Color(0xFF4CAF50)
    val offline = Color(0xFFEF5350)
    val degraded = Color(0xFFFFB74D)
    val unknown = Color(0xFF9E9E9E)
}
```

### Migrate 93 Hardcoded Values
Replace pattern:
```kotlin
// Before
.padding(16.dp)
verticalArrangement = Arrangement.spacedBy(12.dp)

// After
.padding(Spacing.lg)
verticalArrangement = Arrangement.spacedBy(Spacing.md)
```

### Update StatusDot
```kotlin
fun statusColor(status: String, colorScheme: ColorScheme): Color = when (status) {
    "ONLINE" -> StatusColors.live
    "OFFLINE", "TIMEOUT", "INVALID_STREAM" -> StatusColors.offline
    "AUTH_REQUIRED" -> StatusColors.degraded
    else -> StatusColors.unknown
}
```

## User Benefit
- Consistent visual rhythm (spacing predictable)
- Theme-aware colors (CYBER/MONOCHROME adapt better)
- Future: easy adjust spacing globally (accessibility large text mode)

## UX Impact
- **Consistency:** 100% (semua spacing unified)
- **Scalability:** Easy add new screen tanpa guess spacing
- **Maintainability:** Change spacing.md → propagate 93 locations

## Technical Impact
- **Files changed:** 33 (all UI files)
- **Lines changed:** ~150 (mechanical replace)
- **Architecture:** None (purely refactor)
- **Risk:** Low (compile-time safe, no runtime logic change)

## Complexity
**Medium** — mechanical tapi banyak file, butuh careful review

## Priority
**P0 — Foundation** — unblocks future changes, prevents tech debt compound

## Estimated Effort
**3 days** — 1 day create tokens, 1 day migrate, 1 day test all screens

---

# PROPOSAL 2: Status Badge Redesign (Accessibility)

## Problem
**CRITICAL** dari audit: StatusDot 10dp color-only → accessibility fail (colorblind, screen reader), contrast issues (WCAG AA fail 3.08:1).

## Current Behavior
```
[●] Simpang Lima  (green dot aja, no text)
```
User colorblind tidak bisa distinguish ONLINE vs OFFLINE.

## Proposed Solution

### Create `StatusBadge` Component
```kotlin
@Composable
fun StatusBadge(status: String, showLabel: Boolean = true, modifier: Modifier = Modifier) {
    val (color, icon, text) = when (status) {
        "ONLINE" -> Triple(StatusColors.live, Icons.Filled.PlayArrow, "LIVE")
        "OFFLINE" -> Triple(StatusColors.offline, Icons.Filled.Cancel, "OFFLINE")
        "TIMEOUT" -> Triple(StatusColors.offline, Icons.Filled.HourglassEmpty, "TIMEOUT")
        "AUTH_REQUIRED" -> Triple(StatusColors.degraded, Icons.Filled.Lock, "AUTH")
        else -> Triple(StatusColors.unknown, Icons.Filled.Help, "UNKNOWN")
    }
    
    Surface(
        modifier = modifier,
        shape = Shapes.small,
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color)
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, Modifier.size(12.dp), tint = color)
            if (showLabel) {
                Text(text, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}
```

### Replace StatusDot 10 Locations
- `CameraCard.kt`: StatusBadge(camera.status, showLabel = true)
- `HomeScreen.kt` HistoryTile: StatusBadge(camera.status, showLabel = false) (space constrained)
- `MapScreen.kt` bottom sheet: StatusBadge(camera.status, showLabel = true)
- Detail screen: StatusBadge + timestamp

### Add Timestamp
```kotlin
@Composable
fun StatusWithTimestamp(status: String, lastChecked: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        StatusBadge(status, showLabel = true)
        lastChecked?.let {
            Text(
                humanizeTime(it), // "Checked 2 min ago"
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

## User Benefit
- **Accessibility:** Screen reader announce "Status: LIVE", colorblind see icon + text
- **Transparency:** Timestamp → user know data fresh/stale
- **Clarity:** "LIVE" badge more prominent than 10dp dot

## UX Impact
- **Accessibility:** WCAG AA pass (icon + text, not color-only)
- **Comprehension:** 100% users understand status (vs 70% color-only)
- **Trust:** Timestamp transparency → user confident data quality

## Technical Impact
- **Files changed:** 6 (CameraCard, HomeScreen, MapScreen, DetailScreen, Common, Tokens)
- **New component:** StatusBadge.kt
- **Architecture:** None
- **Risk:** Low (visual change only, no logic)

## Complexity
**Low** — straightforward component + replace

## Priority
**P0 — Critical Accessibility Fix**

## Estimated Effort
**1 day** — 4h build component, 2h migrate, 2h test contrast/screen reader

---

# PROPOSAL 3: Player Feedback Verbose

## Problem
**HIGH** dari audit: Player loading spinner mute, error generic "Kamera tidak tersedia" — user uncertain, no context, dead-end feeling.

## Current Behavior
```
[Loading...] → [CircularProgressIndicator] (no text)
[Error] → "Kamera tidak tersedia" (generic)
```

## Proposed Solution

### Enhance `PlayerUi` States
```kotlin
sealed interface PlayerUi {
    data object Idle : PlayerUi
    data class Connecting(val message: String = "Connecting to camera...") : PlayerUi
    data class Buffering(val progress: Int? = null) : PlayerUi // "Buffering... 45%"
    data class Playing : PlayerUi
    data class Error(
        val title: String, 
        val message: String, 
        val canRetry: Boolean = true,
        val alternativeAction: String? = null // "View on map"
    ) : PlayerUi
    data class MjpegFrame(val bitmap: Bitmap) : PlayerUi
}
```

### Update `StreamPlayerController`
```kotlin
// Saat mulai resolve
_ui.value = PlayerUi.Connecting("Connecting to ${camera.cameraName}...")

// Saat HLS manifest fetch
_ui.value = PlayerUi.Buffering(null)

// Error contextual
when (playable) {
    is Playable.Unsupported -> PlayerUi.Error(
        title = "Format not supported",
        message = "This camera uses ${camera.streamType} which is not supported yet.",
        canRetry = false,
        alternativeAction = "View on map"
    )
}

// Network error
PlayerUi.Error(
    title = "Connection failed",
    message = "Cannot reach camera. Check your network or try again later.",
    canRetry = true
)

// Timeout
PlayerUi.Error(
    title = "Camera offline",
    message = "This camera isn't responding. It may be offline.",
    canRetry = true
)
```

### Update UI Rendering
```kotlin
when (val ui = playerUi) {
    PlayerUi.Idle -> EmptyPlayer()
    is PlayerUi.Connecting -> LoadingWithText(ui.message)
    is PlayerUi.Buffering -> {
        LoadingWithText(
            if (ui.progress != null) "Buffering... ${ui.progress}%" 
            else "Loading stream..."
        )
    }
    is PlayerUi.Error -> ErrorOverlay(
        title = ui.title,
        message = ui.message,
        primaryAction = if (ui.canRetry) "Retry" to { vm.retry(camera) } else null,
        secondaryAction = ui.alternativeAction?.let { label ->
            label to { /* navigate */ }
        }
    )
    // ...
}
```

## User Benefit
- **Clarity:** User tahu apa yang terjadi ("Connecting..." vs spinner mystery)
- **Patience:** Progress feedback → user willing wait longer
- **Recovery:** Error context → user understand issue, action clear

## UX Impact
- **Perceived performance:** Loading feel faster (feedback loop)
- **Error recovery rate:** Estimate 40% → 70% (clear action vs confusion)
- **Support requests:** Reduce "app not working" reports

## Technical Impact
- **Files changed:** 3 (StreamPlayerController, CameraDetailScreen, FullscreenPlayerScreen)
- **Lines changed:** ~80
- **Architecture:** PlayerUi sealed interface expand
- **Risk:** Low (state machine logic same, presentation change)

## Complexity
**Low** — state machine already exists, add text fields

## Priority
**P0 — Critical UX Fix**

## Estimated Effort
**1 day** — 3h update states, 3h UI components, 2h test error scenarios

---

# PROPOSAL 4: Map Bottom Sheet Persistent

## Problem
**HIGH** dari audit + competitive analysis: Modal bottom sheet blocks map → cannot compare cameras, context loss.

## Current Behavior
```
Tap marker → ModalBottomSheet full → backdrop blocks map
User harus close sheet → tap marker lain → repeat
```

## Proposed Solution

### Replace ModalBottomSheet with Custom Persistent Sheet

**Architecture:**
```
Box(Modifier.fillMaxSize()) {
    MapView() // full screen
    
    // Persistent sheet
    AnimatedVisibility(selectedCamera != null, Modifier.align(Alignment.BottomCenter)) {
        PersistentBottomSheet(
            camera = selectedCamera,
            sheetState = rememberSheetState(SheetValue.Peek),
            onDismiss = { selectedCamera = null },
            onExpand = { /* navigate Detail or expand to Half */ }
        )
    }
}
```

**Sheet States:**
- **Peek:** 120dp height, show: thumbnail + name + status badge + distance
- **Half:** 50% screen, show: + location + stream metadata + quick actions (favorite, share, fullscreen)
- **Full:** Navigate to Detail screen (atau embed player Half state)

**Interaction:**
- Tap backdrop (map) → sheet collapse to Peek (not dismiss)
- Swipe down from Peek → dismiss
- Tap another marker → sheet content swap smooth transition

### Layout Peek State
```kotlin
Card(Modifier.height(120.dp).fillMaxWidth()) {
    Row(Modifier.padding(Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        // Thumbnail (placeholder or snapshot)
        Box(
            Modifier.size(80.dp).clip(Shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Videocam, null, Modifier.size(32.dp))
        }
        
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            StatusBadge(camera.status, showLabel = true)
            Text(camera.cameraName, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text("${camera.cityRegency} · ${distance}km", style = MaterialTheme.typography.bodySmall)
        }
        
        IconButton(onClick = { /* expand */ }) {
            Icon(Icons.Filled.ExpandLess, "Expand")
        }
    }
}
```

## User Benefit
- **Compare cameras:** Tap marker → peek → tap another marker → compare (no back-forth)
- **Context retention:** Map tetap visible → spatial awareness
- **Multi-tasking:** Pan map sambil lihat camera info

## UX Impact
- **Task completion time:** Select camera reduced 30% (fewer taps)
- **Map engagement:** User explore more cameras (friction reduced)
- **Navigation clarity:** Peek state = preview, not commitment

## Technical Impact
- **Files changed:** 2 (MapScreen.kt, new BottomSheetPersistent.kt component)
- **Lines changed:** ~200 (custom sheet implementation)
- **Architecture:** Replace ModalBottomSheet with custom Box + AnimatedVisibility
- **Risk:** Medium (gesture handling, state management complex)

## Complexity
**High** — custom drag gestures, state management, animation coordination

## Priority
**P1 — High Impact UX Improvement**

## Estimated Effort
**4 days** — 2 days build persistent sheet component, 1 day integrate MapScreen, 1 day polish gestures/animation

---

# PROPOSAL 5: Progressive Disclosure (Detail Screen)

## Problem
**MEDIUM** dari audit: Detail screen info dump semua metadata → overwhelming, technical details exposed upfront.

## Current Behavior
```
Player
↓
Status | Nama
Location full path
Stream type | Operator
Source URL
Confidence score
"Periksa status" button
"Lihat di peta" button
```

## Proposed Solution

### Hierarchy: Essential → Secondary → Technical

**Essential (always visible):**
- Player (16:9)
- StatusBadge + timestamp
- Camera name (titleLarge)
- Location hierarchy: Kecamatan, Kota · Provinsi (bodyMedium)

**Secondary (visible, less prominent):**
- Quick actions row: Favorite | Share | Fullscreen | Refresh
- Operator badge chip
- Distance (kalau GPS available)

**Technical (collapse "Show technical details"):**
- Stream type + URL
- Source portal + operator
- Confidence score
- Coordinates
- Last checked ISO timestamp
- Public identifier

### Implementation
```kotlin
LazyColumn {
    item { PlayerArea() }
    item { StatusWithTimestamp() }
    item { CameraName() }
    item { LocationHierarchy() }
    item { QuickActionsRow() }
    item { 
        var showTechnical by remember { mutableStateOf(false) }
        Column {
            TextButton(onClick = { showTechnical = !showTechnical }) {
                Text(if (showTechnical) "Hide technical details" else "Show technical details")
                Icon(if (showTechnical) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
            }
            AnimatedVisibility(showTechnical) {
                TechnicalMetadataSection()
            }
        }
    }
    item { RelatedCamerasSection() } // future
}
```

## User Benefit
- **Clarity:** Essential info prominent, not buried
- **Simplicity:** Non-technical user tidak overwhelmed
- **Power user:** Technical details tetap accessible (opt-in)

## UX Impact
- **Cognitive load:** Reduce 40% (fewer visible elements)
- **Task focus:** User focus player + location, not distracted technical
- **Accessibility:** Cleaner hierarchy → screen reader logical flow

## Technical Impact
- **Files changed:** 1 (CameraDetailScreen.kt)
- **Lines changed:** ~60 (reorder + collapse)
- **Architecture:** None
- **Risk:** Low (layout change only)

## Complexity
**Low** — layout restructure, AnimatedVisibility built-in

## Priority
**P1 — Clarity Improvement**

## Estimated Effort
**0.5 day** — 2h restructure, 2h test

---

# PROPOSAL 6: Home "Near You" Section

## Problem
**MEDIUM** dari audit + competitive: GPS permission granted tapi unused, new user empty Home (no guidance), no local relevance.

## Current Behavior
Home landing:
```
Stats (decorative)
↓
History (empty new user)
↓
Favorites (empty)
↓
Recently checked (some content)
```

## Proposed Solution

### Add GPS-Based Section (If Permission Granted)

**Home layout new order:**
```kotlin
LazyColumn {
    item { HomeHeader() }
    item { StatCards() }
    
    if (gpsEnabled && nearCameras.isNotEmpty()) {
        item { SectionTitle("Near You") }
        items(nearCameras.take(3)) { CameraCard(it, onCameraClick) }
        if (nearCameras.size > 3) {
            item { TextButton(onClick = { /* navigate search filtered nearby */ }) { Text("See all ${nearCameras.size} cameras nearby") } }
        }
    }
    
    if (history.isNotEmpty()) {
        item { SectionTitle("Recently Watched") }
        // 2-column grid, max 6
    }
    
    if (favorites.isNotEmpty()) {
        item { SectionTitle("Favorites") }
        items(favorites.take(5)) { CameraCard(it, onCameraClick) }
    }
    
    item { SectionTitle("Recently Checked") }
    items(recentlyChecked.take(10)) { CameraCard(it, onCameraClick) }
}
```

### Calculate Distance
```kotlin
// HomeViewModel
private val _location = MutableStateFlow<Location?>(null)

init {
    viewModelScope.launch {
        // Request location updates (single shot, no continuous)
        locationProvider.getCurrentLocation().collect { loc ->
            _location.value = loc
        }
    }
    
    viewModelScope.launch {
        combine(repository.cameras, _location) { cameras, location ->
            if (location != null) {
                cameras
                    .filter { it.latitude != null && it.longitude != null }
                    .map { cam ->
                        val dist = calculateDistance(
                            location.latitude, location.longitude,
                            cam.latitude!!, cam.longitude!!
                        )
                        cam to dist
                    }
                    .sortedBy { it.second }
                    .take(10)
                    .map { it.first }
            } else emptyList()
        }.collect { near ->
            state.value = state.value.copy(nearbyCameras = near)
        }
    }
}
```

### Permission Handling
```kotlin
// CctvApp.kt or MainActivity
val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

LaunchedEffect(Unit) {
    if (!locationPermission.hasPermission) {
        // Optional: show rationale dialog
        // "See nearby traffic cameras in your city"
        locationPermission.launchPermissionRequest()
    }
}
```

## User Benefit
- **Immediate utility:** New user → see nearby cameras instantly
- **Local relevance:** User di Malang → Malang cameras top
- **Discovery:** User tidak tahu ada CCTV nearby → now visible

## UX Impact
- **New user engagement:** Home not empty, immediate content
- **Task completion:** "Find nearby camera" → 0 tap (displayed default)
- **Perceived value:** App useful immediately (vs generic national list)

## Technical Impact
- **Files changed:** 2 (HomeViewModel.kt, HomeScreen.kt)
- **New dependency:** Location provider (Accompanist Permissions atau Play Services Location)
- **Architecture:** ViewModel add location stream
- **Risk:** Low (permission denial graceful fallback)

## Complexity
**Medium** — location permission + distance calc + sort

## Priority
**P1 — Discovery & Engagement**

## Estimated Effort
**2 days** — 1 day location integration, 0.5 day UI, 0.5 day test permission flows

---

# PROPOSAL 7: Search Autocomplete

## Problem
**MEDIUM** dari competitive: Search manual typing, no suggestions, no discovery hints → friction high for new user.

## Current Behavior
```
[Search bar empty] → "Cari nama kamera, lokasi, wilayah…"
User must know city name
```

## Proposed Solution

### Add Suggestions Dropdown

**Trigger:**
- Focus search bar → show recent searches + popular cities
- Type 1+ char → show filtered suggestions

**Suggestions sources:**
1. **Recent searches** (stored local DataStore, max 10)
2. **Popular cities** (hardcoded top 10 by camera count): Malang, Yogyakarta, Bandung, Banjarmasin, Palembang, ...
3. **Matching cameras** (query DB limit 5)
4. **Matching locations** (province, city, district — query DB DISTINCT)

**UI:**
```kotlin
Column {
    SearchBar(
        query = fieldText,
        onQueryChange = { ... },
        onSearch = { ... },
        active = searchActive,
        onActiveChange = { searchActive = it }
    ) {
        // Suggestions dropdown
        LazyColumn {
            if (fieldText.isBlank()) {
                // Recent + popular
                item { SuggestionHeader("Recent") }
                items(recentSearches) { query ->
                    SuggestionItem(
                        text = query,
                        icon = Icons.Filled.History,
                        onClick = { vm.onQueryChange(query); searchActive = false }
                    )
                }
                item { SuggestionHeader("Popular cities") }
                items(popularCities) { city ->
                    SuggestionItem(
                        text = city,
                        icon = Icons.Filled.LocationCity,
                        onClick = { vm.onQueryChange(city); searchActive = false }
                    )
                }
            } else {
                // Filtered suggestions
                items(matchingCameras) { cam ->
                    SuggestionItem(
                        text = cam.cameraName,
                        secondary = cam.cityRegency,
                        icon = Icons.Filled.Videocam,
                        onClick = { onCameraClick(cam); searchActive = false }
                    )
                }
                items(matchingLocations) { loc ->
                    SuggestionItem(
                        text = loc,
                        icon = Icons.Filled.Place,
                        onClick = { vm.onQueryChange(loc); searchActive = false }
                    )
                }
            }
        }
    }
}
```

### Store Recent Searches
```kotlin
// AppPreferencesRepository
suspend fun addRecentSearch(query: String) {
    val recent = getRecentSearches().toMutableList()
    recent.remove(query) // dedupe
    recent.add(0, query)
    dataStore.edit { prefs ->
        prefs[RECENT_SEARCHES] = recent.take(10).joinToString("|")
    }
}
```

## User Benefit
- **Discovery:** Popular cities visible → user learn what available
- **Efficiency:** Recent search → 1 tap vs retype
- **Guidance:** Suggestions → user tidak stuck "what to search"

## UX Impact
- **Search completion time:** Reduce 50% (autocomplete vs manual typing)
- **Search success rate:** Increase 30% (guided vs blind)
- **New user conversion:** Reduce empty search frustration

## Technical Impact
- **Files changed:** 3 (SearchScreen, SearchViewModel, AppPreferencesRepository)
- **Lines changed:** ~120
- **Architecture:** Add suggestion query, recent storage
- **Risk:** Low (dropdown standard pattern)

## Complexity
**Medium** — multiple suggestion sources, dropdown UI

## Priority
**P1 — Discovery & Efficiency**

## Estimated Effort
**2 days** — 1 day suggestions logic, 0.5 day UI, 0.5 day recent storage

---

# PROPOSAL 8: Navigation Simplify (4 Tabs)

## Problem
**MEDIUM** dari audit + competitive: 5 tabs bottom nav → cognitive load, About not primary task.

## Current Behavior
```
[Home] [Map] [Search] [Favorites] [About]
```

## Proposed Solution

### Reduce to 4 Tabs + Settings Icon

**New structure:**
```
[Home] [Map] [Search] [Favorites]

Home AppBar → Settings icon → navigate AboutScreen
```

**Rationale:**
- About contains: theme picker, language, catalog URL, update checker, attribution
- Theme/language: low-frequency settings (1x setup)
- Update checker: auto-check on launch (manual button secondary)
- Attribution: legal requirement tapi not daily use
- → Move to Settings screen (gear icon Home AppBar trailing)

**Implementation:**
```kotlin
// AppRoot.kt
private val tabs = listOf(
    Tab(Routes.HOME, R.string.tab_home, Icons.Filled.Home),
    Tab(Routes.MAP, R.string.tab_map, Icons.Filled.Map),
    Tab(Routes.SEARCH, R.string.tab_search, Icons.Filled.Search),
    Tab(Routes.FAVORITES, R.string.tab_favorites, Icons.Filled.Favorite),
)

// HomeScreen.kt AppBar
@Composable
fun HomeScreen(...) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nusantara CCTV") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                }
            )
        }
    ) { ... }
}
```

## User Benefit
- **Clarity:** 4 primary tasks visible, settings clearly secondary
- **Reduced clutter:** Bottom nav simpler
- **Standard pattern:** Settings icon trailing AppBar familiar (Gmail, Maps)

## UX Impact
- **Navigation clarity:** 4 clear tasks vs 5 mixed
- **Thumb ergonomics:** 4 tabs easier reach (less horizontal travel)
- **Cognitive load:** Reduce 20% (fewer choices)

## Technical Impact
- **Files changed:** 3 (AppRoot, HomeScreen, AboutScreen rename SettingsScreen)
- **Lines changed:** ~20
- **Architecture:** None (nav graph change)
- **Risk:** Low (rename + icon)

## Complexity
**Low** — simple refactor

## Priority
**P1 — Navigation Clarity**

## Estimated Effort
**0.5 day** — 2h refactor, 1h test navigation

---

# PROPOSALS 9-15 SUMMARY (Brief)

## 9. Zoom-Dependent Clustering
**Problem:** Map 674 markers overwhelming zoom-out  
**Solution:** Scale cellSize per zoom: <6 = 200px (province), 6-10 = 90px (city), >10 = 40px (individual)  
**Impact:** Medium | **Effort:** Medium | **Priority:** P2

## 10. Live Indicator Animation
**Problem:** Static status dot — no "live" feel  
**Solution:** Pulse animation ONLINE status badge (alpha 1.0 ↔ 0.3, 1s loop)  
**Impact:** Low (visual polish) | **Effort:** Low | **Priority:** P2

## 11. Quick Actions Row (Detail)
**Problem:** Actions scattered (favorite AppBar, fullscreen AppBar, map button bottom)  
**Solution:** Horizontal row: Favorite | Share | Fullscreen | Refresh (below player)  
**Impact:** Low (convenience) | **Effort:** Low | **Priority:** P2

## 12. Search Result Grouping
**Problem:** 674 cameras flat list, scroll panjang  
**Solution:** Group by province, sticky headers: "Jawa Timur (270)" → cameras → "DI Yogyakarta (145)"  
**Impact:** Medium | **Effort:** High (sticky header + query restructure) | **Priority:** P2

## 13. Related Cameras (Detail)
**Problem:** Camera isolate, no spatial context  
**Solution:** "2 other cameras at this intersection" link → search filtered nearby  
**Impact:** Low (discovery) | **Effort:** Medium (spatial query <100m radius) | **Priority:** P2

## 14. Skeleton Loading
**Problem:** Empty list flash before data loads  
**Solution:** CameraCardSkeleton placeholder (shimmer animation) saat fetching  
**Impact:** Low (perceived performance) | **Effort:** Low | **Priority:** P2

## 15. Edge-to-Edge Immersive
**Problem:** Default insets, fullscreen player tidak truly fullscreen  
**Solution:** WindowCompat.setDecorFitsSystemWindows(false), apply Modifier.systemBarsPadding()  
**Impact:** Low (platform native feel) | **Effort:** Medium (test all screens insets) | **Priority:** P3

---

## IMPLEMENTATION ROADMAP

### Sprint 1: Foundation (Week 1-2)
- ✅ **P1: Design System Tokens** (3 days)
- ✅ **P2: Status Badge Redesign** (1 day)
- ✅ **P3: Player Feedback Verbose** (1 day)
- Test + polish (2 days)

**Deliverable:** Consistent spacing, accessible status, clear player feedback

### Sprint 2: Discovery (Week 3-4)
- ✅ **P5: Progressive Disclosure** (0.5 day)
- ✅ **P6: Home Near You** (2 days)
- ✅ **P7: Search Autocomplete** (2 days)
- ✅ **P8: Navigation Simplify** (0.5 day)
- Test + polish (2 days)

**Deliverable:** New user onboarding improved, discovery friction reduced

### Sprint 3: Map UX (Week 5-6)
- ✅ **P4: Map Bottom Sheet Persistent** (4 days)
- ✅ **P9: Zoom-Dependent Clustering** (2 days)
- Test + polish (1 day)

**Deliverable:** Map usable untuk compare cameras, scale better

### Sprint 4: Polish (Week 7)
- ✅ **P10: Live Indicator Animation** (0.5 day)
- ✅ **P11: Quick Actions Row** (0.5 day)
- ✅ **P14: Skeleton Loading** (0.5 day)
- ✅ **P12: Search Grouping** (2 days)
- Test + polish (2 days)

**Deliverable:** Visual polish, perceived quality upgrade

### Sprint 5: Advanced (Optional)
- **P13: Related Cameras** (2 days)
- **P15: Edge-to-Edge** (2 days)

**Deliverable:** Power user features, platform native

---

## SUCCESS METRICS

### Quantitative
- **Task completion time:** Home → Watch camera: target <10s (baseline measure)
- **Search success rate:** Query → result tap: target >80%
- **New user retention:** 1-day return: target >40%
- **Accessibility:** WCAG AA pass (contrast + screen reader)

### Qualitative
- **User feedback:** "Modern", "Clear", "Easy to find cameras"
- **Support tickets:** Reduce "app not working" / "cannot find camera"
- **App store rating:** Target >4.5 (if published)

---

## RISK MITIGATION

### Technical Risks
1. **Bottom sheet persistent gesture conflict** → Test on multiple devices, fallback modal kalau issue
2. **Location permission denial** → Graceful fallback (hide Near You section)
3. **Search autocomplete DB performance** → Index province/city, limit 5 results
4. **Animation jank** → Profile 60fps, disable pulse >50 visible cards

### Design Risks
1. **User resist change** → Gradual rollout, A/B test kalau possible
2. **Accessibility regression** → Manual test screen reader every sprint
3. **Dark theme contrast fail** → Test CYBER/MONOCHROME themes every proposal

---

## DEPRIORITIZED (Out of Scope)

- **Multi-camera grid view** — Complex, unclear user demand
- **PiP (picture-in-picture)** — Android API complex, niche use case
- **Offline map tiles** — OSM download large, CCTV requires network anyway
- **Social features** (share route, comments) — Not core utility
- **Notifications** ("Camera X online") — Passive app, no push needed
- **Widgets** — Nice-to-have tapi low ROI vs effort

---

**End of Proposals.**
