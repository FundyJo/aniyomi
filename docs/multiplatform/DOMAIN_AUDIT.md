# Domain Multiplatform Audit

## Classification

| Area under `domain/src/androidMain` | Class | Notes / target |
| --- | --- | --- |
| `tachiyomi/domain/category/**` mutating interactors | A | Repository-only business logic; move after write-path tests are added. Read-only category contracts already live in `commonMain`. |
| `tachiyomi/domain/custombuttons/**` except `SaveCustomButtonException` | A | Pure models/repository contracts/use cases; move as a later domain slice. |
| `tachiyomi/domain/track/**` interactors/repositories | B | Repository-only logic, but models use `java.io.Serializable`; switch to `DomainSerializable` only where Android compatibility still requires it. |
| `tachiyomi/domain/history/**` | B | Models use `java.util.Date`; keep persisted timestamps stable and introduce a common date/time contract or Long timestamp model before moving. |
| `tachiyomi/domain/updates/**` | B | Repositories/models are mostly common-ready; interactors use `java.time.Instant` for UI windows and need a common time seam. |
| `mihon/domain/upcoming/**` | B | Depends on entry `expectedNextUpdate`; move after update-time API is stabilized for non-JVM platforms. |
| `tachiyomi/domain/items/season/**` | B | Similar to migrated episode logic, but sorter still uses JVM collator and default flags depend on preferences. |
| `mihon/domain/items/**/Filter*ForDownload.kt` | C | Requires download/storage availability contract before becoming common. |
| `tachiyomi/domain/source/**` repositories/services | C | Source manager contracts expose legacy `Source`, `HttpSource`, Android Paging, Rx/OkHttp patterns; needs shared source contract adapter. |
| `tachiyomi/domain/download/service/DownloadPreferences.kt` | C | Preference-backed platform contract required. |
| `tachiyomi/domain/storage/service/StoragePreferences.kt` | C | Depends on Android-oriented core common storage/preferences; needs platform storage abstraction. |
| `mihon/domain/extensionrepo/**` | C | Repo service is network/serialization-ready in shape, but exception uses `IOException` and service target depends on current data/network stack. |
| `mihon/domain/extension/anime/**` store contracts | C | Store models are pure, but extension store service/data path is tied to current network/extension runtime. |
| `tachiyomi/domain/release/**` | C | Release lookup is network/time dependent; use platform/network contract before moving. |
| `tachiyomi/domain/source/**/Stub*` models | C | Common-ready as models, but coupled to source repository/service package that still needs source contract split. |
| `tachiyomi/domain/backup/service/**` | D | Android app preference/export configuration; keep platform-specific until backup abstraction exists. |
| `tachiyomi/domain/storage/service/StorageManager.kt` | D | Uses `Context`, Android URI conversion, UniFile, and SAF-specific behavior. |
| `eu/kanade/tachiyomi/extension/anime/model/AnimeExtension.kt` | D | Uses Android `Drawable`; legacy APK extension UI/runtime model remains Android-only. |
| `tachiyomi/domain/library/service/LibraryPreferences.kt` | D | PreferenceStore-backed Android/default preference wiring; business models moved, preference storage remains platform-specific. |
| `tachiyomi/domain/entries/**/AnimeFetchInterval.kt`, `MangaFetchInterval.kt` | C | Current implementation uses `java.time`; move after a common time API is selected. |
| `tachiyomi/domain/entries/**/NetworkToLocal*.kt` | C | Depends on source managers and legacy source models; move after shared source adapter design. |
| `tachiyomi/domain/items/**/Set*Default*Flags.kt` | C | Depends on `LibraryPreferences`; move behind common preferences contract. |

## Migrated in this slice

| Slice | Status | Notes |
| --- | --- | --- |
| Anime models/contracts/read use cases | 🚧 | Models, update model, repository contracts, relation contracts, read/reset/set flag use cases are in `commonMain`; fetch interval and network conversion remain Android source-set. |
| Manga models/contracts/read use cases | 🚧 | Models, update model, repository contracts, read/reset/set flag use cases are in `commonMain`; fetch interval and network conversion remain Android source-set. |
| Episode models/contracts/use cases | 🚧 | Episode model/update/repository/read/update/sorting/recognition helpers are in `commonMain`; default preference flag writer remains Android source-set. |
| Chapter models/contracts/use cases | 🚧 | Chapter model/update/repository/read/update/sorting/recognition helpers are in `commonMain`; default preference flag writer remains Android source-set. |
| Library models/sorting | 🚧 | Library entry models, display mode, flags, and category sort models are in `commonMain`; `LibraryPreferences` remains Android source-set. |

## Java-time and serialization notes

| Usage | Classification | Action |
| --- | --- | --- |
| Entry `expectedNextUpdate` | B | Replaced direct `java.time.Instant` in common models with `EntryUpdateInstant` expect/actual; Android/Desktop actuals keep `java.time.Instant` compatibility. |
| Fetch interval interactors | C | Still Android source-set because interval logic uses `Instant`, `ZoneId`, `ZonedDateTime`, and `ChronoUnit`. |
| Updates interactors | B | Still Android source-set; should use the same common time strategy as fetch intervals. |
| History models | B | Still Android source-set; use existing Long DB timestamps or a common time value without changing persistence. |
| `java.io.Serializable` in common models | B | Anime/Manga now use `DomainSerializable`; Android/Desktop actuals keep Java serialization compatibility. |
