# RedReader — Project Context

## Overview
RedReader is an open-source Reddit client for Android. The end goal of this work is a **fully Compose-based app**: one modern stack — 100% Kotlin, Navigation 3 routes on `MainActivityCompose`, Hilt DI, Room — where every screen (listing, post/comment detail, edit, submit, media, profile, settings) is an in-app Compose route and the legacy Activity/Fragment stack is deleted entirely, with **no loss of existing features** (live Reddit data, OAuth, post/comment editing, media viewing, submit with flair + Imgur, deep links, share intents).

Work proceeds incrementally per legacy surface: port to Compose → re-point its callers → verify (compile/assemble/unit/connected + on-device) → delete the legacy classes, layouts, and manifest entry in the same increment. Cutover state: the legacy listing stack, post submission, and comment/post edit are Compose-only; the remaining legacy screens are the settings panels and `ImageViewActivity` (API-resolved GIF/video hosts + page-URL images). The repo is 100% Kotlin today (467 .kt, 0 .java — 441 in `src/main`, 19 in `src/test`, 7 in `src/androidTest`).

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
- `navigation/` — Navigation 3 setup, screen composables, and all 8 ViewModels
- `compose/ui/` — Reusable Compose components and screens (25 files)
- `di/` — Hilt modules (Application, Database)
- `database/` — Room DB (`entities/` + `dao/`: Post, Comment, Subreddit, UserSession)
- `repository/` — Repository layer (4 repositories)

## Navigation 3
Migrated from Navigation 2 (NavController/NavHost) to Navigation 3.

### Key Files
- `navigation/Screens.kt` — `@Serializable NavKey` definitions (17 routes: Main, Settings, PostList, CommentList, UserProfile, Inbox, PostSubmit, SubredditSearch, CommentReply, RedditTerms, Changelog, BugReport, WebViewRoute, HtmlView, OAuthLogin, Album, Image)
- `navigation/NavigationState.kt` — `rememberNavigationState()` + `NavigationState` class (per-top-level back stacks)
- `navigation/Navigator.kt` — `Navigator` class (navigate/goBack actions)
- `navigation/AppNavigation.kt` — `AppNavGraph(navigationState: NavigationState)` — entryProvider + NavDisplay with entryDecorators
- `navigation/AdaptiveNavigation.kt` — `AdaptiveAppNavigation()` — adaptive layout variant
- `navigation/MainScreen.kt` — Main screen composable
- `activities/MainActivityCompose.kt` — Entry point; owns the `NavigationState` at activity scope (routes system back through `baseActivityOnBackPressed`/`baseActivityMustInterceptBack`) and renders `AppNavGraph(navigationState)`. Also handles cold-start deep-link extras (`EXTRA_DEEP_LINK`: `"inbox"` → Main+Inbox, `"changelog"` → Settings+Changelog, `"search"` → Main+SubredditSearch, `"album"` → Main+Album(url) with the URL in `EXTRA_ALBUM_URL`, `"comment_reply"` → Main+CommentReply(postId, commentId) with the id-and-type in `EXTRA_COMMENT_REPLY_ID_AND_TYPE`, `"terms"` → Settings+RedditTerms, `"post_listing"` → Main+PostList(listPath, searchQuery) with the listing path in `EXTRA_POST_LISTING_SUBREDDIT` (a subreddit, `frontpage` / `popular` / `all`, any user listing `u/<user>/<submitted|saved|hidden|upvoted|downvoted>`, a multireddit `m/<name>` / `u/<user>/m/<name>`, or the search location — a subreddit name, `m/<name>` / `u/<user>/m/<name>`, or blank for a global search) plus the search query in `EXTRA_POST_LISTING_SEARCH_QUERY` (only for search listings; `PostListViewModel` then builds a `SearchPostListURL` instead of a plain listing and derives the screen title as `Search: <q>` or `<location>: <q>`; otherwise it maps the path to the matching Reddit listing URL (multireddits to `/me/m/<name>/`) and derives the screen title), `"comment_listing"` → Main+CommentList(listingPath) with the listing path in `EXTRA_COMMENT_LISTING_POST_ID` (a post id, or `u/<user>/comments` — `CommentListViewModel` fetches the matching comment listing and derives the screen title), `"user_profile"` → Main+UserProfile(username) with the username in `EXTRA_USER_PROFILE_USERNAME` — the user-profile links now open the in-app Compose profile (the legacy `UserProfileDialog` fragment was retired with it; it now carries real block/unblock via `RedditAPI.blockUser`/`unblockUser`, account age, suspended/friend/you badges, send-message, and a more-info dialog), `"post_submit"` → Main+PostSubmit(subreddit, shareUrl) with the subreddit in `EXTRA_POST_SUBMIT_SUBREDDIT` and the optional shared text in `EXTRA_POST_SUBMIT_SHARE_URL` — the post-submission form (title, Text/Link type, body/URL, flair dropdown when the subreddit offers flair, "Upload to Imgur" chip for link posts, subreddit-history picker) is the in-app Compose `PostSubmitScreen` driven by `PostSubmitViewModel`; it submits via the cache pipeline's `api/submit` request (the same request `RedditAPI.submit` builds), `"comment_edit"` → Main+CommentEdit(idAndType, initialText, isSelfPost) with the id-and-type in `EXTRA_COMMENT_EDIT_ID_AND_TYPE`, the current markdown in `EXTRA_COMMENT_EDIT_TEXT`, and the self-post flag in `EXTRA_COMMENT_EDIT_SELF_POST` — the comment/post edit form (a Compose `CommentEditScreen` driven by `CommentEditViewModel`, which issues the `api/editusertext` request via `RedditAPI.editComment` — the same endpoint edits both comments and self posts, so one screen covers both titles) is the in-app replacement for the legacy `CommentEditActivity` (retired; the legacy toolbar's `RedditPostActions.Action.EDIT` now re-points here, the same pattern `Action.REPLY` uses); the markdown preview re-uses the legacy `MarkdownParser` in a `MaterialAlertDialog`. `MainActivityCompose` also handles the app's `ACTION_SEND` (text/plain) intent-filter: a share launch cold-starts straight into the post form with the shared text pre-filled as the link URL and the subreddit picker up front

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
