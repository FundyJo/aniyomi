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
