package tachiyomi.core.platform.system

enum class PlatformFamily {
    Android,
    Desktop,
    Ios,
}

data class PlatformInfo(
    val family: PlatformFamily,
    val name: String,
    val version: String? = null,
    val deviceName: String? = null,
    val isDebug: Boolean = false,
)

interface PlatformInfoProvider {
    val current: PlatformInfo
}
