# RedReader — Project Context

## Overview
RedReader is an open-source Reddit client for Android. The end goal of this work is a **fully Compose-based app**: one modern stack — 100% Kotlin, Navigation 3 routes on `MainActivityCompose`, Hilt DI, Room — where every screen (listing, post/comment detail, edit, submit, media, profile, settings) is an in-app Compose route and the legacy Activity/Fragment stack is deleted entirely, with **no loss of existing features** (live Reddit data, OAuth, post/comment editing, media viewing, submit with flair + Imgur, deep links, share intents).

Work proceeds incrementally per legacy surface: port to Compose → re-point its callers → verify (compile/assemble/unit/connected + on-device) → delete the legacy classes, layouts, and manifest entry in the same increment. Cutover state: the legacy listing stack, post submission, comment/post edit, the entire legacy settings stack (the 13-panel Compose `SettingsScreen` replaced `SettingsActivity`/`SettingsFragment`; the legacy classes, all 15 `res/xml/prefs_*.xml` files, and the `androidx.preference` dependency are deleted), **the last legacy content screen**, and the PM composer are all retired: `ImageViewActivity` was deleted in the 38th (its final caller, in-album images whose entries need host resolution, now opens the Compose `ImageScreen`, which self-resolves the album via `LinkHandler.getImageInfo` and pages the result with its `HorizontalPager`), and `PMSendActivity` in the 39th (replaced by the Compose `PMSendScreen` + `PMSend` route; `UserProfileScreen.onSendMessage` and the `cm:` deep link both re-pointed to it; draft-memory kept in a process-wide `PMSendDraft` since the ViewModel is per-entry), and `BugReportActivity` in the 40th (the app-wide error logger + report generator moved out of its companion into `common/BugReporter.kt`; `handleGlobalError` now starts `MainActivityCompose` with the `DEEP_LINK_BUG_REPORT` deep link onto the existing Compose `BugReport` route, and Settings → About gained a "Report a bug" row), and `HtmlViewActivity` in the 41st (the existing Compose `HtmlView` route — already the license viewer — became the only HTML viewer: its WebView's document history is walked before system back via `navigation/HtmlViewBackHandler.kt` + the activity's back overrides, Settings → About's license row loads the asset through `common/AssetHelper.kt`, and cold-start launches use the `html_view` deep link with the HTML/title extras; `BaseActivity.invalidateBackPressedCallback()` is public so the callback stays in sync with the nav stack + WebView history). In the 42nd the legacy Views layer was deleted outright: `WebViewActivity` (the last `ViewsBaseActivity` subclass — a thin wrapper that `setContent`'d the Compose `WebViewScreen`) was removed and `LinkHandler.openInternalBrowser` now starts `MainActivityCompose` with the `web_view` deep link onto the existing Compose `WebViewRoute` (url/title extras), and `LinkDispatchActivity` (the external deep-link funnel) was removed — its VIEW intent-filters and `onCreate` logic moved onto `MainActivityCompose` (`handleExternalViewIntent`, run on first creation for ACTION_VIEW intents: `redreader://` completes the OAuth login, anything else goes through `LinkHandler.onLinkClicked` — which starts the working Compose screen — after which the trampoline finishes). With their last live behaviours re-pointed, the orphaned legacy menu/selection stack went too: `ViewsBaseActivity`, `RRFragment`, `MainMenuFragment`, `MainMenuListingManager`, `MainMenuSelectionListener`, `OptionsMenuUtility`, `PostSelectionListener`, `WebViewFragment`, `views/bezelmenu/*`, `views/webview/*`, the post-action dispatcher `RedditPostActions`, and the orphaned `reddit/prepared` render pipeline (`RedditPreparedPost`, `RedditParsedPost`/`Comment`, `RedditRenderableComment`/`ListItem`, `bodytext/*`, `html/*`) (`prepared/RedditChangeDataManager` and `prepared/markdown/` are kept — used by Settings/`CommentEditScreen`); the three live sort enums no longer implement `OptionsMenuUtility.Sort` (per-entry `menuTitle` string resources + `onSortSelected` removed; `PostCommentSort.key` kept — `PostCommentListingURL` reads it). In the 43rd the Compose post/comment action menus were wired to live Reddit actions on the Compose data path (no `prepared/*` involvement): `PostListViewModel` gained `PostAction` + `performAction()` — vote (`api/vote`), save/unsave (`api/save`/`api/unsave`), hide/unhide (`api/hide`/`api/unhide`) via the existing `RedditAPI.action` + `ActionResponseHandler`, default account, with a transient `actionResult` `StateFlow` surfaced as a Snackbar and local score/saved/hidden updates on success — and `PostListScreen`'s clickable up/down arrows + overflow menu call the ViewModel (vote/save), `ReportDialog.show` (report, reusing the existing Compose `ReportScreen`), or `LinkHandler.shareText` (share, the OS share sheet with the post permalink). The comment side mirrors this: `CommentListViewModel` gained `CommentAction` + `performAction()` (vote) + the same snackbar plumbing, and `CommentListScreen` has clickable vote arrows, a `ReportDialog` report, and a More → Copy-link (clipboard) action; `RedditComment`/`CommentItem` now carry `name` (the full `t1_…` id), `subreddit`, and `permalink`, which vote/report/share need. In the 44th the comment reply path went live end-to-end (mirroring the 26th's `CommentEdit` pattern): a new `CommentReplyViewModel` issues `RedditAPI.comment` (the legacy flow's exact endpoint + `SubmitResponseHandler`/`ActionResponseHandler` semantics — parent as its full thing id, default account, replies-to-inbox on), `CommentReplyScreen` now owns the submission (spinner while submitting, toast + pop-back on success), the `CommentReply` route carries `parentThingId` (the full `t3_`/`t1_` id) instead of `(postId, commentId?)` — so the comment list's Reply button passes `comment.fullName`, the post header card (real post listings only) is clickable to reply to the post (the post's full `t3_` id), the cold-start `comment_reply` deep link passes the raw id-and-type straight through, and the now-redundant `MainActivityCompose.parseCommentReplyIds` was deleted — and the Inbox "new message" button (which had opened an empty `CommentReply`) now opens the `PMSend` composer, as the legacy inbox's new-message button did. Remaining activities are `MainActivityCompose` (launcher) and `OAuthLoginActivity`, plus the `receivers` (BootReceiver / NewMessageChecker / RegularCachePruner). The repo is 100% Kotlin today (342 .kt, 0 .java — 316 in `src/main`, 19 in `src/test`, 7 in `src/androidTest`).

## Branch
**`dev`** — main development branch (renamed from `java-to-kotlin-conversion`).

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
- `navigation/Screens.kt` — `@Serializable` `NavKey` routes (19); `BugReport` is a child of the Settings top level (opened by the `bug_report` deep link / Settings "Report a bug" row)
- `navigation/NavigationState.kt` / `Navigator.kt` — per-top-level back stacks + navigate/goBack
- `navigation/AppNavigation.kt` — `AppNavGraph(navigationState)` (entryProvider + `NavDisplay`); `AdaptiveNavigation.kt` is the adaptive duplicate — **keep both in lockstep**
- `activities/MainActivityCompose.kt` — launcher; owns `NavigationState` (system back via `baseActivityOnBackPressed`/`baseActivityMustInterceptBack`), cold-start deep links, the `ACTION_SEND` share intent, and external deep-link handling (the app's VIEW intent-filters resolve here; `handleExternalViewIntent` runs on first creation: `redreader://` → OAuth `completeLogin`, anything else → `LinkHandler.onLinkClicked` → `finish()`, the former `LinkDispatchActivity` funnel). Deep-link routes + extras live as `DEEP_LINK_*`/`EXTRA_*` constants there (see the local LLM wiki `deep-link-extras` page for the full map).

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
- The connected (`androidTest`) suite can't use the app's transitive `androidx.startup.InitializationProvider`: its classes are packaged only into the app under test, not the test APK, so the test process dies in `installContentProviders`. `src/androidTest/AndroidManifest.xml` disables that provider node (`android:enabled="false"`) to keep the suite booting. The 3 `StartupBenchmark` macro-benchmark tests always fail on an emulator/debuggable build (the benchmark lib's `EMULATOR DEBUGGABLE NOT-SELF-INSTRUMENTING` guard) — the real connected baseline is the non-benchmark tests passing.
