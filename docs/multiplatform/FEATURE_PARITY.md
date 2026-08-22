# Aniyomi Multiplatform Feature Parity

| Feature | Android | Windows | iOS | Notes |
| --- | --- | --- | --- | --- |
| Core Domain | ✅ | 🚧 | 🚧 | KMP layout exists; category contracts are common, most models/use cases still pending. |
| Database | ✅ | 🚧 | 🚧 | SQLDelight schemas remain Android-wired; driver split pending. |
| Anime Library | ✅ | 🚧 | 🚧 | Android reference exists; shared migration pending. |
| Manga Library | ✅ | 🚧 | 🚧 | Android reference exists; shared migration pending. |
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
| Multiplatform Sources | 🚧 | 🚧 | 🚧 | `MediaSource` contract and package manifest model added. |

Legend: ✅ existing/reference, 🚧 migration pending, N/A not applicable.
