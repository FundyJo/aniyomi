# Data Multiplatform Audit

| Component | Current dependency | Target | Migration strategy |
| --- | --- | --- | --- |
| `data` Gradle module | Android-only Kotlin plugin plus SQLDelight Android bundle | common + platform source sets | Convert to KMP only after SQLDelight source roots and drivers are split. |
| `Database` / `AnimeDatabase` SQLDelight config | `src/main/sqldelight`, `src/main/sqldelightanime`, SQL dialect, generated Android module | common | Move schema/query roots to common source set without table, column, filename, or migration renumbering changes. |
| Android DB driver creation | `AndroidSqliteDriver` in `AppModule`; `tachiyomi.db`, `tachiyomi.animedb`; Android open-helper factory and pragmas | platform | Introduce driver factory after data KMP conversion; Android factory must preserve filenames and pragmas. |
| `AndroidMangaDatabaseHandler` / `AndroidAnimeDatabaseHandler` | SQLDelight JVM coroutines, `SqlDriver`, Android Paging | common core + platform paging | Split query/transaction execution from PagingSource creation; keep Android paging adapter in Android source set. |
| Transaction contexts | ThreadLocal, coroutine transaction dispatcher, SQLDelight transactions | common where supported | Keep transaction semantics; move only after driver-compatible tests cover nested transactions. |
| Repository implementations | SQLDelight generated interfaces and domain contracts | common | Migrate repository logic with generated DB interfaces once SQLDelight is common. |
| Source paging repositories | AndroidX PagingSource, source APIs | platform adapter | Keep Android paging implementation; expose common paging/query contract later. |
| Extension repo/store services | Network DTOs and current service stack | platform/common split | Move pure DTOs first; keep network execution behind common network contract. |
| Release service | GitHub/network response models | platform/common split | Keep service execution platform-specific until network contract exists. |
| Preferences | `core.common` PreferenceStore in domain/data callers | platform | Define common preference contract; keep Android PreferenceStore adapter. |
| Filesystem/storage | UniFile/SAF and Android storage managers outside data | platform | Use `PlatformFileSystem`/storage abstractions; no direct Android URI/File in common. |
| OkHttp usage | Mostly source/network layers, not direct repository storage | platform/common decision pending | Audit required features before any Ktor migration. |

## SQLDelight status

| Item | Current state | Next step |
| --- | --- | --- |
| Schemas | Two SQLDelight DBs: manga `Database` and anime `AnimeDatabase`. | Keep both canonical; do not merge or rename. |
| Migrations | Existing numbered `.sqm` files under both schema roots. | Preserve numbering and add migration tests before DB changes. |
| Generated interfaces | Consumed directly by repositories and handlers. | Keep repository APIs stable while moving generated DB access to common. |
| Android driver | Created in `AppModule` with Android open helper factory and WAL/foreign-key pragmas. | Later move into `AndroidDatabaseDriverFactory` without changing filenames. |
| Desktop/iOS drivers | Not implemented. | Add `JdbcSqliteDriver`/Native driver only after data KMP structure exists. |
| Driver factory | Not added in this slice. | Current architecture creates drivers in app DI; adding a parallel factory now would duplicate DB setup. |
