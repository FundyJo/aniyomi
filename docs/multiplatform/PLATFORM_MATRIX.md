# Multiplatform Platform Matrix

| Capability | Shared API | Android implementation | Windows/Desktop implementation | iOS/iPadOS implementation | Status |
| --- | --- | --- | --- | --- | --- |
| Platform info | `PlatformInfoProvider` | Android system/build info | JVM/OS info | UIKit/Foundation info | API added |
| Filesystem | `PlatformFileSystem` | SAF + app storage | Desktop filesystem + file picker | Sandbox + document picker | API added |
| File picker | `FilePicker` | Android picker intents | Native desktop picker | UIDocumentPicker | API added |
| Sharing | `ShareService` | Android Sharesheet | OS share/open integration | UIActivityViewController | API added |
| Clipboard | `ClipboardService` | ClipboardManager | Desktop clipboard | UIPasteboard | API added |
| Notifications | `NotificationService` | Android notifications | Windows notifications | UNUserNotificationCenter | API added |
| Secure storage | `SecureStorage` | Android Keystore | Credential Manager / DPAPI | Keychain | API added |
| Background tasks | `BackgroundTaskScheduler` | WorkManager/services | Coroutine scheduler | BGTaskScheduler/URLSession limits | API added |
| Player | `MediaPlayerEngine` | mpv Android | libmpv bridge | AVFoundation/AVPlayer | API added |
| Web content | `WebContentEngine` | Android WebView | Desktop WebView/browser component | WKWebView | API added |
| OAuth/browser auth | `AuthenticationLauncher` | Custom Tabs/browser | System browser callback | ASWebAuthenticationSession | API added |
| External browser | `ExternalBrowser` | Android browser intent | System browser | UIApplication openURL | API added |
| Network status | `NetworkStatus` | ConnectivityManager | JVM/OS network monitor | Network framework | API added |
| Downloads | `DownloadEngine` | Foreground service/WorkManager | Coroutine worker | URLSession background transfer where possible | API added |
| Window/layout | `WindowManager` | Activity/window metrics | Desktop window state | UIKit window/scene state | API added |
| Android APK extensions | Existing legacy APIs | Supported | N/A | N/A | Preserved |
| Multiplatform sources | `MediaSource` | Planned | Planned | Planned | Contract added |
| Domain chapter/episode slice | Common models/services | Reused by Android domain | Metadata target declared | Metadata target declared | Partial common slice added |
| Shizuku | Platform feature interface | Supported | Unsupported capability | Unsupported capability | Pending |
| TorrServer/torrent | Torrent engine interface | Existing integration | Planned | Capability-gated | Pending |

## Target Source Sets

| Module class | commonMain | androidMain | desktopMain | iosMain |
| --- | --- | --- | --- | --- |
| Platform contracts | Shared interfaces/models | Android adapters | Windows/JVM adapters | iOS adapters |
| Domain | Chapter/episode models and missing-item services started | Legacy Android-only sources retained while moving slices | none when possible | none when possible |
| Data | Repositories/SQLDelight generated APIs | Android driver | JDBC/native desktop driver | Native SQLite driver |
| Source API | Neutral source contracts | APK extension adapter | KMP package/runtime | KMP package/runtime |
| Presentation | Compose Multiplatform UI | Android host glue | Desktop host glue | iOS host glue |
