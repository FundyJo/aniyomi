package tachiyomi.domain.entries

actual enum class TriState {
    DISABLED,
    ENABLED_IS,
    ENABLED_NOT,
    ;

    actual fun next(): TriState {
        return when (this) {
            DISABLED -> ENABLED_IS
            ENABLED_IS -> ENABLED_NOT
            ENABLED_NOT -> DISABLED
        }
    }
}
