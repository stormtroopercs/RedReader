# Java → Kotlin Conversion — Error Scope, Progress & Remaining Work

> **Purpose:** Tracks the compile-error scope of the Java → Kotlin conversion,
> the progress made so far, and the work still to be done. Updated as each
> fix-land commit is pushed so the reduction stays auditable.
>
> **Branch:** `java-to-kotlin-conversion`
> **Current HEAD:** `0091e5a9` (all work pushed to origin)
> **Last error count:** **1,101** (`compileDebugKotlin`)
> **Doc updated:** 2026-08-16
> **Toolchain:** Kotlin 2.4.10, AGP 9.3.1, compileSdk/targetSdk 36, JDK 17
> (`JAVA_HOME=/opt/data/jdk-17.0.17+10`)

---

## 1. Headline numbers

| Metric | Baseline (2026-08-14) | Current (2026-08-16) |
|---|---|---|
| `compileDebugKotlin` exit code | `1` (build fails) | `1` (build fails) |
| **Total `e:` error lines** | **2,719** | **1,101** |
| Unique files with ≥1 error | 285 | 211 |
| Production `.kt` files (converted) | 514 | 519 |
| Remaining `.java` files | 16 (all test sources) | 16 (unchanged) |
| Reduction | — | **−1,618 (−59.5%)** |

The build is **not yet green**. All 1,101 errors remain compile-time (Kotlin
front end). `compileDebugKotlin` is the gating task; the 16 Java test sources
are the next gate after it passes.

**Headline milestone (this session):** the hand-written `navigation/` +
`compose/` bucket (19 files) is now at **0 errors** — the entire rewire
workstream (Phase R) is **DONE**. Every remaining error is in converted-Java
sources (plus 10 in the vendored `gifplayer/GifDecoder.kt`).

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
| `739d185e` | rewire — SettingsViewModel + SettingsScreen onto real `PrefsUtility` API (+19 public setters added) | 1,607 → 1,501 |
| `2bf1f73c` | rewire — PostListViewModel + PostListScreen onto the real cache/listing API (+ AGENTS.md Hilt package fix) | 1,501 → 1,412 |
| `fb5a86c6` | rewire — **all** hand-written `navigation/`+`compose/` files (19) | 1,412 → 1,362 (**hand-written bucket = 0**) |
| `9cfad99d` | `Sort` interface + 3 sort enums + `OptionsMenuUtility` (array covariance / visibility / `Collections.sort` / `defaultAccount`) + `AppbarItemsPref` `EnumMap` chain | 1,362 → 1,254 |
| `eb7d8a35` | `PrefsUtility` 10 `EnumSet<E?>` de-null + cache-age `HashMap` chain (`createFileTypeMap`/`pref_cache_maxage`/`pruneCache`) | 1,254 → 1,203 |
| `3b2a69cd` | `asObject`/`getObject` over-nulled generics (base + override) + 5 call-site `<RedditThing?>` type args | 1,203 → 1,196 |
| `f071b63d` | `UserPostListingURL` duplicate `order` decl (3-way conflict) + 3 return-type `?` | 1,196 → 1,174 |
| `41c573ae` | `WritableHashSet` duplicate `key`/`timestamp` backing fields (annotation-discovered) | 1,174 → 1,155 |
| `9662d8fc` | `MarkdownTokenizer` 20 `Char`→`Int` `when`-branch labels + null-safe `reverseLookup` table | 1,155 → 1,130 |
| `0091e5a9` | `ImageViewScrollbars` `val`-in-`run{}` init fields + `@Synchronized` getter target | 1,130 → **1,101** |

---

## 2. Current error taxonomy (1,101)

| Category | Count | % | Nature |
|---|---|---|---|
| Type mismatch / argument (incl. `None of the following candidates`, `Condition type`) | **362** | 32.9% | Nullable/non-null + API-shape drift |
| Other / long tail (access-weaken, inner-class ctor, `Optional` return, `break`/`continue`, `when`-exhaustive, smart-cast, etc.) | **171** | 15.5% | Diverse (see §4.3) |
| Only safe (`?.`) on nullable receiver | **145** | 13.2% | Over-nullable *return types* from the converter |
| Unresolved reference | **143** | 13.0% | Mostly the hand-written rewire (now done) + dropped members |
| `overrides nothing` | **79** | 7.2% | Over-nulled interface family (§4.1) |
| Class not abstract / unimplemented members | **55** | 5.0% | Interface-anon + enum backfill residue |
| Access privilege (`Cannot access private` / `Cannot weaken protected`) | **27 + ~30** | ~5% | Visibility drift |
| Property must be initialized / abstract | **25** | 2.3% | Dropped field inits (SettingsFragment etc.) |
| Overload resolution ambiguity | **21** | 1.9% | Nullability overloads |
| Return type not a subtype | **20** | 1.8% | Override return-type drift |
| `val` cannot be reassigned | **12** | 1.1% | Java mutable field → `val` |
| Cannot infer type | **13** | 1.2% | Lost explicit generics |
| Type argument out of bounds / conflicting decls | **9 + 4** | ~1% | Generic bounds + duplicate decls |

### Split: converted-Java files vs hand-written Compose files

| Bucket | Errors | Files |
|---|---|---|
| Converted-Java sources (`org/quantumbadger/redreader`) | **1,091** | 210 |
| Vendored `jp/tomorrowkey/gifplayer` | **10** | 1 (`GifDecoder.kt`) |
| Hand-written `navigation/` + `compose/` (new Compose UI) | **0** | 19 ✅ **DONE** |

The hand-written rewire workstream is complete. The remaining 1,101 is the
converted-Java nullability / interface / generics long tail.

### Current top files

| File | Errors | Dominant issues |
|---|---|---|
| `settings/SettingsFragment.kt` | **45** | arg mismatches, unresolved refs, cannot-infer generics, must-init |
| `reddit/RedditAPI.kt` | **36** | nullability + `performRequest` overrides + candidates |
| `fragments/PostListingFragment.kt` | **30** | nullability, candidates, safe-call |
| `activities/MainActivity.kt` | **28** | nullability, unresolved |
| `fragments/CommentListingFragment.kt` | **25** | nullability, safe-call |
| `reddit/api/RedditOAuth.kt` | **23** | missing imports (`RedditAccountManager`, `RequestFailureType`), `RefreshToken?` safe-call, token-result-status type mismatch |
| `adapters/MainMenuListingManager.kt` | **22** | nullability, candidates |
| `fragments/postsubmit/PostSubmitContentFragment.kt` | **21** | nullability, candidates |
| `common/FeatureFlagHandler.kt` | **19** | nullable `getStringSet` + over-nulled `putStringSet` + private `id` |
| `reddit/api/RedditSubredditSubscriptionManager.kt` | **18** | nullability |
| `views/imageview/ImageViewDisplayListManager.kt` | **18** | nullable-arg (incl. 1 `ImageViewScrollbars` fallout) |

---

## 3. Phases — status

| Phase | Scope | Status |
|---|---|---|
| **0. Base-class & singleton accessors** | `RRFragment.getActivity`, manager companions, dropped statics | ✅ done (`26b31b41`) |
| **1. Converter corruption sweep** | `=`→`>`, `!=`→`!`, `@JvmField` on const, reserved names, bad escapes, imports | ✅ done (`deb14b41`, `b75f3ecf`) |
| **2. Nullability pass** | type mismatches, safe-calls, override param nullability | ✅ bulk done (`a1bb0745`, `8944b570`, `dc4ac3b5`); remaining folded into Phases 4/5 |
| **3. Getter/setter → property + overrides** | call-site renames, `getX()`→`x` overrides | ✅ done (`df4a6d4f`, `15685f23`, `fb6d0e23`) |
| **4. Enums, interfaces, generics** | sort enums + `AppbarItemsPref` (`9cfad99d`), `PrefsUtility` `EnumSet` (`eb7d8a35`), `asObject`/`getObject` (`3b2a69cd`) | 🔄 mostly done — **remaining: the over-nulled `RequestResponseHandler`/`CacheDataSource`/`CacheRequestJSONParser.Listener` interface family (79 `overrides nothing` + 55 not-abstract ≈ 134)** |
| **5. Long-tail per-file** | visibility, must-init, return-subtype, safe-call (145), type-mismatch (362), candidates | 🔄 in progress — case-by-case, original-Java reference at `2479dfa0^` |
| **6. Iterate to green** | re-compile, fix cascades | ⏳ pending |
| **7. Test sources (still Java)** | 16 `.java` test/androidTest files | ⏳ separate gate |
| **R. Rewire hand-written Compose files** | 19 `navigation/`+`compose/` files onto the real converted API | ✅ **DONE** (`2bf1f73c` + `fb5a86c6`) — bucket = 0 errors |

---

## 4. Known root causes (verified)

### 4.1 Over-nulled interface family (biggest single remaining cause)

The converter over-nulled the base interfaces `RequestResponseHandler`
(original Java: `onRequestSuccess(E, TimestampUTC)` / `onRequestFailed(F)`, all
non-null), `CacheDataSource` (original Java:
`performRequest(K, TimestampBound, RequestResponseHandler<V,F>)`), and
`CacheRequestJSONParser.Listener`. Because **Kotlin override params are
invariant**, the over-nulled base signatures don't match any impl → **79
`overrides nothing` + 55 "not abstract / unimplemented"** across ~30 files.
The impls are *split* (some already non-null, some nullable), so the fix is to
de-null the three base interfaces to match original Java **and** normalize the
impl signatures — a large, delicate, interconnected multi-file change.
**This is the highest-leverage remaining root cause but the riskiest; do it as
a coordinated, compiler-verified pass.**

### 4.2 Proven over-nulled-generic fixes (all landed this session)

- **`Sort` interface** — original Java `String name()` (non-null) is satisfied
  for free by `Enum.name()`; the converter wrote `fun name(): String?`
  (unimplementable by Kotlin enums, whose `name` is a final `val`). Faithful
  form = `val name: String`. Also `menuTitle` single declaration (constructor
  `override val menuTitle`, no separate `get()` duplicate). `SortGroup`
  `Array<out Sort>` for covariance; `instanceof PostSort[]` →
  `sorts.firstOrNull() is PostSort`. (`9cfad99d`)
- **`EnumSet<E?>` / `EnumMap<K?,V?>`** — `EnumSet<E>` requires
  `E : Enum<E!>!`, so `EnumSet<E?>` violates the bound. De-nulled 10
  `PrefsUtility` functions + `AppbarItemsPref` chain. (`eb7d8a35`)
- **`asObject`/`getObject`** — original Java `<E extends JsonDeserializable>
  E asObject(Class<E>)` / `getObject(...)`; converter over-nulled to
  `<E : JsonDeserializable?> … (Class<E?>?) : E?`. De-nulled base + `JsonObject`
  override + 5 `<RedditThing?>` call-site type args. (`3b2a69cd`)
- **`Optional<E?>` was converter damage** — de-nulled (`8944b570`).

### 4.3 Long-tail categories (Phase 5, case-by-case vs `2479dfa0^`)

- **Duplicate-declaration bug class** — a converter artifact that creates a
  backing field *and* an `override val` with the same name (e.g.
  `UserPostListingURL.order`, `WritableHashSet.key`/`timestamp`, sort-enum
  `menuTitle`). Fix = one declaration. (`f071b63d`, `41c573ae`)
- **`Char` vs `Int` `when` labels** — Java `int c = str.charAt(i)` → Kotlin
  `Char`, but `when(intVar)` needs `.code`. (`9662d8fc`)
- **`val`-in-`run{}` init** — Kotlin forbids initializing a captured `val`
  member inside a `run{}` lambda; drop the `run{}` wrappers. (`0091e5a9`)
- **Safe-call-on-nullable (145)** — over-nullable *return types* (e.g.
  `RedditAccountManager.getDefaultAccount(): RedditAccount?`). Needs
  case-by-case call-site null-handling, not blind de-nullable.
- **Missing imports** — converter dropped same-package-adjacent imports
  (e.g. `RedditOAuth`: `RedditAccountManager` =
  `org.quantumbadger.redreader.account`, `RequestFailureType` =
  `cache/CacheRequest`).
- **`hiltViewModel` import** — real package is
  `androidx.hilt.navigation.compose` (verified in the jar), not the
  non-existent `androidx.hilt.lifecycle.viewmodel.compose`. (`4b29efa5`)
- **Hand-written Compose files referenced a never-built settings API** —
  rewired to the real converted `PrefsUtility` snake_case `pref_*` API (see
  §5 name map). User decision 2026-08-15: rewire, do **not** build the missing
  API.

---

## 5. Rewire workstream — hand-written Compose files ✅ DONE

All 19 `navigation/` + `compose/` files are now at **0 errors**
(`2bf1f73c` + `fb5a86c6`). Rewire name map (missing → real) retained for
reference:

| Phantom (hand-written) | Real (converted) |
|---|---|
| `prefLinkbuttons(context)` | `PrefsUtility.pref_appearance_linkbuttons()` |
| `prefPostsSort(context)` | `PrefsUtility.pref_behaviour_postsort()` → `PostSort` |
| `prefTheme` / `prefAppearanceTheme` | `PrefsUtility.appearance_theme()` → `AppearanceTheme` |
| `SortType.BEST/HOT/NEW/TOP/RISING` | `PostSort.BEST/HOT/NEW/TOP/HOUR…` |
| `ImageViewerMode` | `ImageViewMode` |
| `AlbumViewerMode` | `AlbumViewMode` (top-level in `settings/types/AlbumViewMode.kt`) |
| `CommentTapAction` | `CommentAction` via `pref_behaviour_actions_comment_tap()` |
| `imageQuality` / `ImageQuality` | closest: `images_high_res_thumbnails(): NeverAlwaysOrWifiOnly` |
| `videoQuality` / `gifQuality` | **dropped** (no quality enum exists) |
| `hiltViewModel` (import) | `androidx.hilt.navigation.compose.hiltViewModel` |

---

## 6. Remaining work, in order

1. **Interface-family de-nulling cascade** — the over-nulled
   `RequestResponseHandler` / `CacheDataSource` / `CacheRequestJSONParser.
   Listener` base interfaces (79 `overrides nothing` + 55 not-abstract ≈ 134,
   ~30 files). Highest single leverage; do as a coordinated, compiler-verified
   pass against original Java (`git show 2479dfa0^:<path>`). **Decide first:
   tackle now vs defer to the per-file long tail** — the impls are split
   nullability, so it's the riskiest remaining change.
2. **Phase 5: per-file long tail** (top files in §2): `SettingsFragment` (45),
   `RedditAPI` (36), `PostListingFragment` (30), `MainActivity` (28),
   `CommentListingFragment` (25), `RedditOAuth` (23), … Each case-by-case vs
   original-Java nullability.
3. **Phase 6: iterate to green** `compileDebugKotlin`.
4. **Phase 7: 16 Java test sources** — the next gate after green compile.

Revised estimate to green `compileDebugKotlin`: **~4–6 working days**
(the mechanical ~830-error block is gone, the ~258-error hand-written rewire
is gone, and the sort/generics clusters are done; what remains is the
interface cascade + per-file nullability tail).

---

## 7. Risks & cautions (still apply)

- **Do NOT blind-rewrite from the original Java.** The current Kotlin API
  surface has evolved; verify what each file currently calls first. Simple
  utilities with no callers are safe to rewrite; complex ones with callers
  need careful analysis.
- **Over-reach caution (NEW):** de-nulling a generic's *whole* chain breaks
  other callers that were *self-consistently nullable*. E.g. de-nulling
  `createFileTypeMap`/`pref_cache_maxage` initially broke `CacheManager`
  (which stored the result in `HashMap<Int?, TimeDuration?>?`); the faithful
  end state de-nulled the whole cache-age subgraph (original Java was
  non-null throughout), but the lesson holds — re-baseline after each pass
  and confirm against `2479dfa0^` before committing.
- **Receiver-aware renames only** — confirm the receiver type has the
  property before `getFoo()` → `foo`.
- **Cascading errors** — fixing a base class changes the error set; the count
  may rise before falling. Re-baseline after each pass.
- **Enum changes are API-visible** — sort enums done; any further enum work
  should be coordinated with their `when` sites.
- **`SettingsFragment` is a single-file mountain** (45 left) — isolate if it
  gets risky.
- **Oracle gotcha** — `/tmp/kc_cp.txt` must contain
  `kotlin-compiler-embeddable` + `kotlin-stdlib` + annotations +
  `kotlinx-coroutines`; a missing classpath → `ClassNotFoundException` → errors
  silently suppressed (a stale `-no-stdlib` flag also hides stdlib/`java.util`
  symbols). Rebuild the classpath before trusting any oracle result.
- **`execute_code` sandbox `/tmp` ≠ terminal `/tmp`** — run build/log analysis
  in `terminal`; terminal `/tmp` can be wiped (rebuild `/tmp/compile.log` +
  `/tmp/kc_cp.txt` when missing).

## 8. Tooling in-repo

Conversion-era build tooling. The **reusable** helpers remain in `scripts/`; the
**conversion-specific** scripts (the `fix_*`/`analyze_*`/`plan_ov5`/`scope_analysis`/`blast_radius*`
log-driven transforms + analyzers pinned to this migration's error families) were archived to
`redreader-project/tmp/scripts-archive/` after the conversion completed.

| Script | Purpose |
|---|---|
| `scripts/build_check.sh` | compile + count: `bash scripts/build_check.sh [FILE_FILTER]` → `TOTAL: N` (handles `JAVA_HOME`); optional per-file filter prints that file's `e:` lines |
| `scripts/verify_unit.sh` | compile + flag residual `e:` errors in `git diff` files |
| `scripts/compile_count.sh` | compile + total + per-file top-15 counts |
| `scripts/parse_errors.py` | parse `/tmp/compile.log` → per-file error counts (top 25) |
| `scripts/summarize_compile.sh` | total + per-file counts from a compile log |
| `scripts/kt_oracle.py` | Kotlin 2.4.10 pattern oracle — `from kt_oracle import kc` — compile-check candidate forms before bulk edits (classpath in `/tmp/kc_cp.txt`) |
| `tmp/scripts-archive/fix_getters.py` | bulk `get*()` call-site → property renames (receiver-aware, log-driven) |
| `tmp/scripts-archive/fix_override_null.py` / `fix_override_null2.py` | base+implementor override-param nullability standardizers |
| `tmp/scripts-archive/plan_ov5.py` | override-error planner (multi-line decls, BFS base resolver) → JSON plans |
| `tmp/scripts-archive/apply_propconvert.py` | `override fun getX()` → `override val x` converter |
| `tmp/scripts-archive/extract_nohint*.py`, `analyze_argmismatch*.py`, `analyze_overrides.py` | log analysis helpers |
| `tmp/scripts-archive/scope_analysis.py`, `analyze_clusters.py`, `blast_radius*.sh` | error-scoping / cluster-analysis / blast-radius probes |

## 9. Reproduction

```bash
cd /opt/data/redreader-project/RedReader
export JAVA_HOME=/opt/data/tools/jdk-17.0.17+10
./gradlew compileDebugKotlin --no-daemon > /tmp/compile.log 2>&1
grep -c '^e:' /tmp/compile.log
grep '^e:' /tmp/compile.log | sed 's|file:///opt/data/redreader-project/RedReader/src/main/java/org/quantumbadger/redreader/||; s|\.kt:.*||' | sort | uniq -c | sort -rn   # per-file
```

Original-Java semantics reference: `git show 2479dfa0^:<path>`.

---

*Appendices A–C of the original 2026-08-14 scope doc (unresolved-reference
histogram, per-category file spread, reproduction) remain valid as the
baseline record; current per-file numbers are in §2 above.*
