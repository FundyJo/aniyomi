# Aniyomi Multiplatform Migration Plan

## Principles

- Preserve Android behavior and upgradeability throughout the migration.
- Move business logic before UI and host-app work.
- Keep existing SQLDelight schemas and migrations as the source of truth.
- Prefer interfaces for large platform features and `expect`/`actual` only for small differences.
- Compile after every major phase and keep PRs reviewable.

## Target Architecture

```text
Shared Compose UI
    ↓
Shared Features
    ↓
Domain / Data
    ↓
Database / Network / Sources
    ↓
Platform Abstraction Layer
    ├── Android
    ├── Windows Desktop
    └── iOS / iPadOS
```

## Phases

1. Architecture audit and documentation.
2. Gradle and KMP foundation.
3. Core/domain model cleanup.
4. SQLDelight Multiplatform database drivers and migration tests.
5. Data repositories and data-source migration.
6. Shared network layer and Android OkHttp adapter.
7. Filesystem, preferences, secure storage, and file picker abstractions.
8. Source API migration plus Android legacy extension runtime preservation.
9. Multiplatform source package/runtime design and validation.
10. Shared Compose UI foundation and adaptive navigation.
11. Library, browse, history, and updates feature migration.
12. Reader state/model migration and platform input adapters.
13. Player API plus Android mpv, desktop libmpv, and iOS AVPlayer implementations.
14. Download engine and background task schedulers.
15. Tracking, OAuth, WebView, backup/restore, and notifications.
16. Desktop and iOS host applications.
17. Packaging, CI/CD, performance hardening, and final feature parity.

## Near-Term Work Queue

- Convert `source-api` to declare Android/Desktop/iOS targets after isolating `android.net.Uri` and `java.io.Serializable` compatibility.
- Add `PlatformFileSystem` adapters and move local-source file traversal away from Unifile in common code.
- Continue `domain` KMP migration from the first common slice: move neutral models first, then repository contracts and use cases after paging/storage/date compatibility seams are ready.
- Move SQLDelight schema source roots into `commonMain` and add Android/JDBC/Native driver factories without changing migrations.
- Add migration tests that open current Android database versions and verify all migrations.
- Add desktop/iOS host skeletons only after shared domain/data compile for those targets.

## Current Foundation Slice

- `domain` declares Android, desktop JVM, `iosArm64`, and `iosSimulatorArm64` targets.
- Legacy Android domain sources remain under the Android source set to preserve behavior during incremental moves.
- Chapter/episode domain models and missing-item services are now in `commonMain` with common tests.
- Remaining blockers before larger domain moves: Android PagingSource in source repositories, Android storage `Context`, extension icon `Drawable`, and JVM date/serialization types.

## Documentation Template Per Phase

Changed:
- Files and APIs added or modified.

Migrated:
- Runtime logic moved to `commonMain` or shared modules.

Android:
- Compatibility and upgrade notes.

Desktop:
- Windows behavior and packaging notes.

IOS:
- iPhone/iPad behavior and platform limitations.

Tests:
- Commands run and results.

Remaining blockers:
- Known platform gaps and migration risks.
