# Desktop Source Extensions

Aniyomi Desktop loads JVM-compatible source extensions from the platform app-data extensions directory:

- Windows: `%APPDATA%\Aniyomi\extensions\`
- macOS: `~/Library/Application Support/Aniyomi/extensions/`
- Linux: `$XDG_DATA_HOME/Aniyomi/extensions/` or `~/.local/share/Aniyomi/extensions/`

Supported package layouts:

```text
extensions/
  source-extension.jar
```

with an embedded manifest:

```text
META-INF/aniyomi-extension.json
```

or:

```text
extensions/
  source-extension/
    extension.jar
    extension.json
    icon.png
```

The manifest schema is intentionally small:

```json
{
  "id": "example.en",
  "name": "Example",
  "version": "1.0.0",
  "language": "en",
  "entryPoints": ["com.example.ExampleSourceFactory"],
  "type": "mixed",
  "sources": [123456789]
}
```

Each entry point must expose a zero-argument constructor and implement either `MultiplatformSource` or `MultiplatformSourceFactory`. Each extension JAR is loaded with its own `URLClassLoader`; one failed extension is reported in Browse without terminating the app.

Validation currently rejects packages with missing/invalid manifests, duplicate extension IDs, duplicate source IDs, invalid entry point classes, or native executable entries such as `.dll`, `.exe`, `.so`, `.dylib`, shell scripts, and batch files. Android APK/DEX loading remains Android-only.

Existing Aniyomi APK extensions are not directly desktop packages. The archived `aniyomiorg/aniyomi-extensions` repository still builds extensions via the Android application plugin and many sources use Android preferences/UI types, but source logic that only depends on JVM libraries such as OkHttp, JSoup, RxJava, and Kotlin can be migrated later into a shared JVM runtime and wrapped by desktop entry points.

## Ported Desktop JVM Sources

Current desktop extension modules are packaged as trusted local JARs and can be copied into the extensions directory above:

- `:desktop-extensions:anime:jellyfin:jar` -> `desktop-extensions/anime/jellyfin/build/libs/jellyfin-desktop-extension.jar`
- `:desktop-extensions:manga:mangapill:jar` -> `desktop-extensions/manga/mangapill/build/libs/mangapill-desktop-extension.jar`

Candidate selection:

- Anime: `aniyomiorg/aniyomi-extensions/src/all/jellyfin` was selected over Google Drive and Google Drive Index. It has real Popular, Latest, Search, Details, Episodes, and Video resolution paths; the desktop port removes Android preference UI and OkHttp interceptors, and uses the shared `NetworkClient` plus `SourcePreferenceStore` for `host_url`, `user_id`, `api_key`, and optional `library_id`.
- Manga: the public Aniyomi extension repository currently has no manga source modules, so `keiyoushi/extensions-source/src/en/mangapill` was selected as a compatible Tachiyomi/Mihon manga extension source. It is a small JSoup/HTML source with real Popular, Latest, Search, Details, Chapters, and Page resolution and no Android preference UI.

Status:

- Jellyfin: Implemented, Compile Pending, Runtime Tested Pending. Requires a reachable Jellyfin server and source preferences before it can return catalog results.
- MangaPill: Implemented, Compile Pending, Runtime Tested Pending. Uses the original MangaPill URL building and JSoup selectors for catalog, detail, chapter, and page extraction.
- Build validation: blocked before Kotlin compilation by the external `dl.google.com` DNS/AGP dependency resolution failure for `com.android.tools.build:gradle:8.9.1`.
