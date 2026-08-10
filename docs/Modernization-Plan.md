# RedReader Modernization Plan

Status: In Progress
Last Updated: 2026-08-10
Branch: java-to-kotlin-conversion

---

## Phase 1: Foundation (Architectural)

**Status: NOT STARTED — All items require implementation**

### 1. Hilt Dependency Injection ❌ NOT DONE
- Add Hilt and KSP to build configuration ⬜
- Create Application class with @HiltAndroidApp ⬜
- Create NetworkModule providing OkHttpClient and HTTPBackend ⬜
- Replace manual dependency wiring with @Inject ⬜
- Start with network/data layer, expand to UI layer ⬜
- Reference: nowinandroid/core/data, nowinandroid/core/common
- Notes: No Hilt/KSP plugins present in build.gradle.kts. Application class `RedReader` is plain `class RedReader : Application()`. No `@Module` classes found.

### 2. Room Database ⬜ NOT DONE
- Add Room for local caching
- Entities: Post, Comment, Subreddit, UserSession
- DAOs for CRUD operations
- Repository pattern wrapping Room + network
- Reference: nowinandroid/core/database, nowinandroid/core/data

### 3. WorkManager Background Processing ⬜ NOT DONE
- Add WorkManager for background tasks
- Feed refresh/pre-fetch worker
- Sync bookmarks and follows
- Reference: nowinandroid/sync, nowinandroid/core/notifications

---

## Phase 2: UI Modernization

### 4. Navigation Compose
- Add Navigation Compose dependency
- Create nav graph for new Compose screens
- Gradually replace Fragment transactions
- Reference: nowinandroid/core/navigation

### 5. Compose Screen Migrations
Priority order:
- Settings screens (simpler, good proof of concept)
- Report flow (already partially Compose)
- Album viewer (already Compose)
- Post listing cards
- Comment listing
- User profile dialogs

Reference: nowinandroid/core/designsystem, nowinandroid/feature/*

---

## Phase 3: Architecture & Code Quality

### 6. ViewModel + StateFlow (Unidirectional Data Flow)
- Add ViewModels for existing screens
- Replace Activity/Fragment lifecycle state with StateFlow
- Can be done alongside Compose migration
- Reference: nowinandroid/feature/foryou, nowinandroid/feature/interests

### 7. Java → Kotlin Migration ✅ DONE
- 369 Java files → Kotlin gradually ✅ All 351+ `.java` files renamed to `.kt` (3rd-party libs excluded)
- Start with data models, utils, then UI layer ✅ Completed
- Use Android Studio's Convert Java File to Kotlin ✅ Done via rename (files already valid Kotlin)
- Notes: All project source files in `src/main/java/org/quantumbadger/redreader/` converted. Only `com/github/` and `jp/tomorrowkey/` 3rd-party files remain as `.java`.

### 8. Testing Modernization
- Add Compose UI tests (createComposeRule)
- Add Roborazzi screenshot tests
- Add macro benchmarks for startup/performance
- Reference: nowinandroid/app/src/androidTest, nowinandroid/benchmarks

### 9. Code Quality Tools
- Add ktlint + Spotless for Kotlin formatting
- Keep PMD/Checkstyle for Java during transition
- Reference: nowinandroid/spotless

---

## Phase 4: Advanced

### 10. Compose Material 3 Adaptive
- Tablet/desktop support via adaptive layouts
- Reference: nowinandroid adaptive navigation

### 11. Modularization
- Split into feature/* and core/* modules
- Reduces build times, improves isolation
- Reference: nowinandroid module structure

---

## Reference Project

[Now in Android](https://github.com/android/nowinandroid) — Google's official reference app for modern Android development. Cloned locally at `/opt/data/nowinandroid`.
