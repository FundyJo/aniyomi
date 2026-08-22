package tachiyomi.core.platform.window

import kotlinx.coroutines.flow.StateFlow

enum class WindowSizeClass {
    Compact,
    Medium,
    Expanded,
}

data class WindowState(
    val widthDp: Int,
    val heightDp: Int,
    val sizeClass: WindowSizeClass,
    val isFullscreen: Boolean = false,
)

interface WindowManager {
    val state: StateFlow<WindowState>

    fun setFullscreen(enabled: Boolean)
}
