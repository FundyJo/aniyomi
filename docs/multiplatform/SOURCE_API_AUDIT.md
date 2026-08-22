# Source API Multiplatform Audit

| Component | Current dependency | Target | Strategy |
| --- | --- | --- | --- |
| `SAnime`, `SAnimeImpl` | serialization only | A/common | Keep in `commonMain`; removed JVM `Serializable`. |
| `SManga`, `SMangaImpl` | none | A/common | Keep in `commonMain`; removed JVM `Serializable`. |
| `SEpisode`, `SChapter` | serialization/none | A/common | Keep in `commonMain`; removed JVM `Serializable`. |
| `AnimesPage`, `MangasPage` | neutral | A/common | Keep as shared page containers. |
| `AnimeFilter`, `Filter` | neutral | A/common | Keep UI-free filter state in `commonMain`. |
| `AnimeFilterList`, `FilterList` | Compose `Stable` | B/common | Removed Compose annotation; keep as shared filter lists. |
| `Page` | Android `Uri`, network `ProgressListener` | B/common | Replaced URI slot with neutral `Any?`; progress callback is a local method. |
| `Video` | Android `Uri`, OkHttp `Headers` | B/common | Replaced OkHttp headers with shared `HttpHeaders`; Android interop converts to/from OkHttp. |
| `Hoster` | depends on `Video` serialization | B/common | Keep after `Video` header neutralization. |
| `ThumbnailInfo` | neutral | A/common | Keep in `commonMain`. |
| `MangaSource` | RxJava legacy methods | A/common | Common contract is suspend-only; Rx bridge remains in Android legacy classes. |
| `CatalogueSource` | RxJava legacy methods | A/common | Common contract is suspend-only; Android `HttpSource` preserves legacy fetch methods. |
| `AnimeSource` | RxJava legacy methods | A/common | Common contract is suspend-only; Android `AnimeHttpSource` preserves legacy fetch methods. |
| `MultiplatformSource` | early shared model | A/common | Expanded into `MultiplatformAnimeSource` and `MultiplatformMangaSource`. |
| `HttpSource` | OkHttp, RxJava, JVM crypto/URI | D/android legacy | Moved to `androidMain`; keep APK extension API. |
| `ParsedHttpSource` | JSoup, OkHttp | D/android legacy | Moved to `androidMain`; shared parser abstraction deferred. |
| `AnimeHttpSource` | Android bitmap, OkHttp, RxJava, JSoup-adjacent | D/android legacy | Moved to `androidMain`; keep APK extension API. |
| `ParsedAnimeHttpSource` | JSoup, OkHttp | D/android legacy | Moved to `androidMain`; shared parser abstraction deferred. |
| `ConfigurableSource` | Android `SharedPreferences` | D/android legacy | Moved to `androidMain`. |
| `ConfigurableAnimeSource` | Android `SharedPreferences` | D/android legacy | Moved to `androidMain`. |
| `PreferenceScreen` typealiases | AndroidX Preference | D/android legacy | Removed common expect; Android typealiases remain. |
| `JsoupExtensions` | OkHttp, JSoup | D/android legacy | Moved to `androidMain`. |
| `TorrentUtils`, torrent models | JVM networking/encoding, injected Android network | C/D android legacy | Moved out of common for this slice. |
| `JsonExtensions` | Injekt app singleton | D-risk | Still common in package, but no forbidden platform imports; future slice should replace with explicit Json injection. |
| `source.network.*` | neutral models | A/common | New minimal NetworkClient, request/response/body/header/cookie foundation. |

## Static Gate

`source-api` now registers `checkSourceApiCommonMainImports`, which fails if `commonMain` references Android, AndroidX, Java/JVM, RxJava, OkHttp, or JSoup packages.
