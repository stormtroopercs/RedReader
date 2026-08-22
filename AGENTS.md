# RedReader — Project Context

## Overview
Open-source Reddit client for Android. 100% Kotlin repo (514 .kt files, 0 .java — 488 in `src/main`, 19 in `src/test`, 7 in `src/androidTest`).

## Branch
**`java-to-kotlin-conversion`** — main development branch.

## Tech Stack
| Component | Version |
|---|---|
| Kotlin | 2.4.10 |
| AGP | 9.3.1 |
| Compose BOM | 2026.08.00 |
| Navigation | **Navigation 3** (catalog 1.0.0, resolves to 1.0.1) |
| compileSdk / targetSdk | 37 / 37 |
| minSdk | 23 |
| Hilt | 2.60.1 (DI) |
| Room | 2.8.0 (4 entities, 4 DAOs) |
| OkHttp | 5.3.0 |

## Architecture
**Hybrid UI** — legacy Activities/Fragments coexist with new Compose screens.

### Modules
Single Gradle module. `settings.gradle.kts` defines `rootProject.name = "RedReader"` with **no `include()` statements** — all source lives in `src/` at the repo root.

> Note: `core/`, `feature/`, and `libs/` contain leftover `build/` output (untracked) but are **not part of the build**. `libs/redreader-common/` and `libs/redreader-datamodel/` have their own `build.gradle.kts` but are not wired into `settings.gradle.kts`, so they are orphaned and not compiled.

### Package Layout
All production code is under `src/main/java/org/quantumbadger/redreader/`:
- `activities/` — Entry points (`MainActivityCompose` is the launcher)
- `navigation/` — Navigation 3 setup, screen composables, and all 6 ViewModels
- `compose/ui/` — Reusable Compose components and screens (23 files)
- `di/` — Hilt modules (Application, Database)
- `database/` — Room DB (`entities/` + `dao/`: Post, Comment, Subreddit, UserSession)
- `repository/` — Repository layer (4 repositories)

## Navigation 3
Migrated from Navigation 2 (NavController/NavHost) to Navigation 3.

### Key Files
- `navigation/Screens.kt` — `@Serializable NavKey` definitions (15 routes: Main, Settings, PostList, CommentList, UserProfile, Inbox, PostSubmit, SubredditSearch, CommentReply, RedditTerms, Changelog, BugReport, WebViewRoute, HtmlView, OAuthLogin)
- `navigation/NavigationState.kt` — `rememberNavigationState()` + `NavigationState` class (per-top-level back stacks)
- `navigation/Navigator.kt` — `Navigator` class (navigate/goBack actions)
- `navigation/AppNavigation.kt` — `AppNavGraph(navigationState: NavigationState)` — entryProvider + NavDisplay with entryDecorators
- `navigation/AdaptiveNavigation.kt` — `AdaptiveAppNavigation()` — adaptive layout variant
- `navigation/MainScreen.kt` — Main screen composable
- `activities/MainActivityCompose.kt` — Entry point; owns the `NavigationState` at activity scope (routes system back through `baseActivityOnBackPressed`/`baseActivityMustInterceptBack`) and renders `AppNavGraph(navigationState)`. Also handles cold-start deep-link extras (`EXTRA_DEEP_LINK`: `"inbox"` → Main+Inbox, `"changelog"` → Settings+Changelog) used by the re-routed legacy entry points (new-message notification, legacy settings links)

### Pattern
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
        entry<PostList> { key -> /* key.subreddit */ }
    }
)
```

### ViewModel Integration
- Uses `androidx.hilt.navigation.compose.hiltViewModel` (from `androidx.hilt:hilt-navigation-compose:1.2.0`). Note: `androidx.hilt.lifecycle.viewmodel.compose` does NOT exist as an artifact — the recipe's import was corrected at conversion time (commit "fix: Correct hiltViewModel import path").
- `rememberViewModelStoreNavEntryDecorator()` in `entryDecorators` scopes ViewModels per NavEntry
- Reference: `nav3-recipes/passingarguments/viewmodels/hilt/HiltViewModelsActivity.kt`

### Removed
- `androidx.navigation:navigation-compose` (Nav 2) — removed from libs.versions.toml and build.gradle.kts
- `compose/adaptive/AdaptiveNavigation.kt` — orphaned duplicate, deleted
- All `androidx.navigation.*` (Nav 2) imports — replaced with `androidx.navigation3.*`
- All `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` imports (nonexistent package) — replaced with `androidx.hilt.navigation.compose.hiltViewModel`

## Conventions
- Commit per logical section, not per file.
- NavKeys are `@Serializable` data classes/objects implementing `NavKey`.
- Top-level routes have independent back stacks; child routes push onto the active stack.
- `Navigator.navigate()` auto-detects top-level vs child routes.
