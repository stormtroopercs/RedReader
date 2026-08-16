# Java → Kotlin Conversion — Error Scope, Progress & Remaining Work

> **Purpose:** Tracks the compile-error scope of the Java → Kotlin conversion,
> the progress made so far, and the work still to be done. Updated as each
> fix-land commit is pushed so the reduction stays auditable.
>
> **Branch:** `java-to-kotlin-conversion`
> **Current HEAD:** `739d185e`
> **Last error count:** **1,501** (`compileDebugKotlin`)
> **Doc updated:** 2026-08-15
> **Toolchain:** Kotlin 2.4.10, AGP 9.3.1, compileSdk/targetSdk 36, JDK 17
> (`JAVA_HOME=/opt/data/jdk-17.0.17+10`)

---

## 1. Headline numbers

| Metric | Baseline (2026-08-14) | Current (2026-08-15) |
|---|---|---|
| `compileDebugKotlin` exit code | `1` (build fails) | `1` (build fails) |
| **Total `e:` error lines** | **2,719** | **1,501** |
| Unique files with ≥1 error | 285 | 238 |
| Production `.kt` files (converted) | 514 | 519 |
| Remaining `.java` files | 16 (all test sources) | 16 (unchanged) |
| Reduction | — | **−1,218 (−44.8%)** |

The build is **not yet green**. All 1,501 errors remain compile-time (Kotlin
front end). `compileDebugKotlin` is the gating task; the 16 Java test sources
are the next gate after it passes.

### Progress trajectory (one commit per logical cluster)

| Commit | Fix | Errors |
|---|---|---|
| `952aba55` | baseline: `RedditAccountManager.getInstance()` Hilt-backed | 2,719 |
| `26b31b41` | Phase 0 — base-class & singleton accessors | 2,719 → 2,575 |
| `b75f3ecf` | restore imports dropped by converter | 2,575 → 2,383 |
| `deb14b41` | Phase 1 — converter mechanical corruption sweep | 2,383 → 2,042 |
| `df4a6d4f` | Phase 3a — getter call-sites → properties (268 sites) | 2,042 → 1,782 |
| `15685f23` | Phase 3b — Java property-setters → Kotlin assignments | → 1,764 |
| `a1bb0745` | Phase 2a — override param nullability → non-null (50 groups / 76 decls) | 1,764 → 1,695 |
| `8944b570` | Phase 2b — de-nullable `Optional<E?>` generic (converter over-nulled) | 1,695 → 1,640 |
| `dc4ac3b5` | Phase 2c — 25 more override param groups → non-null | 1,640 → 1,637 |
| `fb6d0e23` | Phase 3c — `override fun getX()` → `override val x` (72 decls / 44 files) + 72 call-site renames | 1,637 → 1,620 |
| `4b29efa5` | rewire — correct `hiltViewModel` import path (7 files) | 1,620 → 1,607 |
| `739d185e` | rewire — SettingsViewModel + SettingsScreen onto real `PrefsUtility` API (+19 public setters added) | 1,607 → **1,501** |

---

## 2. Current error taxonomy (1,501)

| Category | Count | % | Nature |
|---|---|---|---|
| Type mismatch (incl. `None of the following candidates`) | **447** | 29.8% | Nullable/non-null + API-shape drift |
| Other / long tail | **404** | 26.9% | Diverse (see §4) |
| Unresolved reference | **233** | 15.5% | Mostly the hand-written rewire + dropped members |
| Only safe (`?.`) or non-null asserted (`!!.`) | **151** | 10.1% | Over-nullable *return types* from the converter |
| `'overrides nothing'` | **80** | 5.3% | Residual override signature drift |
| Class not abstract / unimplemented members | **55** | 3.7% | Enum abstract-member backfill + interface-anon |
| Property must be initialized / abstract | **43** | 2.9% | Dropped field inits (SettingsFragment etc.) |
| Access privilege (`Cannot access private`) | **33** | 2.2% | Visibility drift |
| Cannot infer type | **32** | 2.1% | Lost explicit generics |
| Return type not a subtype | **23** | 1.5% | Override return-type drift |

### Split: converted-Java files vs hand-written Compose files

| Bucket | Errors | Files |
|---|---|---|
| Converted-Java sources (legacy codebase) | **1,362** | ~220 |
| Hand-written `navigation/` + `compose/` (new Compose UI) | **139** | 19 |

The hand-written bucket is being actively rewired (see §5); the converted-Java
bucket is the nullability/enum/generics long tail.

### Current top files

| File | Errors | Dominant issues |
|---|---|---|
| `navigation/PostListViewModel.kt` | **46** | phantom cache/parse API (rewire in flight) |
| `reddit/PostSort.kt` | **46** | 40 enum entries missing abstract members + `when` exhaustiveness |
| `settings/SettingsFragment.kt` | **45** | arg mismatches, unresolved refs, cannot-infer generics |
| `common/PrefsUtility.kt` | **44** | mangled `EnumSet`/`noneOf` generics (31), type bounds |
| `reddit/RedditAPI.kt` | **38** | nullability + candidates |
| `navigation/PostListScreen.kt` | **37** | phantom types, `mutableIntStateOf` on enum (rewire planned) |
| `fragments/PostListingFragment.kt` | **30** | nullability, candidates |
| `views/imageview/ImageViewScrollbars.kt` | **29** | must-init + nullability |
| `activities/MainActivity.kt` | **28** | nullability, unresolved |
| `activities/OptionsMenuUtility.kt` | **26** | public exposes private type |

---

## 3. Phases — status

| Phase | Scope | Status |
|---|---|---|
| **0. Base-class & singleton accessors** | `RRFragment.getActivity`, manager companions, dropped statics | ✅ done (`26b31b41`) |
| **1. Converter corruption sweep** | `=`→`>`, `!=`→`!`, `@JvmField` on const, reserved names, bad escapes, imports | ✅ done (`deb14b41`, `b75f3ecf`) |
| **2. Nullability pass** | type mismatches, safe-calls, override param nullability | 🔄 mostly done — bulk passes landed (`a1bb0745`, `8944b570`, `dc4ac3b5`); remaining ~151 safe-call-on-nullable + tail of the 447 type mismatches folded into Phases 4/5 (need per-file original-Java nullability checks vs `2479dfa0^`) |
| **3. Getter/setter → property + overrides** | call-site renames, `getX()`→`x` overrides | ✅ done (`df4a6d4f`, `15685f23`, `fb6d0e23`) |
| **4. Enums, interfaces, generics** | `PostSort`/`PostCommentSort`/`UserCommentSort` abstract-member backfill (55), `PrefsUtility` `EnumSet` bounds (44), interface-anon, cannot-infer (32) | ⏳ next up |
| **5. Long-tail per-file** | visibility (33), must-init (43), return-subtype (23), safe-call null semantics (151), candidates (rest of 447) | ⏳ pending (case-by-case, original-Java reference at `2479dfa0^`) |
| **6. Iterate to green** | re-compile, fix cascades | ⏳ pending |
| **7. Test sources (still Java)** | 16 `.java` test/androidTest files | ⏳ separate gate |
| **R. Rewire hand-written Compose files** | 19 `navigation/`+`compose/` files onto the real converted API | 🔄 in flight (139 errors left, see §5) |

---

## 4. Known root causes (verified)

These were root-caused with the Kotlin 2.4.10 oracle (`kt_oracle.py`) and the
original Java (`git show 2479dfa0^:<path>`):

- **Override param nullability is invariant** — a child cannot narrow a base
  param from `T?` to `T`. The converter over-nulled *bases*; the fix is to
  standardize base + all implementors to non-null (body-safe: safe-calls
  degrade to warnings). 50 groups + 25 groups already done; 80
  `overrides nothing` remain (incl. 2 manual framework cases:
  `getWorkManagerConfiguration` — androidx `Configuration.Provider`;
  `asObject` — `JsonDeserializable`).
- **`Optional<E?>` was converter damage** — original Java `Optional<E>` held a
  nullable value but a non-null type param. De-nulled (`8944b570`).
- **Method→property overrides** — converter turned base `fun getX()` into
  `val x` while children kept `override fun getX()`; 72 converted
  (`fb6d0e23`). `is*` properties keep the full name (`override val isHidden`).
- **`hiltViewModel` import** — hand-written files used the non-existent package
  `androidx.hilt.lifecycle.viewmodel.compose`; the real one (verified in the
  jar: `androidx/hilt/navigation/compose/HiltViewModelKt`) is
  `androidx.hilt.navigation.compose` (`4b29efa5`).
- **Hand-written Compose files reference a settings API that was never
  built.** `navigation/`+`compose/` were written against a *planned* settings
  API (camelCase `prefLinkbuttons`, `prefPostsSort`, phantom enums
  `ImageQuality`/`VideoQuality`/`GifQuality`, `CommentTapAction`,
  `ImageViewerMode`, `AlbumViewerMode`, `SortType`…) that exists in neither
  the original Java nor the converted Kotlin. Real API = snake_case `pref_*`
  no-arg functions on the `PrefsUtility` object + real enums
  (`PostSort`, `PostCommentSort`, `CommentAction`, `ImageViewMode`,
  `AlbumViewMode`, `AppearanceTheme`, `AppearanceStatusBarMode`,
  `GifViewMode`, `VideoViewMode`, `NeverAlwaysOrWifiOnly`). **User decision
  (2026-08-15): rewire the hand-written files to the real converted API —
  map to closest existing setting or drop; do NOT build the missing API.**
- **`PrefsUtility` had no programmatic setters** (the original app persisted
  prefs via the Preference-XML framework; only 2 programmatic setters existed).
  19 public `prefX_set(...)` methods were added (`739d185e`) mirroring the
  existing `set_pref_behaviour_notifications` write pattern.
- **Safe-call-on-nullable tail (~151)** — over-nullable *return types*
  (e.g. `RedditAccountManager.getDefaultAccount(): RedditAccount?` can
  genuinely return null despite a non-null Java declaration). Needs
  case-by-case call-site null-handling, not blind de-nullable.

---

## 5. Rewire workstream — hand-written Compose files (139 errors)

| File | Errors | State |
|---|---|---|
| `navigation/SettingsViewModel.kt` | 0 (was 44) | ✅ rewritten onto real `PrefsUtility` getters/setters |
| `navigation/SettingsScreen.kt` | 4 (was 66) | ✅ rewritten (inline mini-framework was internally broken: reserved `__` name, builder≠data-class, `override` on non-open base, `enumConstants` reflection, phantom types); 4 residuals left |
| `navigation/PostListViewModel.kt` | **46** | 🔄 **rewrite in flight** — built on phantom cache API: `RedditURLParser.parse(String)` (real takes `Uri`), `CacheRequest(...)` private ctor, callback-less `makeRequest`, phantom `CacheRequest.Result.*`. Real flow to mirror (verified in `PostListingFragment`): `CacheRequest(UriString, user, session, Priority(API_POST_LIST), strategy, FileType.POST_LIST, DownloadQueueType.REDDIT_API, cache, context, CacheRequestCallbacks{ onDataStreamAvailable/Complete/onFailure })` → `decodeRedditThingFromStream(streamFactory.create())` → `RedditThing.Listing` → `listing.children: ArrayList<MaybeParseError<RedditThing?>>` → `RedditThing.Post.data` (`RedditPost` fields: `id`, `title: UrlEncodedString?`, `author: UrlEncodedString?`, `score: Int`, `num_comments: Int`, `permalink: UrlEncodedString`, `is_self`, `over_18`, `spoiler`, `stickied`, `locked`, `selftext: UrlEncodedString?`, `link_flair_text: UrlEncodedString?`, `created_utc: RedditTimestampUTC`, `findUrl(): UriString?`, `is_video`, `gallery_data`) |
| `navigation/PostListScreen.kt` | **37** | 📋 planned (paired with the ViewModel rewire: consumes its `PostItem`); also fixes `PrefsUtility.prefPostsSort(context)` → `pref_behaviour_postsort()`, `PrefsUtility.SortType.*` → `PostSort.*`, `mutableIntStateOf` on enum ordinal |
| `navigation/UserProfileViewModel.kt` | 9 | ⏳ small API fixes |
| `navigation/CommentListViewModel.kt` | 7 | ⏳ small API fixes (mirror of the PostList cache flow with `FileType.COMMENT_LIST`) |
| `compose/ui/OAuthLoginScreen.kt` | 6 | ⏳ small |
| `compose/ui/UserProfileScreen.kt` | 6 | ⏳ small |
| `compose/ui/UserProfileDialog.kt` (nav) | 3 | ⏳ small |
| `navigation/AdaptiveNavigation.kt` / `AppNavigation.kt` | 3+3 | ⏳ named-arg drift + imports |
| `compose/ctx/RRComposeContext.kt`, `compose/net/NetWrapper.kt`, `compose/ui/UserPropertiesDialog.kt`, `navigation/CommentListScreen.kt`, `navigation/MainScreen.kt`, `navigation/Navigator.kt`, `compose/ui/RedditTermsScreen.kt`, `navigation/InboxViewModel.kt`, `navigation/NavigationState.kt` | 1–2 each | ⏳ mostly mechanical imports |

**Rewire name map (missing → real):**

| Phantom (hand-written) | Real (converted) |
|---|---|
| `prefLinkbuttons(context)` | `PrefsUtility.pref_appearance_linkbuttons()` |
| `prefPostsSort(context)` | `PrefsUtility.pref_behaviour_postsort()` → `PostSort` |
| `prefTheme` / `prefAppearanceTheme` | `PrefsUtility.appearance_theme()` → `AppearanceTheme` |
| `SortType.BEST/HOT/NEW/TOP/RISING` | `PostSort.BEST/HOT/NEW/TOP/HOUR…` (entries verified in `reddit/PostSort.kt`) |
| `ImageViewerMode` | `ImageViewMode` |
| `AlbumViewerMode` | `AlbumViewMode` (top-level in `settings/types/AlbumViewMode.kt`) |
| `CommentTapAction` | `CommentAction` via `pref_behaviour_actions_comment_tap()` |
| `imageQuality` / `ImageQuality` | closest: `images_high_res_thumbnails(): NeverAlwaysOrWifiOnly` |
| `videoQuality` / `gifQuality` | **dropped** (no quality enum exists anywhere) |
| `hiltViewModel` (import) | `androidx.hilt.navigation.compose.hiltViewModel` |

---

## 6. Remaining work, in order

1. **Finish the PostList pair rewire** (`PostListViewModel` 46 + `PostListScreen`
   37) — the real cache flow is fully mapped (see §5); one commit.
2. **Small hand-written fixes** (~56 errors across 13 files) — mostly
   imports + named-args; one commit.
3. **Phase 4: enums + generics** — `PostSort`/`PostCommentSort`/`UserCommentSort`
   abstract-member backfill (55), `PrefsUtility` `EnumSet`/`noneOf` bounds (44),
   cannot-infer (32), interface-anon remainder. One coordinated pass
   (enum changes are API-visible — touches `when` sites across the app).
4. **Phase 5: long tail** — the 151 safe-call-on-nullable errors need
   per-file original-Java nullability checks (`git show 2479dfa0^:<path>`);
   type-mismatch remainder (≈300 after Phase 4); must-init (43), access
   (33), return-subtype (23).
5. **Phase 6: iterate to green** `compileDebugKotlin`.
6. **Phase 7: 16 Java test sources** — the next gate after green compile.

Revised estimate to green `compileDebugKotlin`: **~5–8 working days**
(the original 10–17 estimate was pre-Phases 0–3; the mechanical 830-error
block is already gone, and the rewire workstream removes ~258 of the rest).

---

## 7. Risks & cautions (unchanged, still apply)

- **Do NOT blind-rewrite from the original Java.** The current Kotlin API
  surface has evolved; verify what each file currently calls first. Simple
  utilities with no callers are safe to rewrite; complex ones with callers
  need careful analysis.
- **Receiver-aware renames only** — confirm the receiver type has the
  property before `getFoo()` → `foo`.
- **Cascading errors** — fixing a base class changes the error set; the count
  may rise before falling. Re-baseline after each pass.
- **Enum changes are API-visible** — do `PostSort`/`PostCommentSort`/
  `UserCommentSort` in one coordinated pass with their `when` sites.
- **`SettingsFragment` is a single-file mountain** (45 left) — isolate if it
  gets risky.
- **`execute_code` sandbox `/tmp` ≠ terminal `/tmp`** — run build/log analysis
  in `terminal`; copy artifacts into the repo if the sandbox needs them.

## 8. Tooling in-repo

| Script | Purpose |
|---|---|
| `build_check.sh` | compile + count: `bash build_check.sh` → `exit=1 total_errors=N` (handles `JAVA_HOME`) |
| `kt_oracle.py` | Kotlin 2.4.10 pattern oracle — `from kt_oracle import kc` — compile-check candidate forms before bulk edits |
| `fix_getters.py` | bulk `get*()` call-site → property renames (receiver-aware, log-driven) |
| `fix_override_null.py` / `fix_override_null2.py` | base+implementor override-param nullability standardizers |
| `plan_ov5.py` | override-error planner (multi-line decls, BFS base resolver) → JSON plans |
| `apply_propconvert.py` | `override fun getX()` → `override val x` converter |
| `extract_nohint*.py`, `analyze_argmismatch*.py`, `analyze_overrides.py` | log analysis helpers |

## 9. Reproduction

```bash
cd /opt/data/RedReader
export JAVA_HOME=/opt/data/jdk-17.0.17+10
./gradlew compileDebugKotlin --no-daemon 2>&1 | tee /tmp/compile.log
grep -c '^e:' /tmp/compile.log
grep '^e:' /tmp/compile.log | sed 's/.*redreader\///; s/:.*//' | sort | uniq -c | sort -rn   # per-file
```

Original-Java semantics reference: `git show 2479dfa0^:<path>`.

---

*Appendices A–C of the original 2026-08-14 scope doc (unresolved-reference
histogram, per-category file spread, reproduction) remain valid as the
baseline record; current per-file numbers are in §2 above.*
