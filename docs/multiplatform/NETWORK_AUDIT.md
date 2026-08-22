# Network Multiplatform Audit

| Capability | Current usage | Target | Notes |
| --- | --- | --- | --- |
| OkHttp client | Legacy `HttpSource` and app network stack | platform/common adapter | Keep Android legacy extensions on OkHttp. |
| Requests/headers | `GET`, `Headers`, `Request`, source headers | common contract + adapters | Preserve User-Agent/Referer behavior before introducing Ktor. |
| Interceptors | Existing app/network module configuration | platform | Inventory concrete interceptors before replacing client. |
| Cookies | Legacy extension/browser flows likely depend on OkHttp cookie handling | platform | Must remain compatible for APK extensions. |
| Cache | OkHttp cache behavior used by source/network layer | platform | Common API should expose cache policy, not implementation. |
| DNS/proxy/TLS | OkHttp supports current Android behavior | platform | Do not disable TLS/security checks; model capabilities explicitly. |
| Redirects | Source scraping depends on client defaults | common policy + adapter | Preserve current defaults for legacy sources. |
| Range/streaming | Reader/player/download paths may use streaming/range behavior | platform | Audit per downloader/reader before migration. |
| WebSocket | Not migrated in this slice | platform-specific if needed | Leave out of common API until real usage is confirmed. |
| Authentication | OAuth/browser flows and source auth are Android-oriented | platform auth contract | Use existing `AuthenticationLauncher` direction for shared APIs. |
| RxJava bridge | Legacy `Source`, `CatalogueSource`, `HttpSource` use `Observable` | Android legacy adapter | New common contracts should prefer `suspend`/`Flow`; no second model world without conversion. |
| JSoup parsing | Legacy parsed sources use JSoup | Android/JVM legacy | Keep for APK extensions; evaluate common parser only for new source packages. |

## Decision

Ktor remains a good candidate for the platform implementation behind the new shared `NetworkClient` contract because it has Android, JVM, and iOS engines. This slice does not add Ktor dependencies yet: legacy Android APK extensions stay on OkHttp/RxJava/JSoup, while new multiplatform sources target neutral request/response/header/cookie/body models first. Engine selection should be validated against the repository's Kotlin/Native and dependency versions once Gradle can resolve Google Maven and reach Kotlin compilation.
