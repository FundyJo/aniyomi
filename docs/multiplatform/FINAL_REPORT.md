# Aniyomi Multiplatform Final Report

## Current Status

This report is intentionally incremental. The migration is not complete; Android remains the runnable reference app while shared contracts and the first domain common slice are introduced.

## Architecture

- `core:platform` defines platform-neutral contracts for filesystem, file picker, sharing, clipboard, notifications, secure storage, background tasks, player, WebView/browser auth, network status, downloads, window state, logging, and platform info.
- `source-api` now includes a neutral `MediaSource` contract and source package manifest model for the future multiplatform source runtime.
- `domain` now has Android, desktop JVM, and iOS source sets. Category models, category repository interfaces, and read-only category interactors have moved to `commonMain`.
- Existing Android modules and legacy extension APIs remain in place.

## Modules

- Android reference host: `app`.
- New shared platform contracts: `core:platform`.
- Existing shared/localization KMP modules: `i18n`, `i18n-aniyomi`, `source-api`, `source-local`.
- KMP in progress: `domain`.
- Pending KMP conversion: `data`, `core:common`, `presentation-core`.

## Build Commands

- Android build: `./gradlew :app:assembleDebug`
- Platform contract metadata: `./gradlew :core:platform:compileKotlinMetadata`
- Source API metadata: `./gradlew :source-api:compileKotlinMetadata`
- Formatting/lint: `./gradlew spotlessCheck`
- Domain metadata: `./gradlew :domain:compileKotlinMetadata`
- Domain desktop: `./gradlew :domain:compileKotlinDesktop`

## Platform Matrix

See `docs/multiplatform/PLATFORM_MATRIX.md`.

## Extension System

- Legacy Android APK extensions remain supported by the existing source APIs.
- The new `MediaSource` and `SourcePackageManifest` models define the initial common contract for desktop/iOS-compatible source packages with capabilities, SHA-256 checksums, signatures, and host allowlists.

## Database Migration

- Existing SQLDelight schema and migration folders were not changed.
- Full SQLDelight KMP driver work and migration tests remain pending.

## Libraries

- Current Android/JVM libraries remain unchanged.
- No new external dependencies were added in this slice.

## Known Limitations

- Desktop and iOS host applications are not created yet.
- Platform contracts do not have concrete platform implementations yet.
- Most domain models and use cases still remain in `androidMain` until Android/JVM-only imports are neutralized.
- `source-api` still contains legacy Android/JVM compatibility types that block non-Android targets.
- Compose UI is still AndroidX Compose based.

## Test Status

- Attempted `./gradlew :core:platform:compileKotlinMetadata :source-api:compileKotlinMetadata spotlessCheck`.
- Attempted `./gradlew :core:platform:build :source-api:build`, `./gradlew :app:assembleDebug`, and `./gradlew :domain:compileKotlinMetadata :domain:compileKotlinDesktop spotlessCheck`.
- Gradle could not resolve `com.android.tools.build:gradle:8.9.1` because `dl.google.com` DNS resolution failed in the environment.
- Local `git diff --check` passed before commit, and changed files passed secret scanning.
- Broader Android builds remain required after dependency resolution is available.
