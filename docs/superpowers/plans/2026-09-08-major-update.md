# Nusantara CCTV Monitor Major Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver an accessible Material You and tactical Cyber redesign, reliable official catalog synchronization, verified Indonesian public CCTV expansion, and physical-device validation.

**Architecture:** Keep existing native Kotlin/Compose, manual `AppContainer`, Room, Media3, osmdroid, and Python catalog pipeline. Refactor only theme tokens, reusable UI pieces, screen composition, catalog URL policy, and bounded error paths. Source expansion stays pipeline-first: no device-side portal scraping and no unverified feeds in the shipped catalog.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, DataStore, Room 2.6.1, OkHttp, Media3, osmdroid, Python 3.11 requests, Android 10+ (minSdk 26).

## Global Constraints

- `AGENTS.md` is local-only: never stage, commit, push, replace, or delete it.
- Retain official public-government sources only. No authentication bypass, private feeds, credentials, WebView, recording, mirroring, or re-encoding.
- Validate each new stream as manifest `#EXTM3U` plus one actual TS/fMP4 segment before catalog inclusion.
- Material `ColorScheme` supplies screen foreground and background colors. Verify custom Cyber and Monochrome pairs at WCAG AA.
- Every data UI has populated, loading, empty, and error states. Every control has a real action and a 48dp touch target.
- Motion is 150-220 ms, explains state changes, respects Android animator duration scale, and uses no endless animation.
- Preserve favorites/history on ordinary database upgrades. Never introduce `fallbackToDestructiveMigration()` for upgrades.
- Default catalog URL is `https://raw.githubusercontent.com/xDvnz/nusantara-cctv/main/data/cameras.json`; alternate catalog URL is HTTPS-only and schema/version-gated.
- Use connected physical ADB device `UFKRK19A01000720`; do not launch an emulator.
- Keep dependency list unchanged unless a current dependency cannot implement a required behavior.

---

## File Structure

| Path | Responsibility |
|---|---|
| `android/app/src/main/kotlin/id/nusantara/cctv/data/prefs/AppPreferences.kt` | Persist official/alternate catalog selection and existing appearance/map preferences. |
| `android/app/src/main/kotlin/id/nusantara/cctv/data/catalog/CatalogRepository.kt` | Canonical catalog endpoint policy, URL validation, transactional remote sync, bounded refresh result. |
| `android/app/src/main/kotlin/id/nusantara/cctv/CctvApp.kt` | Wire default endpoint and startup catalog refresh safely. |
| `android/app/src/main/kotlin/id/nusantara/cctv/ui/theme/Theme.kt` | Complete, contrast-safe Material schemes plus system motion scale. |
| `android/app/src/main/kotlin/id/nusantara/cctv/data/prefs/ThemePalette.kt` | Cyber/Monochrome semantic palette values. |
| `android/app/src/main/kotlin/id/nusantara/cctv/ui/components/*` | Shared press, state, feedback, and tactical status primitives. |
| `android/app/src/main/kotlin/id/nusantara/cctv/ui/AppRoot.kt` | Theme-aware navigation transition and startup sync feedback. |
| `android/app/src/main/kotlin/id/nusantara/cctv/ui/{home,search,favorites}/*` | Refresh workflow, readable screen hierarchy, correctly named data states. |
| `android/app/src/main/kotlin/id/nusantara/cctv/ui/map/{MapScreen,MapLayers}.kt` | Persisted layer sheet and camera metadata bottom panel. |
| `android/app/src/main/kotlin/id/nusantara/cctv/ui/about/{AboutScreen,AboutViewModel}.kt` | Uniform About cards, official automatic catalog, advanced alternate source. |
| `android/app/src/main/kotlin/id/nusantara/cctv/data/{api/SourceHttp,player/StreamEngine,player/StreamPlayerController}.kt` | Bounded transient retry, source-safe error mapping and user retry. |
| `tools/discovery/*.py`, `tools/discovery/run_all.py`, `tools/validation/validate.py`, `tools/import_export/export.py` | Official feed discovery, validation metadata, normalized catalog export. |
| `android/app/src/test/kotlin/id/nusantara/cctv/*.kt` | Regression coverage for catalog policy, Room schema/data, palette and source pipeline invariants. |
| `android/app/src/main/res/values/strings.xml`, `android/app/src/main/res/values-en/strings.xml` | Matching Indonesian and English text for every new user-visible string. |

## Task 1: Catalog endpoint policy

**Files:**
- Modify: `android/app/build.gradle.kts:21-24`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/data/prefs/AppPreferences.kt:29-74`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/data/catalog/CatalogRepository.kt:27-42, 122-123, 196-227`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/CctvApp.kt:17-21, 67-69`
- Create: `android/app/src/test/kotlin/id/nusantara/cctv/CatalogEndpointPolicyTest.kt`

**Interfaces:**
- Produces `CatalogRepository.OFFICIAL_CATALOG_URL: String`.
- Produces `CatalogRepository.setAlternateCatalogUrl(url: String?): CatalogUrlResult`.
- Produces `CatalogRepository.resetCatalogUrl(): Unit`.
- Produces `CatalogRepository.activeCatalogUrl(): String`.
- Produces `sealed interface CatalogUrlResult { data object Accepted; data class Rejected(val reason: CatalogUrlError) }` and `enum class CatalogUrlError { NON_HTTPS, INVALID_URL }`.
- `syncFromRemote()` always reads the effective official-or-alternate URL.

- [ ] **Step 1: Write failing endpoint-policy tests**

```kotlin
@Test fun `official url is used when no alternate is configured`() {
    assertEquals(
        "https://raw.githubusercontent.com/xDvnz/nusantara-cctv/main/data/cameras.json",
        repository.activeCatalogUrl(),
    )
}

@Test fun `reject alternate catalog url without https`() {
    assertEquals(
        CatalogUrlResult.Rejected(CatalogUrlError.NON_HTTPS),
        repository.setAlternateCatalogUrl("http://example.test/cameras.json"),
    )
    assertEquals(CatalogRepository.OFFICIAL_CATALOG_URL, repository.activeCatalogUrl())
}

@Test fun `reset restores official catalog url`() {
    assertEquals(CatalogUrlResult.Accepted, repository.setAlternateCatalogUrl("https://example.test/cameras.json"))
    repository.resetCatalogUrl()
    assertEquals(CatalogRepository.OFFICIAL_CATALOG_URL, repository.activeCatalogUrl())
}
```

- [ ] **Step 2: Run endpoint-policy tests and confirm compilation fails**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.CatalogEndpointPolicyTest`

Expected: FAIL because endpoint policy API does not exist.

- [ ] **Step 3: Define endpoint constant and effective selection**

```kotlin
companion object {
    const val OFFICIAL_CATALOG_URL =
        "https://raw.githubusercontent.com/xDvnz/nusantara-cctv/main/data/cameras.json"
}

private var alternateCatalogUrl: String? = initialAlternateUrl?.takeIf(String::isNotBlank)

fun activeCatalogUrl(): String = alternateCatalogUrl ?: OFFICIAL_CATALOG_URL
```

Replace blank `REMOTE_CATALOG_URL` build config with the same public URL only if build config must remain a visible deploy override. Do not permit a build override to disable the official default.

- [ ] **Step 4: Validate alternate URL before persisting**

```kotlin
fun setAlternateCatalogUrl(url: String?): CatalogUrlResult {
    val value = url?.trim().orEmpty()
    if (value.isEmpty()) {
        alternateCatalogUrl = null
        return CatalogUrlResult.Accepted
    }
    val parsed = value.toHttpUrlOrNull() ?: return CatalogUrlResult.Rejected(CatalogUrlError.INVALID_URL)
    if (!parsed.isHttps) return CatalogUrlResult.Rejected(CatalogUrlError.NON_HTTPS)
    alternateCatalogUrl = parsed.toString()
    return CatalogUrlResult.Accepted
}
```

Make DataStore field explicitly mean alternate URL. Preserve migration by retaining the existing preference key `remote_catalog_url` or reading it once as the alternate value.

- [ ] **Step 5: Use active URL in sync and preserve local catalog on failure**

```kotlin
val request = Request.Builder().url(activeCatalogUrl()).build()
```

Keep `CatalogSyncException`, JSON parsing, version comparison, and `db.withTransaction` behavior. Invalid payload, non-incrementing version, network failure, or server error must not mutate camera/source tables.

- [ ] **Step 6: Run catalog unit tests**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.CatalogEndpointPolicyTest --tests id.nusantara.cctv.CatalogDtoParsingTest`

Expected: PASS.

- [ ] **Step 7: Commit catalog policy**

```bash
git add android/app/build.gradle.kts android/app/src/main/kotlin/id/nusantara/cctv/data/prefs/AppPreferences.kt android/app/src/main/kotlin/id/nusantara/cctv/data/catalog/CatalogRepository.kt android/app/src/main/kotlin/id/nusantara/cctv/CctvApp.kt android/app/src/test/kotlin/id/nusantara/cctv/CatalogEndpointPolicyTest.kt
git commit -m "feat: sync official catalog by default"
```

## Task 2: Theme tokens, accessibility, and motion

**Files:**
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/data/prefs/ThemePalette.kt:5-68`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/theme/Theme.kt:17-93`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/AppRoot.kt:69-170`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/components/PressScale.kt`
- Create: `android/app/src/test/kotlin/id/nusantara/cctv/ThemePaletteTest.kt`

**Interfaces:**
- Produces `NusantaraTheme(themeMode: ThemeMode, content: @Composable () -> Unit)` with complete Material semantic token sets.
- Produces `appMotionSpec(): AppMotionSpec` with `enterMillis`, `exitMillis`, and disabled-animation behavior.
- All screen UI uses `MaterialTheme.colorScheme` for foreground/background text/icon coloring.

- [ ] **Step 1: Write failing palette assertions**

```kotlin
@Test fun `cyber foreground colors pass AA on their assigned surfaces`() {
    assertTrue(contrast(PresetPalettes.CYBER.onBackground, PresetPalettes.CYBER.background) >= 4.5)
    assertTrue(contrast(PresetPalettes.CYBER.onSurface, PresetPalettes.CYBER.surface) >= 4.5)
    assertTrue(contrast(PresetPalettes.CYBER.onSurfaceVariant, PresetPalettes.CYBER.surfaceVariant) >= 4.5)
}

@Test fun `monochrome foreground colors pass AA on assigned surfaces`() {
    assertTrue(contrast(PresetPalettes.MONOCHROME.onBackground, PresetPalettes.MONOCHROME.background) >= 4.5)
    assertTrue(contrast(PresetPalettes.MONOCHROME.onSurface, PresetPalettes.MONOCHROME.surface) >= 4.5)
}
```

Implement `contrast(foreground: Long, background: Long): Double` in test using WCAG relative luminance. Do not introduce a runtime color utility merely for these static values.

- [ ] **Step 2: Run palette tests and confirm current palette failures, if any**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.ThemePaletteTest`

Expected: FAIL only for pairs below 4.5:1; otherwise add exact requested coverage and confirm PASS before changing palette.

- [ ] **Step 3: Complete semantic palette mapping**

Extend `ThemePalette` only with `onSecondary`, `onTertiary`, `surfaceContainerHighest`, and their foreground companions if current Material components need them. In `paletteScheme`, map every supplied semantic token and derive untouched fields from `DarkScheme` or `LightScheme`. Keep Cyber matte navy/teal, amber warning, red error. Remove purple tertiary from Cyber unless a real UI state needs it.

- [ ] **Step 4: Centralize animated navigation duration**

```kotlin
private const val ENTER_DURATION_MS = 220
private const val EXIT_DURATION_MS = 160
```

Use `LocalViewConfiguration.current.longPressTimeoutMillis` only if actual system animator duration scale cannot be read reliably with current APIs. Otherwise preserve Compose default behavior when system animations are disabled. Do not animate width/height or continuously animate status indicators.

- [ ] **Step 5: Ensure shared press interaction is semantic and bounded**

```kotlin
fun Modifier.pressScale(enabled: Boolean = true): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (enabled && pressed) 0.98f else 1f, label = "pressScale")
    graphicsLayer { scaleX = scale; scaleY = scale }
        .indication(interactionSource, rememberRipple())
}
```

Apply it only to existing card/button surfaces that already have click behavior. Do not create decorative non-functional controls.

- [ ] **Step 6: Run palette and UI unit tests**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.ThemePaletteTest`

Expected: PASS.

- [ ] **Step 7: Commit theme foundation**

```bash
git add android/app/src/main/kotlin/id/nusantara/cctv/data/prefs/ThemePalette.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/theme/Theme.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/AppRoot.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/components/PressScale.kt android/app/src/test/kotlin/id/nusantara/cctv/ThemePaletteTest.kt
git commit -m "feat: strengthen adaptive theme system"
```

## Task 3: Official catalog UX and consistent About layout

**Files:**
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/about/AboutViewModel.kt:20-111`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/about/AboutScreen.kt:43-292`
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `android/app/src/main/res/values-en/strings.xml`
- Test: `android/app/src/test/kotlin/id/nusantara/cctv/CatalogEndpointPolicyTest.kt`

**Interfaces:**
- `AboutViewModel.saveAlternateAndSync(): Unit` validates and syncs alternate endpoint.
- `AboutViewModel.resetToOfficialAndSync(): Unit` restores official endpoint and syncs it.
- `AboutUiState.usingOfficialCatalog: Boolean` informs user without exposing an editable default URL.

- [ ] **Step 1: Write failing ViewModel policy test**

```kotlin
@Test fun `empty alternate URL uses official endpoint`() = runTest {
    viewModel.onCatalogUrlChange("")
    viewModel.saveAlternateAndSync()
    assertTrue(viewModel.state.value.usingOfficialCatalog)
    assertEquals(CatalogRepository.OFFICIAL_CATALOG_URL, repository.activeCatalogUrl())
}
```

Use a fake repository adapter only if current class cannot be constructed with an in-memory Room database and MockWebServer-free fake transport. Do not add a mocking dependency.

- [ ] **Step 2: Run test and confirm it fails**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.CatalogEndpointPolicyTest`

Expected: FAIL because `usingOfficialCatalog` and reset behavior do not exist.

- [ ] **Step 3: Rework ViewModel state transitions**

- Read persisted alternate URL on startup.
- Treat a blank field as official source, never a disabled sync.
- Refuse insecure/invalid alternate URLs before persisting.
- Set `syncing` false, `isError` true, and a localized specific message for invalid URL, HTTP failure, invalid schema, and old version.
- Reset clears DataStore alternate endpoint, calls `repository.resetCatalogUrl()`, then syncs official endpoint.

- [ ] **Step 4: Recompose About catalog section**

Use one `AboutCard` for catalog summary, last catalog metadata, advanced alternate URL field, save/sync action, reset-to-official action, and message. Keep initial view compact: show official-source status and a button to reveal advanced override. Retain a real expandable Sources and attribution section. Use `TextField`/`Button` content descriptions and explicit labels.

- [ ] **Step 5: Normalize all About section cards**

Keep `AboutCard` as sole card primitive. Ensure Appearance and Updates use its padding, width, surface, vertical gap, text hierarchy, and enabled/loading behavior. No custom box wrapping those sections.

- [ ] **Step 6: Add matching Indonesian and English strings**

Add each new string key in both resource files in same commit. Values include official catalog status, alternate catalog label/hint, reset action, invalid HTTPS message, and advanced override reveal/hide label. Avoid unsupported claims and em dashes.

- [ ] **Step 7: Run focused tests**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.CatalogEndpointPolicyTest`

Expected: PASS.

- [ ] **Step 8: Commit About/catalog UX**

```bash
git add android/app/src/main/kotlin/id/nusantara/cctv/ui/about/AboutViewModel.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/about/AboutScreen.kt android/app/src/main/res/values/strings.xml android/app/src/main/res/values-en/strings.xml android/app/src/test/kotlin/id/nusantara/cctv/CatalogEndpointPolicyTest.kt
git commit -m "feat: clarify catalog sync settings"
```

## Task 4: Refresh results and Search redesign

**Files:**
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/data/catalog/CatalogRepository.kt:178-194`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/home/HomeViewModel.kt:11-80`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/home/HomeScreen.kt:38-107`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/favorites/FavoritesScreen.kt:39-103`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/search/SearchViewModel.kt:24-145`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/search/SearchScreen.kt:63-297`
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `android/app/src/main/res/values-en/strings.xml`
- Test: `android/app/src/test/kotlin/id/nusantara/cctv/CctvDatabaseTest.kt`

**Interfaces:**
- Produces `data class RefreshResult(val catalog: CatalogRepository.SyncResult?, val probed: Int, val changed: Int, val error: String?)`.
- Produces `HomeUiState.refreshMessage: String?`, `FavoritesUiState.refreshMessage: String?`, `SearchUiState.isInitialLoading: Boolean`, and `SearchUiState.error: String?`.
- `CatalogRepository.refreshVisible(cameraIds, engineProbe)` probes no more than 20 distinct IDs.

- [ ] **Step 1: Add regression test for refresh candidate ceiling**

```kotlin
@Test fun `visible refresh probes at most twenty distinct cameras`() = runBlocking {
    val ids = (1..30).map { "camera-$it" }
    val called = mutableListOf<String>()
    repository.refreshVisible(ids) { camera ->
        called += camera.id
        "ONLINE"
    }
    assertEquals(20, called.size)
    assertEquals(called.size, called.distinct().size)
}
```

Seed 30 `CameraEntity` records in the existing Room test first.

- [ ] **Step 2: Run test and confirm existing behavior**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.CctvDatabaseTest`

Expected: existing implementation passes the ceiling assertion. If it fails, fix only the boundary cause.

- [ ] **Step 3: Return refresh outcome instead of swallowing errors**

Home must sync official/active remote catalog first, then probe current visible favorites/history/recent cameras when sync is unavailable or fails. Favorites probes current favorite IDs. Search pull refresh reloads DB results and reports a loading/error outcome instead of pretending remote data was updated. Failures retain existing cards and show a one-line retryable message.

- [ ] **Step 4: Recompose Home status hierarchy**

Retain real total/online/offline values. Use `StatCard` only for real catalog snapshot data, add semantic text labels, and keep history/favorites/recent sections content-driven. Add no placeholder image or fabricated live count. Pull indicator names the refresh action through content description.

- [ ] **Step 5: Recompose Search for scan-first use**

Keep local field state and 250 ms query debounce. Make search field sticky at top, use visible text label/placeholder, keep filters horizontally scrollable but not clipped, show real `results.size` as loaded count rather than a false total, and use distinct state messages for initial loading, no catalog results, no filter match, query error, and pagination loading. Use current `CameraCard` click behavior only.

- [ ] **Step 6: Add strings in both locales**

Add refresh success/failure, refresh changed-status count, search loading, search error, and catalog-unavailable text in Indonesian and English.

- [ ] **Step 7: Run database and parsing tests**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.CctvDatabaseTest --tests id.nusantara.cctv.CatalogDtoParsingTest`

Expected: PASS. Update stale test comments/expectations only after reading current bundled catalog count/version; do not hardcode old v6 values.

- [ ] **Step 8: Commit refresh and search**

```bash
git add android/app/src/main/kotlin/id/nusantara/cctv/data/catalog/CatalogRepository.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/home/HomeViewModel.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/home/HomeScreen.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/favorites/FavoritesScreen.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/search/SearchViewModel.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/search/SearchScreen.kt android/app/src/main/res/values/strings.xml android/app/src/main/res/values-en/strings.xml android/app/src/test/kotlin/id/nusantara/cctv/CctvDatabaseTest.kt
git commit -m "feat: improve catalog refresh and search"
```

## Task 5: Tactical map layer sheet and camera panel

**Files:**
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/map/MapLayers.kt:9-66`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/map/MapScreen.kt:44-216`
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `android/app/src/main/res/values-en/strings.xml`
- Test: `android/app/src/test/kotlin/id/nusantara/cctv/MapLayerTest.kt`

**Interfaces:**
- Produces `enum class MapLayer(val labelRes: Int, val descriptionRes: Int, val preview: MapLayerPreview)`.
- Produces `MapLayerSelector(selected: MapLayer, onSelect: (MapLayer) -> Unit, onDismiss: () -> Unit)`.
- Produces `MapCameraPanel(camera: Camera, onOpen: (Camera) -> Unit, onDismiss: () -> Unit)`.
- `MapViewModel.setMapLayer(layer)` remains persistence boundary.

- [ ] **Step 1: Write map-layer availability test**

```kotlin
@Test fun `every selectable layer resolves a named tile source`() {
    MapLayer.entries.forEach { layer ->
        assertTrue(MapLayers.tileSource(layer).name.isNotBlank())
    }
}

@Test fun `map layer labels have a preview description`() {
    assertEquals(MapLayer.entries.size, MapLayer.entries.map { it.descriptionRes }.distinct().size)
}
```

- [ ] **Step 2: Run test and confirm it fails for missing selector metadata**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.MapLayerTest`

Expected: FAIL because description/preview metadata does not exist.

- [ ] **Step 3: Add layer metadata without adding tile providers**

Keep existing Mapnik, Esri imagery, Carto dark, and OpenTopoMap sources. Define preview as deterministic ColorScheme-neutral swatches or drawable-free composable miniatures, not captured images. Explain selected source and use tiles under provider terms; do not imply provider ownership.

- [ ] **Step 4: Replace FAB dropdown with modal layer selector**

Use `ModalBottomSheet` on portrait screens. Each entry is full-width with preview, localized name, localized description, semantics role, selected check, 48dp minimum height. Existing floating map layer button opens only this selector. Dismiss works via back and tapping outside.

- [ ] **Step 5: Add camera metadata bottom panel**

On marker click, retain selected `Camera` state in `MapScreen`; open `MapCameraPanel` with actual name, locality, source, current stored status, and `Open camera` action. This action calls existing `onCameraClick(camera)`. Do not embed a stream preview in map panel.

- [ ] **Step 6: Add bilingual strings**

Add map layer descriptions, selector label, selected status, panel source/location/status/open labels to both resource files.

- [ ] **Step 7: Run map unit tests**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.MapLayerTest`

Expected: PASS.

- [ ] **Step 8: Commit Map interaction**

```bash
git add android/app/src/main/kotlin/id/nusantara/cctv/ui/map/MapLayers.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/map/MapScreen.kt android/app/src/main/res/values/strings.xml android/app/src/main/res/values-en/strings.xml android/app/src/test/kotlin/id/nusantara/cctv/MapLayerTest.kt
git commit -m "feat: add tactile map layer selector"
```

## Task 6: Stream failure handling and player retry

**Files:**
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/data/api/SourceHttp.kt:38-50, 60-71`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/data/player/StreamEngine.kt:55-77, 114-140, 147-177`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/data/player/StreamPlayerController.kt:58-141`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/detail/CameraDetailScreen.kt`
- Modify: `android/app/src/main/kotlin/id/nusantara/cctv/ui/detail/PlayerErrorText.kt`
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `android/app/src/main/res/values-en/strings.xml`
- Test: `android/app/src/test/kotlin/id/nusantara/cctv/MjpegDecoderTest.kt`

**Interfaces:**
- Produces `SourceHttp.executeWithOneRetry(request: Request): Response` for transient `IOException` only.
- Produces `PlayerError.BOOTSTRAP` when session bootstrap fails and `PlayerError.NETWORK` only for transient connection/timeout failures.
- `StreamPlayerController.retry(camera)` remains explicit user-only retry trigger.

- [ ] **Step 1: Add a failing transient classification test**

```kotlin
@Test fun `timeout is transient but authorization is not`() {
    assertTrue(SourceHttp.isTransient(IOException("timeout")))
    assertFalse(SourceHttp.isTransient(HttpStatusException(403)))
}
```

If `HttpStatusException` is not present, test `isRetryableHttp(code: Int)` directly:

```kotlin
assertFalse(SourceHttp.isRetryableHttp(401))
assertFalse(SourceHttp.isRetryableHttp(403))
assertFalse(SourceHttp.isRetryableHttp(404))
assertTrue(SourceHttp.isRetryableHttp(503))
```

- [ ] **Step 2: Run test and confirm missing classifier**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.SourceHttpTest`

Expected: FAIL because retry classifier does not exist.

- [ ] **Step 3: Add bounded retry at OkHttp boundary**

One retry only for connection timeout/reset and HTTP 408/429/500/502/503/504 where a retry is safe. Do not retry status 401, 403, 404, invalid media payload, or unsupported format. Preserve 10 second connect, 20 second read, and 30 second call upper bounds. Do not retry Media3 internal segment requests in an application loop.

- [ ] **Step 4: Preserve error cause through resolve/player UI**

Replace bootstrap `runCatching` suppression with logged, typed result. Map `IOException` timeouts to NETWORK, failed bootstrap to BOOTSTRAP/UNAVAILABLE as appropriate, HTTP auth rejection to UNAVAILABLE, and parsing/source creation to SOURCE. Keep `resolve()` on `Dispatchers.IO`; never construct/release `ExoPlayer` off main thread.

- [ ] **Step 5: Ensure retry is visible and safe**

Keep detail-screen retry control only when a camera exists and `PlayerUi.Error` is shown. Disable double starts while `Loading`, announce error/retry state with Compose semantics, and always release old player/MJPEG job before retry.

- [ ] **Step 6: Add bilingual error strings**

Add session unavailable, temporary network failure, retry label/state only in both translation files.

- [ ] **Step 7: Run player tests**

Run: `cd android; ./gradlew.bat testDebugUnitTest --tests id.nusantara.cctv.SourceHttpTest --tests id.nusantara.cctv.MjpegDecoderTest`

Expected: PASS.

- [ ] **Step 8: Commit player reliability**

```bash
git add android/app/src/main/kotlin/id/nusantara/cctv/data/api/SourceHttp.kt android/app/src/main/kotlin/id/nusantara/cctv/data/player/StreamEngine.kt android/app/src/main/kotlin/id/nusantara/cctv/data/player/StreamPlayerController.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/detail/CameraDetailScreen.kt android/app/src/main/kotlin/id/nusantara/cctv/ui/detail/PlayerErrorText.kt android/app/src/main/res/values/strings.xml android/app/src/main/res/values-en/strings.xml android/app/src/test/kotlin/id/nusantara/cctv/SourceHttpTest.kt
git commit -m "fix: clarify stream recovery failures"
```

## Task 7: Source pipeline integrity and verified expansion

**Files:**
- Modify: `tools/discovery/run_all.py:10`
- Modify: `tools/validation/validate.py:23-81, 94-159`
- Modify: `tools/import_export/export.py:14-345`
- Create or modify: `tools/discovery/<verified_source>.py`
- Modify: `docs/data-sources/DATA-SOURCES.md`
- Modify: `data/cameras.json`
- Modify: `data/cameras.csv`
- Modify: `android/app/src/main/assets/catalog/cameras.json`
- Test: `tools/tests/test_export.py`

**Interfaces:**
- Every source ID appears exactly once in `run_all.FETCHERS`, `validate.SOURCES`, and `export.SOURCE_META`.
- Each source module exports `fetch_all() -> list[dict]` and writes `data/raw/<source_id>.json`.
- Export retains only `ONLINE` and `OFFLINE` cameras with nonempty stream URLs.

- [ ] **Step 1: Write failing pipeline invariant tests**

```python
from tools.discovery.run_all import FETCHERS
from tools.import_export.export import SOURCE_META
from tools.validation.validate import SOURCES

def test_each_fetcher_has_validation_and_metadata():
    assert len(FETCHERS) == len(set(FETCHERS))
    assert set(FETCHERS) == set(SOURCE_META) == set(SOURCES)

def test_source_metadata_has_unique_province_code():
    assert all(meta["province"] for meta in SOURCE_META.values())
```

- [ ] **Step 2: Run invariant tests and confirm current duplicate/stale configuration failures**

Run: `python -m unittest tools.tests.test_export`

Expected: FAIL until duplicate `kedirikota` and orphan `bukittinggi` metadata/config paths are removed or reconciled.

- [ ] **Step 3: Repair current source registry before adding anything**

Make exactly one source ID per real source: `malangkota`, `jogjakota`, `palembang`, `banjarmasin`, `bandungkota`, `bukittinggikota`, `kedirikota`. Remove duplicate dictionary keys and fetcher names that do not have actual matching module and export metadata. Keep one unique `KODE_PROV` entry per province. Fix `export.py` source-specific pid/location mapping so Kediri reads the fields actually emitted by `kedirikota.py` (`id`, `name`, `location`, `stream_url`).

- [ ] **Step 4: Implement every research-verified new source one at a time**

For each source from completed research report that has an official governmental portal and passes actual validation:

```python
def fetch_all():
    session = http_session()
    response = session.get(API, timeout=20, headers={"Referer": PORTAL_URL})
    response.raise_for_status()
    cameras = normalize_portal_payload(response.json())
    save_json(RAW_DIR / "<source_id>.json", cameras)
    return cameras
```

Add exactly one matching entry in `FETCHERS`, `SOURCES`, and `SOURCE_META`, with public portal URL, operator, province/city, access type, required referer/bootstrap URL, and source terms. Do not add a source based only on web-search claims; record why each rejected candidate failed.

- [ ] **Step 5: Run discovery and validation source-by-source**

```bash
python tools/discovery/run_all.py
python tools/validation/validate.py --only <source_id>
```

For every admitted source, inspect manifest and segment validation result. Respect per-source worker/delay rate limits. Never disable TLS verification except documented Malang fallback context already established; do not generalize `verify=False` to new sources.

- [ ] **Step 6: Regenerate and test catalog**

```bash
python tools/validation/validate.py
python tools/import_export/export.py
copy data\cameras.json android\app\src\main\assets\catalog\cameras.json
python -m unittest tools.tests.test_export
```

Expected: JSON/csv/asset contain same `catalog_version`, all sources recognized, no empty stream URLs, no source ID mismatch.

- [ ] **Step 7: Update source documentation truthfully**

For each admitted source, document official portal URL, access flow, format, rate limit, coordinates, date checked, and terms. Update rejection list with source and observable reason. State catalog numbers as the regeneration snapshot, not permanent coverage.

- [ ] **Step 8: Commit source integrity and catalog**

```bash
git add tools/discovery tools/validation/validate.py tools/import_export/export.py tools/tests/test_export.py docs/data-sources/DATA-SOURCES.md data/cameras.json data/cameras.csv android/app/src/main/assets/catalog/cameras.json
git commit -m "feat: expand verified public CCTV catalog"
```

## Task 8: Full regression and physical-device verification

**Files:**
- Modify: `android/app/build.gradle.kts:17-18` only after scope passes verification
- Create: `data/shots/major-update-<timestamp>/` locally only
- Modify: `docs/TESTING.md` only if physical-device procedure is missing/incorrect

**Interfaces:**
- Release version increments from `2.0` to `3.0`, `versionCode` from `5` to `6` only after passing all checks.
- No screenshots or raw validation logs are staged.

- [ ] **Step 1: Read current catalog asset to update drifted tests**

Run:

```powershell
python -c "import json; d=json.load(open('android/app/src/main/assets/catalog/cameras.json', encoding='utf-8')); print(d['catalog_version'], len(d['cameras']), sum(c['status']=='ONLINE' for c in d['cameras']))"
```

Update `CctvDatabaseTest` exact catalog version/count only from this output. Preserve assertion that the bundled asset seeds successfully.

- [ ] **Step 2: Run complete static regression**

Run:

```powershell
cd android
./gradlew.bat testDebugUnitTest
./gradlew.bat lint
./gradlew.bat assembleDebug
```

Expected: all unit tests pass, lint reports zero errors, debug APK builds.

- [ ] **Step 3: Install debug APK on physical device**

Run:

```powershell
adb devices
adb install -r "android/app/build/outputs/apk/debug/app-debug.apk"
adb shell am force-stop id.nusantara.cctv
adb shell monkey -p id.nusantara.cctv -c android.intent.category.LAUNCHER 1
```

Expected: only device `UFKRK19A01000720` used; app foregrounds without fatal exception.

- [ ] **Step 4: Capture and inspect five themes**

For System, Light, Dark, Cyber, Monochrome: open About, change theme, navigate Home, Search, Map, Favorites, return About. Capture each with:

```powershell
adb exec-out screencap -p > "data/shots/major-update-<timestamp>/<theme>.png"
```

Inspect each image for readable text, surface contrast, no clipping/overlap, correct bottom navigation, no placeholder content, and 48dp-accessible controls.

- [ ] **Step 5: Exercise primary interactive flows**

1. Pull Home, Search, Favorites down once. Confirm single refresh indicator, catalog/probe result or explicit failure, and preserved existing cards.
2. Search a known source/city and select/clear at least province, status, and stream type filters.
3. Open Map, change all four layers, zoom/pan, select cluster, select one camera marker, dismiss/open camera panel.
4. In About, check update, switch language ID/EN, reveal sources, reveal alternate catalog field, submit `http://example.test/cameras.json`, confirm rejection, reset official source, then sync official source.
5. Play one Malang camera and one non-Malang HLS camera. For an unavailable camera, confirm visible error then manual retry without crash.
6. Toggle favorite, open fullscreen, rotate device, navigate back.

- [ ] **Step 6: Inspect runtime logs**

Run:

```powershell
adb logcat -d -v brief | Select-String -Pattern 'FATAL EXCEPTION|AndroidRuntime|NetworkOnMainThreadException|IllegalStateException'
```

Expected: no matching application crash lines. Investigate any match before proceeding.

- [ ] **Step 7: Build release and update version**

Only after Steps 1-6 pass, update:

```kotlin
versionCode = 6
versionName = "3.0"
```

Run:

```powershell
cd android
./gradlew.bat assembleRelease
```

Expected: signed release APK succeeds.

- [ ] **Step 8: Review and commit final application changes**

Run:

```bash
git status --short
git diff --check
git diff -- AGENTS.md
```

Expected: `AGENTS.md` has no staged or working-tree diff. Do not add `.claude/`, screenshots, raw discovery files, logs, keystore, or external secrets.

```bash
git add android docs data tools dist
git restore --staged AGENTS.md
git commit -m "feat: v3.0 surveillance experience and verified catalog"
```

Do not push, tag, create a GitHub release, or upload artifacts without a separate explicit request.

## Plan self-review

- Spec coverage: Tasks 1 and 3 cover automatic official catalog plus HTTPS alternative. Tasks 2, 4, and 5 cover five themes, contrast, motion, refresh, Search, Map, and About. Task 6 covers stream reliability. Task 7 covers official source integrity and verified expansion. Task 8 covers physical-device verification and v3.0 version policy.
- Placeholder scan: no unresolved implementation placeholders. `<source_id>` occurs only in the explicit per-source pipeline template, whose specific source is selected by completed verification.
- Type consistency: endpoint APIs are introduced in Task 1 before Task 3. `RefreshResult` is introduced in Task 4 before screen use. map APIs are introduced in Task 5. New player error APIs are introduced in Task 6.
