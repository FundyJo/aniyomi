# Aniyomi Multiplatform Feature Parity

| Feature | Status | Notes |
| --- | --- | --- |
| Domain Core | Implemented / compile pending | Category, Anime, Manga, Episode, Chapter, Library, History, Updates, Tracking, Download contracts are in shared source sets from earlier slices. |
| Data KMP Foundation | Implemented / compile pending | Shared SQLDelight schemas/migrations and platform driver factories exist; Gradle remains blocked before Kotlin compilation. |
| SQLDelight Shared Schema | Implemented / compile pending | Shared schema/migration files are in `data/commonMain`. |
| Android DB Driver | Implemented / compile pending | Existing DB names preserved; compile/runtime validation pending. |
| Desktop DB Driver | Implemented / compile pending | Driver exists; desktop path behavior still needs real validation. |
| iOS DB Driver | Implemented / compile pending | Native driver exists; sandbox path behavior still needs real validation. |
| Shared Source Models | Implemented / compile pending | `SAnime`, `SManga`, `SEpisode`, `SChapter`, filters, `Page`, `Video`, and `Hoster` are platform-neutral in `source-api/commonMain`. |
| Legacy Android Sources | Preserved / compile pending | `HttpSource`, `AnimeHttpSource`, parsed sources, JSoup helpers, preferences, and torrent helpers are isolated in `androidMain`. |
| Shared Source API | Implemented / compile pending | `MultiplatformSource`, anime/manga contracts, source pages, filters, media, episode, page, video, subtitle, and track models are in `commonMain`. |
| Shared Network API | Implemented / compile pending | `NetworkClient`, request/response/body, `HttpMethod`, `HttpHeaders`, and `Cookie` are in `commonMain`. |
| Android Legacy Adapters | Implemented / compile pending | Android adapters map legacy anime/manga sources into the new multiplatform contracts. |
| More Data Repositories | Partially implemented / compile pending | Anime relation, episode, chapter repositories plus entry mappers/sanitizers moved to `commonMain`; anime/manga repositories still need date/logging refactor. |
| Extension Runtime | Not started | No iOS code loading, APK/JAR/DEX runtime, JS, WASM, or package runtime added in this slice. |
| UI Migration | Not started | Android UI remains reference implementation; desktop/iOS UI intentionally untouched. |

Legend: `Implemented / compile pending` means code is present but Gradle did not reach Kotlin compilation because external dependency resolution is blocked.
