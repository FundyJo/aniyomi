# Aniyomi Multiplatform Feature Parity

| Feature | Status | Notes |
| --- | --- | --- |
| Domain Core | Compile Pending | Category, Anime, Manga, Episode, Chapter, Library, History, Updates, Tracking, Download contracts are in shared source sets from earlier slices. |
| Data KMP Foundation | Compile Pending | Shared SQLDelight schemas/migrations and platform driver factories exist; Gradle remains blocked before Kotlin compilation. |
| SQLDelight Shared Schema | Compile Pending | Shared schema/migration files are in `data/commonMain`. |
| Android DB Driver | Compile Pending | Existing DB names preserved; compile/runtime validation pending. |
| Desktop DB Driver | Compile Pending | `DesktopDatabaseDriverFactory` opens `tachiyomi.db` and `tachiyomi.animedb` under the platform app-data directory; runtime validation is pending. |
| iOS DB Driver | Compile Pending | Native driver exists; sandbox path behavior still needs real validation. |
| Shared Source Models | Compile Pending | `SAnime`, `SManga`, `SEpisode`, `SChapter`, filters, `Page`, `Video`, and `Hoster` are platform-neutral in `source-api/commonMain`. |
| Legacy Android Sources | Compile Pending | `HttpSource`, `AnimeHttpSource`, parsed sources, JSoup helpers, preferences, and torrent helpers are isolated in `androidMain`. |
| Shared Source API | Compile Pending | `MultiplatformSource`, anime/manga contracts, source pages, filters, media, episode, page, video, subtitle, and track models are in `commonMain`. |
| Shared Network API | Compile Pending | `NetworkClient`, request/response/body, `HttpMethod`, immutable `HttpHeaders`, shared `Cookie`, streaming response bodies, and Ktor client scaffolding are in shared source sets. |
| Ktor Engines | Compile Pending | Android uses Ktor OkHttp, desktop uses CIO, and iOS uses Darwin; runtime validation is pending because Gradle is blocked before Kotlin compilation. |
| Android Legacy Network Interop | Compile Pending | Header, cookie, and request conversions bridge shared network models to OkHttp without removing legacy Android OkHttp usage. |
| Android Legacy Adapters | Compile Pending | Android adapters map legacy anime/manga sources, filters, details, episodes/chapters, pages, and videos into multiplatform contracts. |
| Source Registry | Compile Pending | Shared `SourceRegistry`, `BuiltinSourceRegistry`, Android legacy registry adapter infrastructure, and desktop extension reload now replace desktop registry sources from validated JVM packages. |
| Shared Global Search | Compile Pending | Shared query/result/state/error models, concurrency-limited per-source isolation, partial result states, and desktop search UI are implemented. |
| More Data Repositories | Compile Pending | SQLDelight-backed repositories are in `commonMain`; additional `data` and `domain` import gates now guard Kotlin common source sets. |
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
| Player | Compile Pending | Jellyfin episode video resolution now loads real `VideoSource` entries into an embedded libmpv/JNA engine with header forwarding, quality selection, playback controls, keyboard controls, fullscreen state, and periodic progress persistence; compile/runtime validation remains blocked locally. |
| History | Compile Pending | Desktop History screen reads real anime and manga history SQLDelight views. |
| Updates | Compile Pending | Desktop Updates screen reads real anime and manga update SQLDelight views. |
| Downloads | Compile Pending | Desktop coroutine download engine now persists queue metadata, downloads MangaPill chapter pages and direct anime video files, exposes pause/resume/cancel/retry/delete/open-folder actions, and reader/player routes prefer completed local files; Kotlin compile and runtime validation remain pending. |
| Tracking | Compile Pending | Desktop source secret storage uses Windows DPAPI via JNA for Jellyfin API tokens; non-Windows desktop reports unsupported instead of plaintext fallback. |
| Backup | Not Started | Desktop backup import/export has not been implemented. |
| Settings | Compile Pending | Desktop settings include Jellyfin host/user/library fields, DPAPI-backed token save, and a real Test Connection action. |
| Packaging | Compile Pending | Compose Desktop native distribution metadata is configured for MSI/EXE tasks; task execution is blocked before plugin resolution. |
| UI Migration | Compile Pending | Desktop now has a Compose Desktop shell with a typed back stack, routed details, reader/player routes, and preserved sidebar on detail screens. |

Legend: `Not Started` means no real implementation is present. `Implemented` means code exists and is not known to be blocked by local compiler access. `Compile Pending` means implementation code exists but has not been compiler-validated because dependency resolution fails first. `Runtime Tested` is reserved for features launched and exercised in the desktop app.

Current external blocker: Gradle still fails resolving `com.android.tools.build:gradle:8.9.1` from `dl.google.com` before Kotlin compilation starts; this was reproduced for `:app-desktop:compileKotlinDesktop`, `:source-api:compileKotlinDesktop`, and `:data:compileKotlinDesktop`.
