# Aniyomi Multiplatform Feature Parity

| Feature | Android | Windows | iOS | Notes |
| --- | --- | --- | --- | --- |
| Core Domain | ✅ | 🚧 | 🚧 | KMP layout exists; Category, Anime/Manga/Episode/Chapter/Library, History, Updates models/contracts, and Tracking models/contracts are in `commonMain`; more slices pending. |
| Database | ✅ | 🚧 | 🚧 | SQLDelight schemas and adapters moved to `commonMain`; Android driver factory preserves existing DB names; desktop/iOS driver factories added; repository migration and tests pending. |
| Anime Library | ✅ | 🚧 | 🚧 | Anime domain/library models and sorting are common; preferences/data/UI remain Android-wired. |
| Manga Library | ✅ | 🚧 | 🚧 | Manga domain/library models and sorting are common; preferences/data/UI remain Android-wired. |
| Search | ✅ | 🚧 | 🚧 | Requires source/runtime migration. |
| Anime Sources | ✅ | 🚧 | 🚧 | Legacy Android extensions preserved; KMP sources planned. |
| Manga Sources | ✅ | 🚧 | 🚧 | Legacy Android extensions preserved; KMP sources planned. |
| History | ✅ | 🚧 | 🚧 | Domain models/contracts/use cases and data repositories moved to `commonMain`; UI and platform behavior remain Android reference. |
| Downloads | ✅ | 🚧 | 🚧 | `DownloadEngine` contract added. |
| Reader | ✅ | 🚧 | 🚧 | Needs shared reader state and image pipeline. |
| Player | ✅ | 🚧 | 🚧 | `MediaPlayerEngine` contract added. |
| Tracking | ✅ | 🚧 | 🚧 | Models/repository contracts and simple grouping use cases moved to `commonMain`; OAuth/browser/token storage implementations remain platform-specific. |
| Backup / Restore | ✅ | 🚧 | 🚧 | Needs secure cross-platform extraction/export work. |
| Settings | ✅ | 🚧 | 🚧 | Preferences migration pending. |
| Legacy APK Extensions | ✅ | N/A | N/A | Must remain Android-only. |
| Multiplatform Sources | 🚧 | 🚧 | 🚧 | `MediaSource` contract exists; legacy Source API compatibility/network audit documented, adapters pending. |

Legend: ✅ existing/reference, 🚧 migration pending, N/A not applicable.
