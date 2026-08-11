# RedReader Project

RedReader is an open-source Reddit client for Android written in Java and Kotlin. It provides
a clean, ad-free experience for browsing Reddit with support for image galleries, video playback,
custom themes, and offline reading.

## Architecture

RedReader is a traditional Android app currently transitioning toward modern Android best practices.
It is a multi-activity app using the following:

-   **UI:** Primarily XML layouts with Android Views, with Compose being incrementally adopted.
    Material 3 Expressive (MDC-Android 1.14.0) is the current design system.
-   **State Management:** Activity/Fragment lifecycle-based state management, with plans to
    migrate toward Unidirectional Data Flow (ViewModel + StateFlow).
-   **Dependency Injection:** Currently manual wiring. Hilt is planned for future adoption.
-   **Navigation:** Fragment-based navigation with manual transactions.
-   **Data:** Custom data layer using OkHttp for network requests and Jackson/Kotlinx Serialization
    for JSON parsing. No local database currently.
-   **Background Processing:** Currently uses direct OkHttp calls and handlers. WorkManager is
    planned for background sync tasks.

## Build Configuration

-   **Kotlin:** `android.builtInKotlin=true` (use Android Studio's built-in Kotlin compiler)
-   **Annotation Processing:** KSP (Kotlin Symbol Processing) for Hilt code generation
-   **Hilt:** Dagger Hilt for dependency injection with `ksp` for compiler
-   **KSP Isolation:** `ksp.project.isolation.enabled=true` for faster builds

## Current Tech Stack

| Technology | Version |
|------------|---------|
| Kotlin | 2.2.21 |
| AGP | 9.3.0 |
| compileSdk | 36 |
| minSdk | 23 |
| targetSdk | 36 |
| MDC-Android | 1.14.0 (Material 3 Expressive) |
| Compose BOM | 2025.11.00 |
| OkHttp | 5.3.0 |
| Media3 | 1.8.0 |

## Modules

The project has a flat structure with two library modules:

-   `src/` — Main Android application (single module)
-   `libs/redreader-common/` — Shared utilities, extensions, and helpers
-   `libs/redreader-datamodel/` — Data models and serialization

## Commands to Build & Test

-   Build: `./gradlew assembleDebug`
-   Build release: `./gradlew assembleRelease`
-   Run PMD: `./gradlew pmd`
-   Run Checkstyle: `./gradlew Checkstyle`
-   Run local tests: `./gradlew test`
-   Run instrumented tests: `./gradlew connectedAndroidTest`

## Linting & Code Quality

-   PMD rules: `config/pmd/rules.xml`
-   Checkstyle config: `config/checkstyle/checkstyle.xml`
-   Lint baseline: `config/lint/lint-baseline.xml`
-   Lint config: `config/lint/lint.xml`

## Modernization Goals

RedReader is on a path toward modern Android development practices. Key goals include:

1.  **Compose migration:** Incrementally convert XML layouts to Jetpack Compose
2.  **Hilt DI:** Adopt Hilt for dependency injection
3.  **Unidirectional Data Flow:** Migrate to ViewModel + StateFlow architecture
4.  **Room:** Add local database for caching posts/comments
5.  **WorkManager:** Add background sync for feeds
6.  **Navigation Compose:** Replace Fragment transactions with declarative navigation
7.  **Java → Kotlin:** Gradually migrate remaining Java code to Kotlin

Reference project for modern patterns: [Now in Android](https://github.com/android/nowinandroid)

## Continuous integration

-   Workflows are defined in `.github/workflows/*.yaml`

## Version control and code location

-   The project uses git and is hosted at https://github.com/stormtroopercs/RedReader.
