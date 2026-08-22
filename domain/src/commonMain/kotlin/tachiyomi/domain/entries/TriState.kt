package tachiyomi.domain.entries

expect enum class TriState {
    DISABLED,
    ENABLED_IS,
    ENABLED_NOT,
    ;

    fun next(): TriState
}

inline fun applyFilter(filter: TriState, predicate: () -> Boolean): Boolean = when (filter) {
    TriState.DISABLED -> true
    TriState.ENABLED_IS -> predicate()
    TriState.ENABLED_NOT -> !predicate()
    else -> error("Unknown TriState: $filter")
}
