package tachiyomi.core.platform.logging

enum class LogLevel {
    Debug,
    Info,
    Warn,
    Error,
}

interface PlatformLogger {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}
