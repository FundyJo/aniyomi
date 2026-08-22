package tachiyomi.core.platform.errors

sealed class PlatformError(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class NetworkError(message: String, cause: Throwable? = null) : PlatformError(message, cause)

class SourceError(message: String, cause: Throwable? = null) : PlatformError(message, cause)

class ParserError(message: String, cause: Throwable? = null) : PlatformError(message, cause)

class AuthenticationError(message: String, cause: Throwable? = null) : PlatformError(message, cause)

class DatabaseError(message: String, cause: Throwable? = null) : PlatformError(message, cause)

class StorageError(message: String, cause: Throwable? = null) : PlatformError(message, cause)

class PlayerError(message: String, cause: Throwable? = null) : PlatformError(message, cause)

class DownloadError(message: String, cause: Throwable? = null) : PlatformError(message, cause)

class UnsupportedPlatformFeature(
    feature: String,
    platform: String,
    cause: Throwable? = null,
) : PlatformError("$feature is not supported on $platform", cause)
