# Aniyomi Multiplatform Feature Parity

| Feature | Android | Windows | iOS | Notes |
| --- | --- | --- | --- | --- |
| Core Domain | ✅ | 🚧 | 🚧 | KMP layout exists; Category plus Anime/Manga/Episode/Chapter/Library model slices are in `commonMain`; more slices pending. |
| Database | ✅ | 🚧 | 🚧 | SQLDelight schemas remain Android-wired; data/SQLDelight audit documented, driver split pending. |
| Anime Library | ✅ | 🚧 | 🚧 | Anime domain/library models and sorting are common; preferences/data/UI remain Android-wired. |
| Manga Library | ✅ | 🚧 | 🚧 | Manga domain/library models and sorting are common; preferences/data/UI remain Android-wired. |
| Search | ✅ | 🚧 | 🚧 | Requires source/runtime migration. |
| Anime Sources | ✅ | 🚧 | 🚧 | Legacy Android extensions preserved; KMP sources planned. |
| Manga Sources | ✅ | 🚧 | 🚧 | Legacy Android extensions preserved; KMP sources planned. |
| History | ✅ | 🚧 | 🚧 | Depends on shared domain/data migration. |
| Downloads | ✅ | 🚧 | 🚧 | `DownloadEngine` contract added. |
| Reader | ✅ | 🚧 | 🚧 | Needs shared reader state and image pipeline. |
| Player | ✅ | 🚧 | 🚧 | `MediaPlayerEngine` contract added. |
| Tracking | ✅ | 🚧 | 🚧 | OAuth and secure storage abstractions added. |
| Backup / Restore | ✅ | 🚧 | 🚧 | Needs secure cross-platform extraction/export work. |
| Settings | ✅ | 🚧 | 🚧 | Preferences migration pending. |
| Legacy APK Extensions | ✅ | N/A | N/A | Must remain Android-only. |
| Multiplatform Sources | 🚧 | 🚧 | 🚧 | `MediaSource` contract exists; legacy Source API compatibility/network audit documented, adapters pending. |

Legend: ✅ existing/reference, 🚧 migration pending, N/A not applicable.
