# RedReader Modernization Plan

Status: In Progress
Last Updated: 2026-08-08
Branch: material3-expressive-migration

---

## Phase 1: Foundation (Architectural)

### 1. Hilt Dependency Injection ✅ DONE
- Add Hilt and KSP to build configuration ✅
- Create Application class with @HiltAndroidApp ✅
- Create NetworkModule providing OkHttpClient and HTTPBackend ✅
- Replace manual dependency wiring with @Inject (ongoing)
- Start with network/data layer, expand to UI layer
- Reference: nowinandroid/core/data, nowinandroid/core/common

### 2. Room Database
- Add Room for local caching
- Entities: Post, Comment, Subreddit, UserSession
- DAOs for CRUD operations
- Repository pattern wrapping Room + network
- Reference: nowinandroid/core/database, nowinandroid/core/data

### 3. WorkManager Background Processing
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

### 7. Java → Kotlin Migration
- 369 Java files → Kotlin gradually
- Start with data models, utils, then UI layer
- Use Android Studio's Convert Java File to Kotlin

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
