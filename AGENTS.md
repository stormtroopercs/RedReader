# RedReader — Project Context

## Overview
Open-source Reddit client for Android. 100% Kotlin codebase (484 files, 0 Java).

## Branch
**`java-to-kotlin-conversion`** — main development branch.

## Tech Stack
| Component | Version |
|---|---|
| Kotlin | 2.4.10 |
| AGP | 9.3.1 |
| Compose BOM | 2026.08.00 |
| Navigation | **Navigation 3** (1.0.0) |
| compileSdk / targetSdk | 36 |
| minSdk | 23 |
| Hilt | 2.55 (DI) |
| Room | 2.6.x (4 entities, 4 DAOs) |
| OkHttp | 5.3.0 |

## Architecture
**Hybrid UI** — legacy Activities/Fragments coexist with new Compose screens.

### Modules
- `app/` — Main application module
- `libs/redreader-common/` — Shared utilities
- `libs/redreader-datamodel/` — Data models

### Package Layout
- `activities/` — Entry points (`MainActivityCompose` is launcher)
- `navigation/` — Navigation 3 setup + screen composables
- `compose/ui/` — Reusable Compose components (14 screens)
- `di/` — Hilt modules (Application, Database, Network)
- `database/` — Room DB (Post, Comment, Subreddit, UserSession)
- `repository/` — Repository layer (4 repositories)
- `viewmodel/` — ViewModels (6 total)

## Navigation 3
Migrated from Navigation 2 (NavController/NavHost) to Navigation 3.

### Key Files
- `navigation/Screens.kt` — `@Serializable NavKey` definitions (Main, Settings, PostList, CommentList, UserProfile, Inbox, PostSubmit)
- `navigation/NavigationState.kt` — `rememberNavigationState()` + `NavigationState` class (per-top-level back stacks)
- `navigation/Navigator.kt` — `Navigator` class (navigate/goBack actions)
- `navigation/AppNavigation.kt` — `AppNavGraph()` — entryProvider + NavDisplay with entryDecorators
- `navigation/AdaptiveNavigation.kt` — `AdaptiveAppNavigation()` — adaptive layout variant
- `navigation/MainScreen.kt` — Main screen composable
- `activities/MainActivityCompose.kt` — Entry point, renders `AppNavGraph()`

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
- Uses `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` (NOT `androidx.hilt.navigation.compose.hiltViewModel`)
- `rememberViewModelStoreNavEntryDecorator()` in `entryDecorators` scopes ViewModels per NavEntry
- Reference: `nav3-recipes/passingarguments/viewmodels/hilt/HiltViewModelsActivity.kt`

### Removed
- `androidx.navigation:navigation-compose` (Nav 2) — removed from libs.versions.toml and build.gradle.kts
- `compose/adaptive/AdaptiveNavigation.kt` — orphaned duplicate, deleted
- All `androidx.navigation.*` imports (replaced with `androidx.navigation3.*`)
- All `androidx.hilt.navigation.compose.hiltViewModel` imports (replaced with `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`)

## Conventions
- Commit per logical section, not per file.
- NavKeys are `@Serializable` data classes/objects implementing `NavKey`.
- Top-level routes have independent back stacks; child routes push onto the active stack.
- `Navigator.navigate()` auto-detects top-level vs child routes.
