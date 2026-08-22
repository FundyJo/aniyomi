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
| Source Registry | Compile Pending | Shared `SourceRegistry`, `BuiltinSourceRegistry`, Android legacy registry adapter infrastructure, and desktop extension reload now replace desktop registry sources from validated JVM packages. |
| Shared Global Search | Implemented / compile pending | Shared query/result/state/error models, concurrency-limited per-source isolation, partial result states, and desktop search UI are implemented. |
| More Data Repositories | Implemented / compile pending | SQLDelight-backed repositories are in `commonMain`; additional `data` and `domain` import gates now guard Kotlin common source sets. |
| Extension Runtime | Compile Pending | Desktop JVM extension package loading is implemented for isolated JARs with `META-INF/aniyomi-extension.json` or sidecar `extension.json`; package format is documented in `docs/multiplatform/DESKTOP_EXTENSIONS.md`; Android APK loading remains Android-only and iOS code loading remains unsupported. |
| Desktop App Startup | Compile Pending | `app-desktop` is registered with Compose Multiplatform Desktop and a real `main() = application { ... }`; Gradle is still blocked before Kotlin compilation. |
| Desktop Database | Compile Pending | Desktop startup constructs the real manga/anime SQLDelight databases using the desktop driver factory and app-data directories. |
| Library | Compile Pending | Desktop Library screen reads real anime and manga favorites from SQLDelight library views with search, counts, categories, and bookmark state. |
| Browse | Compile Pending | Desktop Browse screen uses the real `SourceRegistry`, shows loaded/failed desktop extensions, separates Anime/Manga sources, and can invoke selected-source Popular, Latest, and Search APIs. |
| Search | Compile Pending | Desktop Global Search uses `GlobalSourceSearch`, shows loading, partial results, grouped source results, errors, retry, and empty states. |
| Anime Details | Compile Pending | Selected desktop anime source results can load real source details and episode lists inline; full routed detail screens and library actions are still pending. |
| Manga Details | Compile Pending | Selected desktop manga source results can load real source details and chapter lists inline; full routed detail screens and library actions are still pending. |
| Episodes | Compile Pending | Episode lists returned by real anime source APIs are displayed from selected Browse results; watched/progress/download state and player routing are still pending. |
| Chapters | Compile Pending | Chapter lists returned by real manga source APIs are displayed from selected Browse results; read/bookmark/download state and reader routing are still pending. |
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

Current external blocker: Gradle still fails resolving `com.android.tools.build:gradle:8.9.1` from `dl.google.com` before Kotlin compilation starts; this was reproduced for `:app-desktop:compileKotlinDesktop`, `:source-api:compileKotlinDesktop`, and `:data:compileKotlinDesktop`.
