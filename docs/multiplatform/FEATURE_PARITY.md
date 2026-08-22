# Aniyomi Multiplatform Feature Parity

| Feature | Status | Notes |
| --- | --- | --- |
| Domain Core | Implemented / compile pending | Category, Anime, Manga, Episode, Chapter, Library, History, Updates, Tracking, Download contracts are in shared source sets from earlier slices. |
| Data KMP Foundation | Implemented / compile pending | Shared SQLDelight schemas/migrations and platform driver factories exist; Gradle remains blocked before Kotlin compilation. |
| SQLDelight Shared Schema | Implemented / compile pending | Shared schema/migration files are in `data/commonMain`. |
| Android DB Driver | Implemented / compile pending | Existing DB names preserved; compile/runtime validation pending. |
| Desktop DB Driver | Implemented / compile pending | `DesktopDatabaseDriverFactory` opens `tachiyomi.db` and `tachiyomi.animedb` under the platform app-data directory; runtime validation is pending. |
| iOS DB Driver | Implemented / compile pending | Native driver exists; sandbox path behavior still needs real validation. |
| Shared Source Models | Implemented / compile pending | `SAnime`, `SManga`, `SEpisode`, `SChapter`, filters, `Page`, `Video`, and `Hoster` are platform-neutral in `source-api/commonMain`. |
| Legacy Android Sources | Preserved / compile pending | `HttpSource`, `AnimeHttpSource`, parsed sources, JSoup helpers, preferences, and torrent helpers are isolated in `androidMain`. |
| Shared Source API | Implemented / compile pending | `MultiplatformSource`, anime/manga contracts, source pages, filters, media, episode, page, video, subtitle, and track models are in `commonMain`. |
| Shared Network API | Statically validated / compile pending | `NetworkClient`, request/response/body, `HttpMethod`, immutable `HttpHeaders`, shared `Cookie`, streaming response bodies, and Ktor client scaffolding are in shared source sets. |
| Ktor Engines | Implemented / compile pending | Android uses Ktor OkHttp, desktop uses CIO, and iOS uses Darwin; runtime validation is pending because Gradle is blocked before Kotlin compilation. |
| Android Legacy Network Interop | Statically validated / compile pending | Header, cookie, and request conversions bridge shared network models to OkHttp without removing legacy Android OkHttp usage. |
| Android Legacy Adapters | Statically validated / compile pending | Android adapters map legacy anime/manga sources, filters, details, episodes/chapters, pages, and videos into multiplatform contracts. |
| Source Registry | Implemented / compile pending | Shared `SourceRegistry`, `BuiltinSourceRegistry`, Android legacy registry adapter infrastructure, and desktop `BuiltinSourceRegistry` wiring exist. |
| Shared Global Search | Implemented / compile pending | Shared query/result/state/error models, concurrency-limited per-source isolation, partial result states, and desktop search UI are implemented. |
| More Data Repositories | Implemented / compile pending | SQLDelight-backed repositories are in `commonMain`; additional `data` and `domain` import gates now guard Kotlin common source sets. |
| Extension Runtime | Not started | No iOS code loading, APK/JAR/DEX runtime, JS, WASM, or package runtime added in this slice. |
| Desktop App Startup | Compile Pending | `app-desktop` is registered with Compose Multiplatform Desktop and a real `main() = application { ... }`; Gradle is still blocked before Kotlin compilation. |
| Desktop Database | Compile Pending | Desktop startup constructs the real manga/anime SQLDelight databases using the desktop driver factory and app-data directories. |
| Library | Compile Pending | Desktop Library screen reads real anime and manga favorites from SQLDelight library views with search, counts, categories, and bookmark state. |
| Browse | Compile Pending | Desktop Browse screen uses the real `SourceRegistry` and separates Anime Sources and Manga Sources without fake source data. |
| Search | Compile Pending | Desktop Global Search uses `GlobalSourceSearch`, shows loading, partial results, grouped source results, errors, retry, and empty states. |
| Anime Details | Not Started | Source selection exists, but anime detail navigation and episode loading are not implemented in this slice. |
| Manga Details | Not Started | Source selection exists, but manga detail navigation and chapter loading are not implemented in this slice. |
| Episodes | Not Started | Episode-list UI is not implemented yet. |
| Chapters | Not Started | Chapter-list UI is not implemented yet. |
| Reader | Not Started | Desktop manga reader has not been implemented. |
| Player | Not Started | Desktop media player/libmpv integration has not been implemented. |
| History | Compile Pending | Desktop History screen reads real anime and manga history SQLDelight views. |
| Updates | Compile Pending | Desktop Updates screen reads real anime and manga update SQLDelight views. |
| Downloads | Not Started | Desktop screen shows the resolved download directory; the coroutine download worker is not implemented. |
| Tracking | Not Started | Desktop secure storage refuses plaintext writes until DPAPI/Credential Manager integration is added. |
| Backup | Not Started | Desktop backup import/export has not been implemented. |
| Settings | Compile Pending | Desktop settings foundation lists real service-backed groups only; no fake preference toggles are added. |
| Packaging | Compile Pending | Compose Desktop native distribution metadata is configured for MSI/EXE tasks; task execution is blocked before plugin resolution. |
| UI Migration | Started / compile pending | Desktop now has a Compose Desktop shell with permanent sidebar; Android UI remains the reference implementation and iOS UI is untouched. |

Legend: `Implemented / compile pending` means code is present but Gradle did not reach Kotlin compilation. `Compile Pending` means feature code exists but has not been compiler-validated because dependency resolution fails first. `Runtime Tested` is reserved for features launched and exercised in the desktop app. `Statically validated / compile pending` means source-set scans and model/API review passed locally, but compile/runtime validation remains pending.

Current external blocker: Gradle still fails resolving `com.android.tools.build:gradle:8.9.1` from `dl.google.com` before Kotlin compilation starts.
