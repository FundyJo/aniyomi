# Aniyomi Multiplatform Feature Parity

| Feature | Android | Desktop | iOS | Notes |
| --- | --- | --- | --- | --- |
| Core Domain | ✅ | 🚧 | 🚧 | First chapter/episode model slice is common; broader domain remains pending. |
| Database | ✅ | 🚧 | 🚧 | Existing SQLDelight Android data remains canonical. |
| Library | ✅ | 🚧 | 🚧 | Android reference exists; shared migration pending. |
| Anime Library | ✅ | 🚧 | 🚧 | Android reference exists; shared migration pending. |
| Manga Library | ✅ | 🚧 | 🚧 | Android reference exists; shared migration pending. |
| Search | ✅ | 🚧 | 🚧 | Requires source/runtime migration. |
| Anime Sources | ✅ | 🚧 | 🚧 | Legacy Android extensions preserved; KMP sources planned. |
| Manga Sources | ✅ | 🚧 | 🚧 | Legacy Android extensions preserved; KMP sources planned. |
| History | ✅ | 🚧 | 🚧 | Depends on shared domain/data migration. |
| Downloads | ✅ | 🚧 | 🚧 | `DownloadEngine` contract added. |
| Updates | ✅ | 🚧 | 🚧 | Android reference exists; shared migration pending. |
| Reader | ✅ | 🚧 | 🚧 | Needs shared reader state and image pipeline. |
| Player | ✅ | 🚧 | 🚧 | `MediaPlayerEngine` contract added. |
| Tracking | ✅ | 🚧 | 🚧 | OAuth and secure storage abstractions added. |
| Backup | ✅ | 🚧 | 🚧 | Needs secure cross-platform extraction/export work. |
| Settings | ✅ | 🚧 | 🚧 | Preferences migration pending. |
| Extensions | ✅ | 🚧 | 🚧 | Legacy Android APK extensions preserved; KMP extension runtime pending. |
| Legacy APK Extensions | ✅ | N/A | N/A | Must remain Android-only. |
| Multiplatform Sources | 🚧 | 🚧 | 🚧 | `MediaSource` contract and package manifest model added. |

Legend: ✅ existing/reference, 🚧 migration pending, N/A not applicable.
