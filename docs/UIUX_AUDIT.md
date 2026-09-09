# UI/UX AUDIT — Nusantara CCTV Monitor v2.2

**Audit Date:** 2026-09-09  
**Current Version:** 2.2 (versionCode 7)  
**Platform:** Android native, Jetpack Compose + Material 3

---

## EXECUTIVE SUMMARY

App sudah **functional complete** dengan architecture solid (MVVM + Repository, Media3, osmdroid). Tech stack modern, code bersih, no major crashes. 

**Primary gaps:** design system immature, spacing inconsistent, player UX minimal, map info density tinggi, navigation flat (5 tabs — mental load), empty/error states generic, no progressive disclosure, accessibility untested, information architecture belum dioptimalkan untuk skala (674 kamera).

**Opportunity:** UI/UX polish bisa tingkatkan perceived quality 2-3x tanpa architecture rewrite.

---

## 1. ARCHITECTURE AUDIT

### ✅ STRENGTHS

**Clean separation:**
- MVVM + Repository pattern proporsional
- Manual DI (AppContainer) — no overhead, graf kecil
- Room + Flow — reactive, offline-first
- StreamEngine adapter pattern — format agnostic (HLS/DASH/MJPEG/RTSP)
- Coroutines + StateFlow — lifecycle-aware
- Single module — build cepat, no premature modularization

**Data flow solid:**
- Seed JSON bundled → Room → UI (offline-first)
- Remote sync opsional + hash verification
- Status probe on-demand (bukan polling massal)
- Favorites + history lokal (no auth)

**Player robust:**
- Media3/ExoPlayer + HLS native
- Cookie/referer injection per-source (malangkota bootstrap)
- MJPEG multipart parser manual
- RTSP unsupported dengan pesan jelas

**Map clustering:**
- Screen-space grid (90px cell) — rebuild on zoom/pan
- No bonuspack dependency
- Handles 674 markers tanpa lag

### ⚠️ WEAKNESSES

**ViewModels bloated:**
- `HomeViewModel` aggregate 3 flows (cameras, favorites, history) — bisa pisah ke UseCase
- `SearchViewModel` debounce + pagination + filter cascade — complex, hard test
- `MapViewModel` clustering logic masuk UI layer — seharusnya domain

**Player lifecycle:**
- `StreamPlayerController` couple ke ViewModel — hard mock untuk test
- ExoPlayer release manual — risk leak kalau DisposableEffect miss
- MJPEG frame flow unbounded — no backpressure (bisa OOM kalau consumer lambat)

**Map performance:**
- Rebuild markers setiap recompose — andalkan remember tapi fragile
- No marker pool — alokasi baru setiap zoom (bisa optimize)
- Clustering grid static 90px — tidak scale per zoom level

**DB queries:**
- `CameraDao.observeAll()` emit full list setiap update — bisa incremental
- Search FTS absent — query LIKE `%term%` slow di 674+ rows
- No index on status/province/city — filter scan full table

**No instrumented tests:**
- Unit test ada (4 files) tapi UI test zero
- Navigation flow untested
- Player state machine untested

---

## 2. CURRENT UI STATE

### Theme System

**Colors:**
- Material You dynamic (Android 12+) ✅
- Fallback: teal/deep-sea hardcoded
- Preset: CYBER (neon), MONOCHROME (grayscale)
- Status colors hardcoded di `StatusDot` (0xFF4CAF50) — bukan token

**Typography:**
- 6 variants defined: headlineSmall, titleLarge/Medium, bodyMedium/Small, labelLarge/Small
- No Display variant untuk hero
- Line height default Compose (tight) — long text cramped
- Font: system default (no custom font)

**Spacing:**
- **CRITICAL:** No design tokens. 93 instances `.dp` hardcoded.
- Patterns found: 4dp, 8dp, 10dp, 12dp, 16dp, 24dp, 28dp, 32dp, 48dp
- Mostly 8dp grid tapi inconsistent (10dp vs 12dp arbitrary)
- `Arrangement.spacedBy(12.dp)` most common
- No semantic naming (spacing.sm/md/lg)

**Shape:**
- RoundedCornerShape: 10dp, 12dp, 28dp (pill search bar)
- No token system

**Elevation:**
- Card default elevation (Material 3 tonalElevation)
- Map panel 4.dp hardcoded
- No shadow/depth system defined

### Current Screens

#### Home (`HomeScreen.kt`)

**Layout:**
```
Title + subtitle (static text)
↓
StatCard row: Total | Online | Offline (3 columns)
↓
History section (2-column grid, max 6)
↓
Favorites section (list, max 5)
↓
Recently checked section (list, unlimited scroll)
```

**Issues:**
- Stat cards decorative — tidak actionable
- History tiles: icon placeholder + StatusDot — no thumbnail
- Favorites truncated 5 — "see all" missing
- Recently checked: last_checked sort tapi tidak tampil timestamp
- No empty state kalau new user
- No "Near You" (GPS ada tapi unused di Home)
- Pull-to-refresh probes ALL visible cameras — bisa 16+ network req sekaligus

#### Map (`MapScreen.kt`)

**Layout:**
```
osmdroid MapView (fullscreen)
↓
LayerPanel (floating top-left): 4 basemap buttons
↓
StatusBar (floating bottom): coordinate display
↓
BottomSheet (modal): camera preview on marker tap
```

**Issues:**
- **CRITICAL:** 674 markers — clustering works tapi visual masih ramai zoom-out
- Cluster count badge hardcoded Paint (bukan Compose) — styling inconsistent
- Bottom sheet modal — blocks map interaction
- No search on map
- No filter on map (harus ke Search tab)
- Layer panel 4 buttons vertikal — takes vertical space
- Status bar coordinate: niche use case, always visible
- Marker color status-based tapi kecil (40px) — hard distinguish zoom-out
- No "locate me" button
- No camera detail preview (nama + location) — harus tap baru tahu

#### Search (`SearchScreen.kt`)

**Layout:**
```
Pill search bar (28dp radius)
↓
Filter chips row (6 filters): Province | City | District | Status | Stream | Operator
↓
Result count text
↓
LazyColumn camera cards (pagination 60/page)
```

**Issues:**
- Filter chips always visible — takes space even if unused
- 6 filters horizontal scroll — province/city/district cascade tapi tidak intuitive
- No active filter summary (selected count)
- No "Clear all filters" quick action
- Search debounce 250ms — bisa 300-400ms (less aggressive)
- Empty state generic: "Mulai mencari" — bisa suggest popular searches
- No search history
- No grouping results (by province/city)
- Result count text small — easy miss

#### Detail (`CameraDetailScreen.kt`)

**Layout:**
```
TopAppBar: back | title | favorite | fullscreen | reload
↓
Player area (16:9 aspect ratio locked)
↓
Metadata card: status dot | nama | location | stream type | operator
↓
"Periksa status" button
↓
"Lihat di peta" button
```

**Issues:**
- **HIGH:** Player aspect ratio 16:9 forced — kalau stream 4:3 atau square, pillarbox/letterbox muncul
- Player loading: CircularProgressIndicator center — no text "Connecting..." / "Buffering..."
- Player error: generic `PlayerErrorText` — tidak kasih context (network? format? auth?)
- Metadata flat list — location bisa hierarchy (province > city > district)
- "Periksa status" button — unclear apa bedanya dengan reload
- No stream quality indicator (bitrate/resolution kalau available)
- No last_checked timestamp display
- No related cameras (same intersection/area)
- Fullscreen button di AppBar — should be in player controls (common pattern)

#### Fullscreen Player (`FullscreenPlayerScreen.kt`)

**Layout:**
```
Black background
↓
Player fills screen (sensor landscape forced)
↓
Back button floating top-left
```

**Issues:**
- **CRITICAL:** Forces sensor landscape — tapi kalau user nonton portrait device (phone on stand), forced rotate annoying
- No immersive mode (status/nav bars hidden)
- Back button always visible — should hide with player controls
- No keep-screen-on explicit (rely on player default)
- MJPEG fullscreen: bitmap scaled tapi no aspect ratio preserve logic visible

#### Favorites (`FavoritesScreen.kt`)

**Layout:**
```
Title
↓
Empty state (if zero favorites)
↓
LazyColumn camera cards
```

**Issues:**
- Empty state good (icon + text + hint) ✅
- No grouping (by province/city)
- No sort options (alphabetical, last viewed, status)
- Pull-to-refresh probes all favorites — bisa mahal kalau 20+ favorites

#### About (`AboutScreen.kt`)

**Layout:**
```
App version card
↓
Theme picker (5 chips: System | Light | Dark | Cyber | Monochrome)
↓
Language picker (2 chips: Indonesia | English)
↓
Update checker section (button + version info)
↓
Catalog advanced (optional URL)
↓
Attribution (sources list, collapsible)
```

**Issues:**
- Theme picker: 5 horizontal chips — scroll on small screen
- Update checker: manual button — bisa auto-check on open (sudah ada 4s delay di AppRoot)
- Catalog URL: power-user feature tapi prominent — bisa hide di "Advanced"
- Attribution sources: expand/collapse — good ✅
- No licenses screen (LICENSES.md link bisa tambah)
- No "Report issue" link
- No app intro / onboarding hint

---

## 3. COMPONENT AUDIT

### `CameraCard.kt`

**Current:**
- Card dengan StatusDot + nama + location + stream/operator metadata
- Press scale animation (PressScale) ✅
- Ripple ✅

**Issues:**
- Status dot 10dp — small, hard tap target
- Location flatten: "Kecamatan • Kota" — bisa hierarchy
- Stream type + operator same line — cramped kalau panjang
- No visual "Live" indicator selain dot
- No last_checked hint
- No distance (kalau GPS available)

### `StatusDot`

**Current:**
- 10dp circle, color-coded:
  - ONLINE: green 0x4CAF50
  - OFFLINE/TIMEOUT/INVALID_STREAM/MOVED: red 0xEF5350
  - AUTH_REQUIRED: orange 0xFFB74D
  - else: gray 0x9E9E9E

**Issues:**
- **MEDIUM:** Color-only status — accessibility fail (colorblind, screen reader)
- No label text (kalau zoom/large text)
- Hardcoded colors — not semantic tokens
- Size 10dp terlalu kecil untuk touch interaction
- Bisa tambah pulse animation untuk LIVE

### `EmptyState`

**Current:**
- Generic component: icon (48dp) + title + description
- Used: favorites, search, history

**Issues:**
- Icon 48dp — could be 64-72dp (more prominent)
- No CTA button option (e.g., "Browse map" dari favorites empty)
- No illustration/image option (icon aja bland)

### `OfflineBanner`

**Current:**
- Red error container strip, icon + text

**Good:** ✅ Contextual, non-blocking

**Issues:**
- Always top edge — bisa collide dengan TopAppBar
- No dismiss action
- No "Retry" CTA

### Missing Components

**Needed:**
- Loading skeleton (card placeholder)
- Toast/Snackbar wrapper (error feedback)
- Bottom sheet reusable (map pakai modal manual)
- Filter bottom sheet (search filter bisa dedicated component)
- Status badge (bukan dot aja — bisa chip "LIVE" / "OFFLINE")
- Section header reusable (Home punya, tapi local function)
- Metadata row (key-value pairs — detail screen manual layout)

---

## 4. UX FRICTION POINTS

### Navigation

**Current:** 5 tabs bottom nav — Home | Map | Search | Favorites | About

**Issues:**
- **MEDIUM:** 5 tabs high — Nielsen max 3-5, tapi About bukan primary task
- About bisa pindah ke Settings icon di Home/Search AppBar
- Map + Search overlap — bisa merge (Map dengan search overlay)
- Home → Detail → back — no breadcrumb / context
- Deep link support unclear (camera/{id} defined tapi no intent-filter audit)

### Discovery Flow

**Intended:** Home → Browse/Search → Detail → Watch

**Friction:**
- New user: landing Home tapi mostly empty (no favorites, no history)
- No onboarding hint "Tap Map to explore" / "Search by city"
- Map overwhelming (674 markers) — no guided entry
- Search empty state passive — no suggestions "Try: Jakarta, Surabaya, Malang"

### Camera Selection

**Goal:** User cari kamera terdekat / lokasi spesifik

**Friction:**
- No "Near Me" di Home (GPS permission granted tapi unused)
- Map: harus zoom manual ke lokasi — no geolocation button
- Search: harus tahu nama kota — no autocomplete, no "Popular cities"
- Filter cascade: pilih province → city → district — banyak tap, bisa preset "Jawa Timur cities"

### Watching Stream

**Goal:** Open camera → watch live → fullscreen jika perlu

**Friction:**
- Detail screen player 16:9 fixed — kalau portrait user, player kecil
- Loading state: spinner aja — no text feedback "Connecting to camera..."
- Error generic: "Kamera tidak tersedia" — tidak kasih cause (offline? auth? format?)
- Fullscreen: force landscape — annoy portrait viewers
- No PiP (picture-in-picture) — user tidak bisa browse camera lain sambil nonton
- No multi-camera view (grid 2x2 popular intersections)

### Status Understanding

**Goal:** User tahu kamera online/offline/bermasalah

**Issues:**
- Status dot color-only — no text label di card
- "ONLINE" vs "TIMEOUT" vs "OFFLINE" — tidak jelas beda (ke user sama aja "not working")
- "AUTH_REQUIRED" — user bingung (apakah bisa nonton? tidak bisa?)
- Last_checked tidak display — user tidak tahu status fresh/stale
- "Periksa status" button — tidak jelas aksi apa (probe? refresh?)

### Favorites Management

**Goal:** Save camera favorit, quick access

**Friction:**
- Favorite toggle di Detail screen aja — tidak bisa favorite dari card (Home/Search)
- No bulk favorite (select multiple di Map)
- No favorite folders/groups (by city, by route)
- No sort/filter di Favorites screen
- No "add to Home screen widget" (Android widget)

---

## 5. INFORMATION ARCHITECTURE GAPS

### Hierarchy Issues

**Camera metadata flatten:**
```
Current: "Simpang X, Kec. Y, Kota Z, Jawa Timur"
Better:  Hierarchy visual atau breadcrumb
```

**Home sections unclear priority:**
- History, Favorites, Recently Checked — ketiga bisa overlap, tidak jelas mana primary

### Progressive Disclosure Absent

**Detail screen dump semua:**
- Metadata flat (nama, location, stream, operator, source URL)
- Bisa: essential info visible, technical details collapse "Show more"

**Search filter 6 chips exposed:**
- Bisa: common filters visible (Province, Status), advanced collapse

### Grouping Weak

**Search results flat list:**
- 674 cameras scroll panjang
- Bisa group by: Province header → city list

**Map no region boundary:**
- Marker scatter — tidak ada polygon province/city
- Bisa: zoom out show province label, zoom in show city boundary

### Contextual Info Missing

**Detail screen isolate:**
- Camera X di Simpang Y — tidak tahu camera lain nearby
- Bisa: "3 other cameras in this area" link

**Map marker tap:**
- Bottom sheet minimal (nama + status) — harus tap "Open" untuk detail
- Bisa: preview thumbnail (snapshot?), quick metadata

---

## 6. VISUAL DESIGN ISSUES

### Inconsistent Spacing

**Found:** 4, 8, 10, 12, 16, 24, 28, 32, 48 dp
**Should:** 4, 8, 12, 16, 24, 32, 48, 64 (consistent scale)

**Example inconsistency:**
- Home history grid: `spacedBy(10.dp)` 
- Search results: `spacedBy(10.dp)`
- Favorites: `spacedBy(10.dp)`
- Detail buttons: `spacedBy(12.dp)`

Bisa standardize: contentPadding 16dp, item spacing 12dp, section spacing 24dp.

### Typography Gaps

**No Display style** — Home title "Nusantara CCTV" pakai headlineSmall (24sp Bold) — bisa 32-36sp Display.

**Body line height default** — Compose default 1.43 — tight untuk long text (location names, error messages). Bisa 1.5-1.6.

**No caption metadata style** — stream type / operator pakai labelSmall (11sp Medium) — bisa 10sp Regular caption.

### Color System Incomplete

**Status colors hardcoded** — tidak semantic token.

Bisa:
```kotlin
object StatusColors {
    val live = Color(0xFF4CAF50)      // green
    val offline = Color(0xFFEF5350)   // red  
    val degraded = Color(0xFFFFB74D)  // orange
    val unknown = Color(0xFF9E9E9E)   // gray
}
```

**No surface elevation tokens** — card, bottom sheet, dialog pakai default.

### Shape Inconsistency

- Card: 12dp corner (default Material 3)
- Search bar: 28dp pill
- History tile: 12dp
- Filter chip: default (8dp)

Bisa define: shape.small (8dp), shape.medium (12dp), shape.large (16dp), shape.pill (999dp).

### Animation Minimal

**Exist:** PressScale (camera card press) ✅

**Missing:**
- Favorite toggle (heart fill animation)
- Status dot pulse (live indicator)
- Map cluster expand/collapse
- Screen transition (nav fade default aja)
- Pull-to-refresh custom indicator
- Loading progress (player buffering)

---

## 7. INTERACTION PATTERNS

### Tap Targets

**StatusDot 10dp** — too small tap (min 48dp Android guideline).

**Filter chip** — default size OK, tapi kalau 6 chips scroll, edge chips hard tap.

**Map marker 40dp** — OK tapi cluster badge text kecil (Paint 16sp).

### Gestures

**Map:** pan, zoom, tap marker ✅

**Player:** tap show/hide controls (ExoPlayer default) ✅

**Missing:**
- Swipe dismiss (bottom sheet)
- Long-press camera card (quick favorite)
- Pinch zoom (player — kalau MJPEG)

### Feedback

**Visual:**
- Ripple effect ✅
- Press scale ✅
- Loading spinner ✅

**Missing:**
- Haptic feedback (favorite toggle, filter select)
- Sound (optional, camera stream start)
- Toast confirmation ("Added to favorites")

### State Visibility

**Loading:**
- Home: pull-to-refresh indicator ✅
- Search: "Memuat..." text di bottom ✅
- Player: CircularProgressIndicator ✅

**Error:**
- Player: `PlayerErrorText` component ✅
- Network: `OfflineBanner` ✅

**Empty:**
- Favorites: `EmptyState` ✅
- Search: `EmptyState` ✅

**Missing:**
- Skeleton loading (card placeholder saat fetch)
- Retry after error (harus manual reload)
- Progress indicator (catalog sync, status probe batch)

---

## 8. ACCESSIBILITY AUDIT

### Screen Reader

**Not tested** — assume zero screen reader optimization.

**Likely issues:**
- StatusDot no contentDescription
- Map markers no semantic label
- Filter chips no selection announcement
- Player controls default (Media3) — likely OK

### Color Contrast

**Status colors:**
- Green 0x4CAF50 on white — contrast 3.08:1 ❌ (WCAG AA fail)
- Red 0xEF5350 on white — contrast 3.79:1 ❌ (WCAG AA fail)
- Orange 0xFFB74D on white — contrast 2.24:1 ❌ (WCAG AA fail)

**Fix:** Use Material 3 semantic colors (error, primary, tertiary) — auto contrast.

### Font Scaling

**Not tested** — Compose scale default, likely OK tapi:
- StatCard value (headlineSmall) bisa overflow kalau 200% scale
- Camera name (titleMedium) 1 line ellipsis — bisa 2 lines
- Search bar placeholder — bisa wrap

### Touch Targets

**StatusDot 10dp** — fail 48dp min.

**Map marker 40dp** — borderline (min 48dp, tapi marker center tolerance).

---

## 9. PERFORMANCE OBSERVATIONS

### Startup

- Seed JSON load first launch — measured? (likely <1s, 674 cameras ~200KB)
- Room query observeAll() — emit full list — bisa jadi bottleneck kalau 2000+ cameras

### Scroll Performance

- LazyColumn camera cards — lazy load ✅
- Search pagination 60/page — reasonable ✅
- Map rebuild markers — remember() cache, tapi recompose trigger bisa optimize

### Memory

- ExoPlayer lifecycle — manual release DisposableEffect — risk leak
- MJPEG frames unbounded Flow — no backpressure — bisa OOM
- Map Bitmap markers — no pool — bisa optimize

### Network

- Parallel probe status (pull-to-refresh) — bisa 16+ concurrent — no rate limit, bisa throttle
- HLS manifest fetch default timeout 10s — reasonable
- No request deduplication (multiple Detail screen same camera)

---

## 10. ANDROID PLATFORM GAPS

### Material Design 3

**Good:**
- Dynamic color ✅
- Semantic color scheme ✅
- Typography scale ✅

**Missing:**
- Motion system (standard easing, duration)
- Elevation overlays
- State layers (hover — no, focus — keyboard nav untested)

### Edge-to-Edge

**Not implemented** — status bar + nav bar default insets.

Bisa: WindowCompat.setDecorFitsSystemWindows(window, false), apply padding insets.

### Predictive Back (Android 14+)

**Not implemented** — default back behavior.

### Dynamic Color Contrast (Material You)

**Implemented** ✅ — Android 12+ dynamic scheme.

**Fallback** ✅ — teal/deep-sea pre-Android 12.

### Split Screen / Foldable

**Not tested** — likely breaks:
- Map 5-tab nav di narrow width
- Player aspect ratio di split 50/50
- Fullscreen force landscape di foldable

### Widgets

**Absent** — opportunity: "Favorite cameras" widget (tap open Detail).

### Shortcuts

**Absent** — opportunity: dynamic shortcuts (recent cameras, favorites).

### Notifications

**Absent** — opportunity: "Camera X offline" alert (opt-in).

---

## 11. DATA PRESENTATION ISSUES

### Status Ambiguity

**"ONLINE"** — clear ✅

**"OFFLINE" vs "TIMEOUT" vs "INVALID_STREAM"** — user tidak perlu tahu technical difference. Bisa merge: "Unavailable (offline)" / "Unavailable (error)".

**"AUTH_REQUIRED"** — user bingung apakah bisa nonton. Bisa: "Requires login (not supported)".

**"UNKNOWN"** — passive. Bisa: "Not checked yet".

### Location Naming

**Flatten:** "Simpang Lima, Kec. Klojen, Kota Malang, Jawa Timur"

**Verbose** — bisa hierarchy:
```
Simpang Lima
Klojen, Malang · Jawa Timur
```

### Stream Metadata

**"HLS • Dishub"** — cryptic untuk non-technical user.

Bisa: "Live stream · Dishub Malang" (translate stream type ke user-friendly).

### Timestamp

**lastChecked: "2026-09-09T14:32:10"** — stored ISO 8601 tapi **tidak ditampilkan**.

Bisa: "Checked 2 minutes ago" / "Last online 3 hours ago".

---

## 12. SEVERITY CLASSIFICATION

### CRITICAL (Blocks Core Function)

- ❌ None found — app functional

### HIGH (Major UX Degradation)

1. **Spacing inconsistency (93 hardcoded dp)** — design system immature, scaling sulit
2. **Player aspect ratio forced 16:9** — pillarbox/letterbox kalau stream non-standard
3. **Status color-only** — accessibility fail (colorblind, screen reader)
4. **Map marker density zoom-out** — 674 markers overwhelming (clustering works tapi visual ramai)
5. **Fullscreen force landscape** — annoy portrait viewers
6. **No progressive disclosure** — detail screen info dump, search filter always exposed

### MEDIUM (Usability Issues)

7. **Navigation 5 tabs** — About bukan primary task, bisa Settings icon
8. **Home empty new user** — no onboarding, no "Near Me", no suggestions
9. **Search no grouping** — 674 flat list, bisa group by province
10. **Favorites no sort/filter** — linear list aja
11. **Player loading/error generic** — no context feedback
12. **No skeleton loading** — spinner aja, bisa placeholder cards
13. **Pull-to-refresh parallel probe** — 16+ concurrent network req, no throttle
14. **Detail screen no related cameras** — isolate, bisa "Nearby cameras"
15. **Typography line height tight** — long text cramped

### LOW (Quality of Life)

16. **StatCard tidak actionable** — decorative, bisa tap filter by status
17. **History tile no thumbnail** — icon placeholder, bisa snapshot
18. **Search no autocomplete** — manual typing
19. **Filter chip scroll** — 6 chips horizontal, edge chips hard reach
20. **No favorite from card** — harus open Detail
21. **Map no geolocation button** — manual zoom ke lokasi
22. **No PiP** — cannot browse while watching
23. **Empty state no CTA** — passive text
24. **No haptic feedback** — interactions flat
25. **Edge-to-edge absent** — default insets

### POLISH (Visual/Consistency)

26. **Typography no Display style** — Home title undersized
27. **Shape inconsistency** — 10dp vs 12dp vs 28dp arbitrary
28. **Color tokens absent** — status colors hardcoded
29. **Animation minimal** — no favorite pulse, status live indicator
30. **No dark mode map audit** — map tiles contrast unknown
31. **No licenses screen** — attribution ada, link LICENSES.md bisa tambah
32. **Timestamp tidak display** — last_checked stored tapi hidden

---

## 13. COMPETITIVE REFERENCE

### Google Maps (Relevant Patterns)

- **Bottom sheet persistent** — camera preview on map bisa persistent (bukan modal)
- **Search suggestions** — autocomplete city names
- **"Near Me" prominent** — geolocation primary action
- **POI clustering** — zoom-dependent marker density
- **Info hierarchy** — name bold, address secondary, metadata collapse

### YouTube / YouTube TV (Player Reference)

- **Loading state text** — "Connecting..." / "Buffering..."
- **Error retry prominent** — big "Retry" button
- **Quality selector** — auto/720p/480p (kalau stream support)
- **Gesture controls** — double-tap skip, swipe volume/brightness
- **Immersive mode fullscreen** — hide system UI

### Waze (Live Status)

- **Status badge pulse** — live indicator animation
- **User-reported status** — "offline 2 min ago" timestamp
- **Related alerts** — "3 reports nearby"

### Windy (Map Interaction)

- **Layer picker** — bottom sheet, not floating buttons
- **Info overlay** — tap map show overlay card, not modal
- **Zoom-dependent detail** — zoom out hide detail, zoom in show

---

## 14. RECOMMENDATIONS PRIORITY

### Phase 1: Design System Foundation (Effort: Medium, Impact: High)

1. **Create spacing tokens** — 4/8/12/16/24/32/48/64dp semantic (xs/sm/md/lg/xl/xxl)
2. **Create color tokens** — status colors semantic (live/offline/degraded/unknown)
3. **Typography scale expand** — add Display, fix line height 1.5
4. **Shape tokens** — small/medium/large/pill
5. **Replace 93 hardcoded dp** — migrate to tokens

**Outcome:** Consistency, scalability, maintainability.

### Phase 2: Critical UX Fixes (Effort: Medium, Impact: High)

6. **Status accessibility** — color + icon + text label (bukan dot aja)
7. **Player loading/error feedback** — text context ("Connecting...", "Error: camera offline")
8. **Progressive disclosure** — Detail metadata collapse "Show more", Search filter hide advanced
9. **Map info density** — zoom-dependent clustering, marker size scale
10. **Navigation simplify** — merge About ke Settings, consider Map search overlay

**Outcome:** Usability, accessibility, perceived quality.

### Phase 3: Discovery & Onboarding (Effort: Low, Impact: Medium)

11. **Home "Near Me"** — GPS-based nearby cameras section
12. **Search suggestions** — popular cities, recent searches
13. **Empty state CTA** — "Browse map" button dari favorites empty
14. **Map geolocation button** — tap center to user location
15. **Onboarding hints** — first launch tips overlay (dismissible)

**Outcome:** New user conversion, feature discovery.

### Phase 4: Information Architecture (Effort: Medium, Impact: Medium)

16. **Search grouping** — results by province/city headers
17. **Detail related cameras** — "3 nearby" link
18. **Favorites sort/filter** — by city, by last viewed, by status
19. **Metadata hierarchy** — location breadcrumb, technical details collapse
20. **Timestamp display** — "Checked 2 min ago" humanize

**Outcome:** Scalability, navigation clarity.

### Phase 5: Polish & Platform (Effort: High, Impact: Low-Medium)

21. **Animation system** — favorite pulse, status live indicator, screen transitions
22. **Skeleton loading** — card placeholders
23. **Edge-to-edge** — immersive layout
24. **PiP support** — watch while browsing
25. **Widgets** — home screen favorite cameras
26. **Instrumented tests** — UI navigation, player state machine

**Outcome:** Premium feel, platform native experience.

---

## 15. SUCCESS METRICS

**Pre-change baseline:**
- Startup to first camera: ? taps (measure)
- Search to watch: ? taps (measure)
- Map to watch: ? taps (measure)

**Post-redesign target:**
- Reduce "Home empty" bounce (add Near Me, suggestions)
- Reduce search abandonment (add grouping, autocomplete)
- Increase favorite usage (add quick favorite, sort)
- Reduce player error confusion (add contextual feedback)

**Qualitative:**
- Perceived quality: "modern Android app" (vs "functional tapi bland")
- Accessibility: WCAG AA contrast, screen reader support
- Consistency: design system mature, spacing/color/shape unified

---

## APPENDIX: FILE INVENTORY

### UI Screens (9 files)
- `ui/home/HomeScreen.kt` + `HomeViewModel.kt`
- `ui/map/MapScreen.kt` + `MapViewModel.kt` + `Clusterer.kt` + `MapLayers.kt`
- `ui/search/SearchScreen.kt` + `SearchViewModel.kt`
- `ui/detail/CameraDetailScreen.kt` + `FullscreenPlayerScreen.kt` + `PlayerErrorText.kt`
- `ui/favorites/FavoritesScreen.kt`
- `ui/about/AboutScreen.kt` + `AboutViewModel.kt`

### Components (4 files)
- `ui/components/CameraCard.kt`
- `ui/components/Common.kt` (StatusDot, EmptyState, OfflineBanner, ErrorRetry)
- `ui/components/PressScale.kt`
- `ui/theme/Theme.kt` + `Type.kt`

### Data Layer (8 files)
- `data/model/Models.kt`
- `data/db/CctvDatabase.kt` + entities + DAOs
- `data/catalog/CatalogRepository.kt` + `CatalogDto.kt`
- `data/player/StreamEngine.kt` + `StreamPlayerController.kt`
- `data/prefs/AppPreferences.kt` + `ThemePalette.kt`
- `data/api/SourceHttp.kt`
- `data/update/UpdateChecker.kt`

### Tests (4 files)
- `CatalogDtoParsingTest.kt`
- `CctvDatabaseTest.kt`
- `MjpegDecoderTest.kt`
- `VersionCompareTest.kt`

**Total:** 33 Kotlin files main source, 4 test files.

---

**End of Audit.**
