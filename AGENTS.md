# RedReader — Project Context

## Overview
RedReader is an open-source Reddit client for Android. The end goal of this work is a **fully Compose-based app**: one modern stack — 100% Kotlin, Navigation 3 routes on `MainActivityCompose`, Hilt DI, Room — where every screen (listing, post/comment detail, edit, submit, media, profile, settings) is an in-app Compose route and the legacy Activity/Fragment stack is deleted entirely, with **no loss of existing features** (live Reddit data, OAuth, post/comment editing, media viewing, submit with flair + Imgur, deep links, share intents).

Work proceeds incrementally per legacy surface: port to Compose → re-point its callers → verify (compile/assemble/unit/connected + on-device) → delete the legacy classes, layouts, and manifest entry in the same increment. Cutover state: the legacy listing stack, post submission, comment/post edit, the entire legacy settings stack (the 13-panel Compose `SettingsScreen` replaced `SettingsActivity`/`SettingsFragment`; the legacy classes, all 15 `res/xml/prefs_*.xml` files, and the `androidx.preference` dependency are deleted), and **the last legacy content screen are all retired**: `ImageViewActivity` was deleted in the 38th. Its final caller (in-album images whose entries need host resolution) now opens the Compose `ImageScreen`, which self-resolves the album via `LinkHandler.getImageInfo` and pages the result with its `HorizontalPager`. The standalone media paths: direct still/`.gif`/video file URLs, pattern-resolvable hosts (imgflip / makeameme / giphy) via `LinkHandler.resolveImagePatternUrl`, and live-API hosts (imgur / gfycat / redgifs / streamable / v.redd.it / deviantart) via `NetWrapper.fetchImageInfo`, all with the media toolbar (save / share media / share link / image-info) and a "view in browser" fallback for unplayable hosts. Remaining legacy surfaces are the five non-content activities still in the manifest — `PMSendActivity` (mailto/PM form; already Compose-launched), `BugReportActivity`, `HtmlViewActivity`, `LinkDispatchActivity`, `WebViewActivity` — plus the `receivers` (BootReceiver / NewMessageChecker / RegularCachePruner). The repo is 100% Kotlin today (432 .kt, 0 .java — 401 in `src/main`, 19 in `src/test`, 7 in `src/androidTest`).

## Branch
**`java-to-kotlin-conversion`** — main development branch.

## Tech Stack
| Component | Version |
|---|---|
| Kotlin | 2.4.10 |
| AGP | 9.3.1 (never downgrade below the 9.x series) |
| Compose BOM | 2026.08.00 |
| Navigation | Navigation 3 (1.1.6) |
| compileSdk / targetSdk / minSdk | 37 / 37 / 23 |
| Hilt / Room / OkHttp | 2.60.1 / 2.8.4 / 5.5.0 |

## Architecture
Single Gradle module; all source under `src/` (no `include()` in `settings.gradle.kts`). `core/`, `feature/`, `libs/` at the repo root are **not part of the build**.

Production code is under `src/main/java/org/quantumbadger/redreader/`:
- `activities/` — entry points (`MainActivityCompose` is the launcher)
- `navigation/` — Navigation 3 setup, screen composables, ViewModels
- `compose/` — Compose components, screens, prefs, theme, net layer
- `di/`, `database/`, `repository/` — Hilt modules, Room, repository layer

## Navigation 3
Nav 2 (NavController/NavHost) was fully removed; everything uses `androidx.navigation3.*`.

Key files:
- `navigation/Screens.kt` — `@Serializable` `NavKey` routes (18)
- `navigation/NavigationState.kt` / `Navigator.kt` — per-top-level back stacks + navigate/goBack
- `navigation/AppNavigation.kt` — `AppNavGraph(navigationState)` (entryProvider + `NavDisplay`); `AdaptiveNavigation.kt` is the adaptive duplicate — **keep both in lockstep**
- `activities/MainActivityCompose.kt` — launcher; owns `NavigationState` (system back via `baseActivityOnBackPressed`/`baseActivityMustInterceptBack`), cold-start deep links, and the `ACTION_SEND` share intent. Deep-link routes + extras live as `DEEP_LINK_*`/`EXTRA_*` constants there (see the local LLM wiki `deep-link-extras` page for the full map).

Pattern:
```kotlin
NavDisplay(
    backStack = navigationState.backStack,
    onBack = { navigator.goBack() },
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()  // ViewModel scoping per NavEntry
    ),
    entryProvider = entryProvider {
        entry<Main> { /* ... */ }
    }
)
```

ViewModels: `hiltViewModel` from `androidx.hilt.navigation.compose.hiltViewModel` (`hilt-navigation-compose`) — the `androidx.hilt.lifecycle.viewmodel.compose` package does **not** exist; don't re-introduce it.

## Conventions
- Commit per logical section (per legacy surface/increment), not per file; AGENTS.md in its own commit.
- NavKeys are `@Serializable` data classes/objects implementing `NavKey`.
- Top-level routes have independent back stacks; child routes push onto the active stack.
- No dead weight in the repo: legacy classes, layouts, and `res/xml` prefs are deleted in the same increment that re-points their callers.
