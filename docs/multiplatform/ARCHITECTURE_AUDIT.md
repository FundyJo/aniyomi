# Aniyomi Multiplatform Architecture Audit

## Snapshot

- Current repository is a single Android application with shared library modules and several Android-only presentation/runtime modules.
- Existing KMP usage is limited to `i18n`, `i18n-aniyomi`, `source-api`, and `source-local`; only Android targets are currently configured there.
- `data`, `domain`, `core:common`, `presentation-core`, `presentation-widget`, and `app` still use Android/JVM source layouts and Android Gradle/Kotlin plugins.
- SQLDelight schemas are already centralized in `data`, with separate manga and anime databases plus migration folders.

## Gradle Modules

| Module | Current role | Current platform state | Migration note |
| --- | --- | --- | --- |
| `app` | Android host app, DI wiring, navigation, services, player/reader UI | Android application | Keep Android reference host; split platform integrations behind shared interfaces. |
| `core:common` | Preferences, network helpers, utilities, torrents, Android/native dependencies | Android library | Extract platform-neutral utilities to common source sets; move Android/TorrServer/FFmpeg bindings behind interfaces. |
| `core:archive` | Archive helpers | Android library | Move pure archive contracts to common; keep Android `Context` helpers in Android source set. |
| `core:platform` | New platform abstraction contracts | KMP Android/Desktop/iOS | First shared seam for filesystem, player, downloads, auth, web, notifications, and window APIs. |
| `core-metadata` | Metadata serialization | Android library | Candidate for early KMP after `source-api` model cleanup. |
| `data` | SQLDelight databases, repositories/data sources | Android library | Convert to SQLDelight KMP after driver abstractions and migration tests are in place. |
| `domain` | Use cases and business models | Android library | High-priority KMP target; remove `unifile`, Android paging, and Android/JVM-only imports. |
| `source-api` | Legacy manga/anime source contracts | KMP Android-only target | Add desktop/iOS once Android/JVM leaks are isolated; new neutral source contract added. |
| `source-local` | Local source implementation | KMP Android-only target | Needs filesystem abstraction before desktop/iOS targets. |
| `presentation-core` | Compose UI primitives | Android Compose | Convert after adopting Compose Multiplatform dependencies. |
| `presentation-widget` | Android Glance widgets | Android-only | Keep Android-specific; do not move to common. |
| `i18n`, `i18n-aniyomi` | Moko resources | KMP Android-only target | Extend targets after resource tooling is validated for desktop/iOS. |
| `macrobenchmark` | Android benchmark tests | Android-only | Remains Android-only. |

## Android-Specific Hotspots

Approximate import counts from `android.*` / `androidx.*` imports:

| Area | Count | Notes |
| --- | ---: | --- |
| `app` | 7293 | Main Android host, services, player, reader, WorkManager, notifications, WebView, storage. |
| `presentation-core` | 778 | AndroidX Compose artifacts and Android UI utilities. |
| `presentation-widget` | 168 | Glance app widgets; intentionally Android-only. |
| `core:common` | 85 | Android/network/preferences/native helpers mixed with reusable utilities. |
| `data` | 16 | SQLDelight Android driver and Android paging integration. |
| `domain` | 7 | Android/JVM data types and paging leakage. |
| `source-api` | 15 | `android.net.Uri`, preferences, and legacy Android compatibility hooks. |
| `source-local` | 9 | Local file access and Android resource/runtime usage. |

## Critical Boundaries

- Database: `data/src/main/sqldelight` and `data/src/main/sqldelightanime` contain the canonical schemas and migrations; these must not be rewritten or reset.
- Source API: legacy APK extensions depend on current `MangaSource`, `AnimeSource`, RxJava bridges, and Android compatibility types; compatibility must stay on Android.
- Network: OkHttp is shared through Android/JVM modules today; desktop and iOS need a KMP HTTP seam before replacing implementation details.
- Filesystem: Android SAF/Unifile is currently mixed into source/local/domain flows; common code needs path and picker abstractions.
- Player: mpv, FFmpeg, MediaSession, PiP, and native `.so` libraries are Android packaged today; shared code should depend only on player contracts.
- Background work: WorkManager and Android services must be contained in Android implementations of a common scheduler/download API.
- Web/OAuth: Android WebView/custom-tabs behavior must move behind common browser/authentication interfaces.

## Security And Data Preservation Risks

- Existing SQLite schemas and migrations must remain canonical to avoid data loss for Android users.
- Backups and extension packages need Zip Slip/path traversal checks, checksums, signatures, and no plaintext secret exports.
- Extension runtimes must enforce host allowlists and capabilities; desktop/iOS cannot execute arbitrary Android APK/native code.
- Logs must avoid OAuth tokens, session cookies, credentials, and sensitive headers.

## Initial Migration Slice

Changed:
- Added a dedicated `core:platform` KMP module for platform abstraction contracts.
- Added a neutral `MediaSource` contract beside the legacy Android-compatible extension API.

Migrated:
- No existing runtime implementation was moved in this slice; this avoids breaking Android behavior before abstractions are wired.

Android:
- Existing Android app and legacy extension runtime remain the reference implementation.

Desktop:
- `core:platform` now declares `desktop` JVM metadata and source-set compatibility.

IOS:
- `core:platform` now declares `iosArm64` and `iosSimulatorArm64` metadata.

Tests:
- Validation status is tracked in the migration plan and final report as phases complete.

Remaining blockers:
- `source-api` common code still contains Android/JVM-only types.
- `domain` and `data` are still Android Gradle modules.
- Compose UI still uses AndroidX artifacts instead of Compose Multiplatform artifacts.
