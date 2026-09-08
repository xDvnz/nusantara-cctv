# Nusantara CCTV Monitor Major Update Design

## Scope

Refactor the existing Android UI while retaining native Kotlin, Compose, Room, Media3, osmdroid, existing catalog schema, and verified-source policy. `AGENTS.md` remains local-only and excluded from commits.

## Visual direction

Reading this as: public CCTV monitoring mobile application for Indonesian users, with Material You foundation and mobile tactical-surveillance Cyber variant, dial ENERGY 2 / RHYTHM 2 / MOTION 2.

- System, Light, Dark, Cyber, and Monochrome themes remain selectable.
- System uses dynamic Material You on Android 12+ and existing fallback colors on Android 10-11.
- Cyber uses matte dark surfaces, teal for actionable/live state, amber for warnings, red only for failure. It references the supplied surveillance screenshot without copying its desktop layout.
- Monochrome removes hue dependence and retains semantic text/icon labels.
- Each theme is built from Material `ColorScheme` tokens. No raw foreground colors in screen UI.
- Typography remains app typography because it is readable, locally available, and matches Android platform conventions.
- Surfaces use tonal hierarchy rather than pervasive shadow. Elevated controls are limited to selectors, sheets, and transient panels.
- Identity motif: concise live/status indicators and map camera markers, because application task is inspecting current public camera availability.

Reasons:
- Material color tokens keep text contrast and component states consistent across all five themes.
- Cyber hierarchy prioritizes status scanning without turning the product into a desktop-console clone.
- Teal only marks available/actionable surveillance state so attention has one clear meaning.
- Short 150-220 ms state transitions explain touch response without consuming battery or distracting from video.

## UI architecture

Keep navigation, ViewModels, repository, and player contracts. Refactor shared Compose components and screen composition only where behavior or consistency changes.

### Shared theme and motion

- Extend theme palette tokens only as required by existing Material 3 components.
- Use composition-local motion settings derived from Android animator duration scale. When animations are disabled, state changes are immediate.
- Interactive surfaces use Material state layers and one press-scale treatment. Tap targets remain at least 48dp.
- Every data view retains loading, empty, error, and populated states.

### Home, Search, Favorites

- Home and Favorites expose pull-to-refresh only when their content can refresh.
- Refresh synchronizes remote catalog from official default URL, then probes at most 20 currently visible cameras. Failed requests leave existing local data intact and return a readable status.
- Search is reorganized around a sticky search field, compact filter chips, result count from actual DB results, grouped readable results, and explicit no-match/loading/error states.
- No fabricated metrics, availability claims, or placeholder camera cards.

### Map

- Retain osmdroid and screen-space clustering.
- Replace plain dropdown layer picker with an accessible layer sheet anchored from a 48dp control. Entries show a real preview swatch, selected state, label, and description.
- Layers remain only those whose tile source is configured and operational: standard, satellite, terrain, dark.
- Persist layer preference in DataStore.
- Camera marker selection opens a portrait-safe bottom panel with actual camera metadata and open action. No video preview is fabricated before user opens the stream.

### About and catalog

- All sections use one `AboutCard` primitive and shared spacing.
- Keep only App, Appearance, Updates, Catalog, Sources, and Developer sections.
- Sources and attribution are one expandable section from real source records.
- Default catalog URL is the repository raw JSON URL. It is used automatically at app launch and during pull-to-refresh.
- Alternative catalog URL is an advanced optional field. It accepts HTTPS only, requires a valid known schema, and only applies versions newer than installed data. A reset action restores official default.
- Portal URLs are never accepted as catalog URLs because they are not the catalog schema.

## Reliability

- Preserve database user data through explicit migrations. Never use destructive migration for application upgrades.
- Fix confirmed Room test drift and add regression tests for URL validation, fallback selection, and catalog synchronization behavior.
- Configure network security configuration in manifest when its certificate anchors are needed.
- Resolve/bootstrap remains on `Dispatchers.IO`; all source errors become user-visible `PlayerUi.Error` states, with a retry action.
- Retry only transient connection failures with bounded attempts and timeout. Do not retry authentication rejection, invalid stream URLs, or unsupported RTSP.
- Preserve the Malang invariant: bootstrap, catalog request, stream request, and segment request use one consistent User-Agent plus complete cookies.

## Data expansion

- Discovery pipeline remains authority for all catalog additions.
- Add a locality only after its official public-government portal, metadata, manifest, and at least one video segment pass the existing validation rule.
- Validate sources serially where rate limiting requires it. Record source-specific bootstrap, referer, User-Agent, and TLS quirks in fetcher/export/validation configuration.
- Failed, private, token-only, geo-restricted, challenge-protected, or unverified endpoints are excluded.
- Application copy states coverage is a verified catalog snapshot, never all CCTV in Indonesia and never permanent live availability.

## Verification

1. Run debug build, unit tests, and lint.
2. Install and operate on detected physical ADB device only, no emulator.
3. Exercise each theme, each map layer, theme selection, navigation, search filter, pull-to-refresh, catalog reset/alternative validation, camera detail, Malang playback, one non-Malang playback, retry error state, favorite, fullscreen, language switch, and update check.
4. Capture screenshots from physical device and inspect for clipped/overlapping text, touch layout, and theme contrast.
5. Use WCAG contrast checks for explicit Cyber and Monochrome text/background token pairs. Dynamic system colors rely on Material You semantic scheme.

## Non-goals

- No WebView, private camera access, credential bypass, stream recording, mirroring, re-encoding, mass device-side probing, Google Maps API key, RTSP relay, or claim that every Indonesian CCTV is accessible.
