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
| Shared Network API | Statically validated / compile pending | `NetworkClient`, request/response/body, `HttpMethod`, immutable `HttpHeaders`, shared `Cookie`, streaming response bodies, and Ktor client scaffolding are in shared source sets. |
| Ktor Engines | Implemented / compile pending | Android uses Ktor OkHttp, desktop uses CIO, and iOS uses Darwin; runtime validation is pending because Gradle is blocked before Kotlin compilation. |
| Android Legacy Network Interop | Statically validated / compile pending | Header, cookie, and request conversions bridge shared network models to OkHttp without removing legacy Android OkHttp usage. |
| Android Legacy Adapters | Statically validated / compile pending | Android adapters map legacy anime/manga sources, filters, details, episodes/chapters, pages, and videos into multiplatform contracts. |
| Source Registry | Implemented / compile pending | Shared `SourceRegistry`, `BuiltinSourceRegistry`, and Android legacy registry adapter infrastructure exist; full app wiring is still pending. |
| Shared Global Search | Started / compile pending | Shared query/result/state/error models and concurrency-limited per-source search isolation are implemented; UI migration is pending. |
| More Data Repositories | Implemented / compile pending | SQLDelight-backed repositories are in `commonMain`; additional `data` and `domain` import gates now guard Kotlin common source sets. |
| Extension Runtime | Not started | No iOS code loading, APK/JAR/DEX runtime, JS, WASM, or package runtime added in this slice. |
| UI Migration | Not started | Android UI remains reference implementation; desktop/iOS UI intentionally untouched. |

Legend: `Implemented / compile pending` means code is present but Gradle did not reach Kotlin compilation. `Statically validated / compile pending` means source-set scans and model/API review passed locally, but compile/runtime validation remains pending.

Current external blocker: Gradle still fails resolving `com.android.tools.build:gradle:8.9.1` from `dl.google.com` before Kotlin compilation starts.
