# COMPETITIVE ANALYSIS — Public CCTV / Map / Media Apps

**Purpose:** Extract proven patterns dari apps serupa untuk inform redesign decisions.

**Scope:** Interaction patterns, information hierarchy, navigation, live status, map UX, player experience — bukan visual clone.

---

## APPS ANALYZED

1. **Google Maps** — POI discovery, map clustering, location search
2. **Waze** — Live status indicators, user-reported data, bottom sheet interaction
3. **YouTube / YouTube TV** — Video player UX, loading/error states, fullscreen
4. **Windy** — Weather map layers, zoom-dependent info density, overlay cards
5. **Flightradar24** — Real-time tracking, info cards, filter overlays
6. **Citymapper** — Multi-modal navigation, contextual info, quick actions

---

## 1. GOOGLE MAPS — Discovery & Navigation

### Pattern: Bottom Sheet Persistent

**Implementation:**
- Tap marker → bottom sheet slides up (bukan modal penuh)
- 3 states: collapsed (peek), half-expanded (summary), full (detail)
- Swipe down collapse, tap backdrop dismiss
- Map tetap interactable di state collapsed/half

**Applies to CCTV:**
- Map marker tap → peek: camera name + status + thumbnail
- Swipe up → half: location metadata + quick actions (favorite, share)
- Swipe up full → same as Detail screen (player embed)
- **Benefit:** User bisa compare cameras tanpa back-forth navigation

### Pattern: Search Autocomplete Aggressive

**Implementation:**
- Ketik 1 karakter → suggestions muncul
- Recent searches prioritas top
- Category chips: Restaurants, Gas, Hotels (contextual)
- "Near [location]" suggestions

**Applies to CCTV:**
- Search camera: "Malang" → suggest cities: "Malang, Magelang, ..."
- Recent: last 5 searched cities/cameras
- Chips: "Near me", "Jawa Timur", "Online cameras", "Favorites"
- **Benefit:** Reduce typing, guide discovery

### Pattern: Info Hierarchy Progressive

**Implementation:**
- POI card collapsed: Name (bold) + category + rating + distance
- Expanded: + hours, photos, reviews, directions
- Tertiary info hidden: "More info" section

**Applies to CCTV:**
- Camera card: Name (bold) + status badge + location
- Tap expand: + stream type, operator, last checked, coordinates
- Technical: source URL, confidence score → "Technical details" collapse
- **Benefit:** Clarity, reduce cognitive load

### Pattern: "Nearby" as Primary Action

**Implementation:**
- Open app → "Explore nearby" default tab
- GPS permission upfront dengan clear benefit ("Find places near you")
- Nearby results auto-update saat pan map

**Applies to CCTV:**
- Home screen: "Cameras near you" section top (if GPS granted)
- Map: geolocation button prominent (blue dot + center)
- Permission: "See nearby traffic cameras in your city"
- **Benefit:** Immediate utility, local relevance

---

## 2. WAZE — Live Status & Community Data

### Pattern: Status Badge with Timestamp

**Implementation:**
- "Heavy traffic" badge + "Reported 2 min ago"
- Color + icon + text + time = complete context
- Pulse animation pada fresh reports (<5 min)
- Gray out stale data (>30 min)

**Applies to CCTV:**
- Status: "ONLINE" badge green + icon 📹 + "Checked 2 min ago"
- "OFFLINE" badge red + icon ⚠️ + "Last online 3 hours ago"
- Pulse animation "LIVE" untuk stream aktif
- Gray text "Status unknown (not checked today)"
- **Benefit:** User tahu data fresh/stale, bukan assume real-time

### Pattern: Related Alerts Contextual

**Implementation:**
- Tap road segment → "3 reports on this road"
- Group by proximity + type
- Quick scroll nearby alerts

**Applies to CCTV:**
- Detail screen: "2 other cameras at this intersection"
- Map cluster: "5 cameras in Kota Malang"
- Tap cluster → list preview + "See all"
- **Benefit:** Discover related content, context awareness

### Pattern: Bottom Navigation 4 Tabs Max

**Implementation:**
- 4 tabs: Home, Alerts, Map, Profile
- Map access dari Home (bukan separate tab)

**Applies to CCTV:**
- Consider: Home, Search, Favorites, Settings (4 tabs)
- Map integrate ke Home (floating button) atau Search overlay
- About merge ke Settings
- **Benefit:** Reduce tab overload

---

## 3. YOUTUBE / YOUTUBE TV — Video Player UX

### Pattern: Loading State with Text

**Implementation:**
- Spinner + text "Getting video ready..."
- Progress: "Buffering... 45%"
- Network slow: "Slow connection. Video may buffer"
- Timeout: "Taking longer than usual. [Retry]"

**Applies to CCTV:**
- Player loading: "Connecting to camera..." (bukan spinner mute)
- Buffering: "Loading stream..." (HLS segment fetch)
- Slow: "Connection slow. Stream quality may vary"
- Error clear: "Camera offline. Last seen 2 hours ago. [Retry]"
- **Benefit:** User informed, manage expectations

### Pattern: Fullscreen Immersive

**Implementation:**
- Enter fullscreen → hide status bar + nav bar (edge-to-edge)
- Tap → show controls 3s, auto-hide
- Swipe down → exit fullscreen (predictive back)
- Portrait lock option (user choice, bukan force)

**Applies to CCTV:**
- Fullscreen: immersive mode (hide system UI)
- Controls: tap show, 3s timeout auto-hide
- Back gesture: swipe down dismiss (YouTube pattern familiar)
- Orientation: user choice (jangan force landscape)
- **Benefit:** Maximize screen real estate, user control

### Pattern: Error Recovery Prominent

**Implementation:**
- Error overlay: icon + message + big "Retry" button
- "Playback error. Tap to retry."
- Alternative action: "Go back" / "Browse related"
- No stack trace, no technical jargon

**Applies to CCTV:**
- Error overlay: camera icon crossed + "Camera unavailable"
- Primary: big "Retry" button
- Secondary: "View on map" / "Try another camera"
- Context: "This camera isn't responding. It may be offline."
- **Benefit:** Clear action, no dead-end

### Pattern: Quality Selector (If Applicable)

**Implementation:**
- Settings gear → Quality: Auto / 1080p / 720p / 480p
- Badge: "HD" / "SD"
- Auto default dengan network-aware fallback

**Applies to CCTV:**
- Most CCTV single quality (HLS adaptive playlist rare)
- If multi-bitrate available: "Auto (recommended)" / "High" / "Low"
- Else: hide selector, show resolution badge "720p" (if known)
- **Benefit:** Transparency, user control bandwidth

---

## 4. WINDY — Map Layers & Data Overlays

### Pattern: Layer Picker Bottom Sheet

**Implementation:**
- Floating button → bottom sheet layer picker
- Visual preview tiap layer (bukan text aja)
- Radio select current active
- Swipe dismiss

**Applies to CCTV:**
- Map LAYERS button → bottom sheet (bukan floating 4 buttons)
- Preview: thumbnail each basemap (OSM, Satelit, Gelap, Medan)
- Active layer radio selected
- **Benefit:** Less visual clutter, preview before switch

### Pattern: Zoom-Dependent Detail

**Implementation:**
- Zoom out: show regional summary (province-level data)
- Zoom in: granular data (city markers)
- Zoom closer: individual POI labels
- Auto-hide labels kalau density tinggi

**Applies to CCTV:**
- Zoom out (<6): cluster by province (badge count)
- Zoom mid (6-10): cluster by city
- Zoom in (>10): individual markers
- Label: show on zoom >12, hide kalau overlapping
- **Benefit:** Reduce clutter, scale gracefully

### Pattern: Info Card Overlay (Not Modal)

**Implementation:**
- Tap location → card slide dari bottom (peek)
- Card floating di atas map, map tetap pan/zoom
- Card drag up expand, drag down dismiss
- No fullscreen takeover

**Applies to CCTV:**
- Marker tap → card peek (camera name + status + thumbnail)
- Drag up → half (metadata + actions)
- Map tetap interactable (pan ke camera lain)
- **Benefit:** Multi-tasking, compare cameras

---

## 5. FLIGHTRADAR24 — Real-Time Tracking

### Pattern: Filter Overlay Toggle

**Implementation:**
- Filter button → overlay panel (bukan navigate away)
- Checkboxes: Aircraft type, Airline, Altitude range
- Apply real-time (no "Done" button)
- Clear all chips visible

**Applies to CCTV:**
- Search filter → bottom sheet overlay
- Chips visible di Search bar (active count badge)
- Apply instant (auto-search debounce)
- Clear all prominent
- **Benefit:** Context retention, immediate feedback

### Pattern: Live Indicator Animation

**Implementation:**
- Active tracking: pulsing dot + "LIVE" badge
- Historical: static gray dot + timestamp
- Update frequency label: "Updated 5 sec ago"

**Applies to CCTV:**
- ONLINE camera: green pulse dot + "LIVE" badge
- Status badge animation: fade pulse 2s loop
- Last checked: "Checked 30 sec ago" (relative time)
- **Benefit:** Visual real-time feel, data freshness clear

### Pattern: Detail Panel Scrollable

**Implementation:**
- Aircraft detail: panel half-screen, scrollable content
- Sections: Current, Flight info, Aircraft, Photos
- Map tetap visible 50% screen

**Applies to CCTV:**
- Camera detail di map: bottom sheet half, scrollable
- Sections: Video preview, Location, Metadata, Related
- Map tetap visible top 50%
- **Benefit:** Context + detail simultan

---

## 6. CITYMAPPER — Contextual Actions

### Pattern: Quick Actions Row

**Implementation:**
- Location screen: row icon buttons
- "Directions" | "Save" | "Share" | "Street view"
- 4 max actions, most-used prioritas

**Applies to CCTV:**
- Camera detail: quick actions row
- "Favorite" | "Share" | "View on map" | "Fullscreen"
- Icon + label, horizontal scroll kalau >4
- **Benefit:** Common tasks accessible, reduce menu dive

### Pattern: Section Headers Sticky

**Implementation:**
- List sections: "Nearby", "Recent", "Favorites"
- Header sticky on scroll
- Visual separator (line + spacing)

**Applies to CCTV:**
- Home: "Near You" → "Recently Watched" → "Favorites" → "All Cameras"
- Search results: "Jawa Timur" → "DI Yogyakarta" (province headers)
- Sticky headers on scroll
- **Benefit:** Orientation, hierarchy clear

---

## PATTERN SUMMARY — APPLICABILITY RANKING

### HIGH PRIORITY (Directly Applicable, High Impact)

1. **Bottom sheet persistent (Maps, Windy)** — Map marker → peek, expand, dimiss. Benefit: context retention, multi-compare.
2. **Status with timestamp (Waze)** — "ONLINE · Checked 2 min ago". Benefit: data freshness transparency.
3. **Loading text feedback (YouTube)** — "Connecting to camera..." bukan spinner mute. Benefit: user informed.
4. **Progressive disclosure (Maps)** — Essential info visible, technical collapse. Benefit: clarity.
5. **Search autocomplete (Maps)** — City suggestions, recent searches. Benefit: discovery, efficiency.

### MEDIUM PRIORITY (Applicable, Moderate Effort)

6. **Zoom-dependent clustering (Windy)** — Province → city → camera. Benefit: scale gracefully.
7. **Layer picker bottom sheet (Windy)** — Preview basemap before switch. Benefit: less clutter.
8. **Error recovery prominent (YouTube)** — Big retry, context message. Benefit: no dead-end.
9. **Quick actions row (Citymapper)** — Favorite | Share | Map | Fullscreen. Benefit: common tasks accessible.
10. **Live indicator animation (Flightradar)** — Pulse "LIVE" badge. Benefit: visual feedback.

### LOW PRIORITY (Nice-to-Have, Requires Infrastructure)

11. **Filter overlay instant apply (Flightradar)** — Real-time filter tanpa "Done". Benefit: immediate feedback (tapi debounce perlu tune).
12. **Related content contextual (Waze)** — "2 cameras at this intersection". Benefit: discovery (tapi perlu spatial query optimize).
13. **Fullscreen immersive (YouTube)** — Hide system UI. Benefit: maximize screen (tapi edge-to-edge setup).
14. **Section headers sticky (Citymapper)** — Scroll dengan header fixed. Benefit: orientation (tapi LazyColumn stickyHeader support).

---

## ANTI-PATTERNS TO AVOID

### From Nusantara CCTV Current

1. **Modal bottom sheet (current Map)** — Blocks map interaction → use persistent sheet
2. **5-tab navigation** — Cognitive load → merge/reduce to 4 max
3. **Color-only status** — Accessibility fail → add icon + text
4. **Generic empty state** — Passive → add CTA / suggestions
5. **Player loading mute** — User uncertain → add text feedback

### From Other Apps Mistakes

6. **Over-animation** — Excessive motion sickness (beberapa weather apps) → use subtle pulse only
7. **Auto-play aggressive** — Bandwidth waste (news apps) → CCTV should manual play (user initiate)
8. **Notification spam** — Waze over-alert → CCTV no push notification (passive app)
9. **Paywall core features** — Maps API billing shock → CCTV stay free, use OSM
10. **Complex onboarding** — 10-screen tutorial skip rate tinggi → max 2-3 tips, dismissible

---

## DESIGN PRINCIPLE EXTRACTION

### From Maps/Waze: **Local-First**
- GPS prominent, nearby default
- Apply: "Near You" Home section, geolocation button

### From YouTube: **Feedback-Rich**
- Loading text, error context, progress visible
- Apply: Player states verbose, error actionable

### From Windy: **Progressive Complexity**
- Simple view default, layers/filters opt-in
- Apply: Essential info visible, advanced collapse

### From Flightradar: **Data Freshness Transparency**
- Timestamp always visible, stale grayed
- Apply: "Checked X ago" every status badge

### From Citymapper: **Task-Oriented**
- Quick actions common tasks, reduce navigation
- Apply: Favorite/Share/Map/Fullscreen row prominent

---

## IMPLEMENTATION NOTES

### Pattern: Bottom Sheet Persistent

**Compose Implementation:**
```kotlin
ModalBottomSheet(
    sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false // enable 3 states
    ),
    onDismissRequest = { /* collapse */ }
) {
    // peek: 80dp height
    // half: 50% screen
    // full: 90% screen
}
```

**Gotcha:** `ModalBottomSheet` di Compose Material 3 default modal (backdrop blocks). Perlu custom `BottomSheetScaffold` untuk persistent non-blocking.

### Pattern: Zoom-Dependent Clustering

**OSMDroid Implementation:**
```kotlin
val zoom = mapView.zoomLevelDouble
val cellSize = when {
    zoom < 6 -> 200 // province-level clusters
    zoom < 10 -> 90 // city-level (current)
    else -> 40 // individual, minimal cluster
}
clusterer.cluster(items, projection, cellSize)
```

**Benefit:** Existing `Clusterer.kt` sudah flexible, tinggal scale cellPx per zoom.

### Pattern: Live Indicator Pulse

**Compose Implementation:**
```kotlin
val infiniteTransition = rememberInfiniteTransition()
val alpha by infiniteTransition.animateFloat(
    initialValue = 1f, targetValue = 0.3f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000),
        repeatMode = RepeatMode.Reverse
    )
)
Box(
    Modifier
        .size(12.dp)
        .alpha(if (status == "ONLINE") alpha else 1f)
        .background(liveColor, CircleShape)
)
```

**Cost:** Animation per LIVE camera card — acceptable kalau <50 visible, bisa disable kalau list >100.

---

## COMPETITIVE GAPS (Features They Have, CCTV Doesn't Need)

- **Social features** (Waze user reports) — CCTV data government-sourced, no UGC
- **Ads** (Maps promoted pins) — CCTV non-commercial
- **Premium tiers** (YouTube Premium) — CCTV free public utility
- **AR mode** (Maps Live View) — CCTV stationary cameras, no AR benefit
- **Voice navigation** (Waze audio) — CCTV passive viewing
- **Offline maps** (Maps download regions) — CCTV streams require network

---

## CONCLUSION

**Top 5 patterns adopt:**

1. **Bottom sheet persistent** — Map UX overhaul
2. **Status timestamp** — Data transparency
3. **Player feedback verbose** — Loading/error text
4. **Search autocomplete** — Discovery efficiency
5. **Progressive disclosure** — Clarity hierarchy

**Implementation priority:** Match dengan UIUX_AUDIT.md Phase 2 Critical UX Fixes.

---

**End of Analysis.**
