# RedReader Modernization Plan

Status: In Progress
Last Updated: 2026-08-10
Branch: java-to-kotlin-conversion

---

## Phase 1: Foundation (Architectural)

**Status: IN PROGRESS — Core infrastructure complete, wiring in progress**

### 1. Hilt Dependency Injection ✅ PARTIALLY DONE
- Add Hilt and KSP to build configuration ✅ DONE
- Create Application class with @HiltAndroidApp ⬜ TODO
- Create NetworkModule providing OkHttpClient and HTTPBackend ✅ DONE
- Replace manual dependency wiring with @Inject ⬜ TODO
- Start with network/data layer, expand to UI layer ✅ DONE (network layer)
- Reference: nowinandroid/core/data, nowinandroid/core/common
- Notes: Hilt 2.56.2 added. KSP processor enabled. Application class `RedReader` still needs `@HiltAndroidApp`. NetworkModule provides OkHttpClient and HTTPBackend. RepositoryModule and RedReaderModule consolidated into ApplicationModule and DatabaseModule.

### 2. Room Database ✅ DONE
- Add Room for local caching ✅ DONE
- Entities: Post, Comment, Subreddit, UserSession ✅ DONE
- DAOs for CRUD operations ✅ DONE
- Repository pattern wrapping Room + network ⬜ TODO
- Reference: nowinandroid/core/database, nowinandroid/core/data
- Notes: Room 2.6.1 added. Entities: PostEntity, CommentEntity, SubredditEntity, UserSessionEntity. DAOs: PostDao, CommentDao, SubredditDao, UserSessionDao. DatabaseModule provides all DAOs via Hilt.

### 3. WorkManager Background Processing ✅ DONE
- Add WorkManager for background tasks ✅ DONE
- Feed refresh/pre-fetch worker ✅ DONE
- Sync bookmarks and follows ✅ DONE
- Reference: nowinandroid/sync, nowinandroid/core/notifications
- Notes: WorkManager 2.10.0 added. Workers: NewMessageWorker, CachePrunerWorker, FeedRefreshWorker, SyncBookmarksWorker. WorkManagerInitializer configures all workers.

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
