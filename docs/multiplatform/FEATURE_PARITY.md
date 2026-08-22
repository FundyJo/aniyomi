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
| Browse | Compile Pending | Desktop Browse screen uses the real `SourceRegistry`, shows loaded/failed desktop extensions, separates Anime/Manga sources, invokes Popular/Latest/Search, and routes result clicks to details. |
| Search | Compile Pending | Desktop Global Search uses `GlobalSourceSearch`, shows loading/partial/error/retry states, and routes anime/manga result clicks to detail screens. |
| Anime Details | Compile Pending | Routed anime detail screen loads real source details and episodes, shows cover/metadata/loading/error/retry states, and can add source entries to the SQLDelight anime library. |
| Manga Details | Compile Pending | Routed manga detail screen loads real source details and chapters, shows cover/metadata/loading/error/retry states, and can add source entries to the SQLDelight manga library. |
| Episodes | Compile Pending | Episode lists returned by real anime source APIs route to video resolution/player screen; progress persistence is implemented in desktop SQLDelight helpers. |
| Chapters | Compile Pending | Chapter lists returned by real manga source APIs route to the desktop reader; page progress/read-threshold persistence is implemented in desktop SQLDelight helpers. |
| Reader | Compile Pending | Desktop reader state, modes, toolbar, keyboard navigation, Coil memory/disk image loading, MangaPill page pipeline, and progress resume are implemented; runtime validation is pending. |
| Player | Started / compile pending | Jellyfin episode video resolution routes to a player screen with controls and header visibility; embedded libmpv backend remains pending. |
| History | Compile Pending | Desktop History screen reads real anime and manga history SQLDelight views. |
| Updates | Compile Pending | Desktop Updates screen reads real anime and manga update SQLDelight views. |
| Downloads | Not Started | Desktop screen shows the resolved download directory; reader/player source flows are prioritized before the coroutine download worker. |
| Tracking | Compile Pending | Desktop source secret storage uses Windows DPAPI via JNA for Jellyfin API tokens; non-Windows desktop reports unsupported instead of plaintext fallback. |
| Backup | Not Started | Desktop backup import/export has not been implemented. |
| Settings | Compile Pending | Desktop settings include Jellyfin host/user/library fields, DPAPI-backed token save, and a real Test Connection action. |
| Packaging | Compile Pending | Compose Desktop native distribution metadata is configured for MSI/EXE tasks; task execution is blocked before plugin resolution. |
| UI Migration | Started / compile pending | Desktop now has a Compose Desktop shell with a typed back stack, routed details, reader/player routes, and preserved sidebar on detail screens. |

Legend: `Implemented / compile pending` means code is present but Gradle did not reach Kotlin compilation. `Compile Pending` means feature code exists but has not been compiler-validated because dependency resolution fails first. `Runtime Tested` is reserved for features launched and exercised in the desktop app. `Statically validated / compile pending` means source-set scans and model/API review passed locally, but compile/runtime validation remains pending.

Current external blocker: Gradle still fails resolving `com.android.tools.build:gradle:8.9.1` from `dl.google.com` before Kotlin compilation starts; this was reproduced for `:app-desktop:compileKotlinDesktop`, `:source-api:compileKotlinDesktop`, and `:data:compileKotlinDesktop`.
