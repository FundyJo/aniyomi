package tachiyomi.domain.entries

actual data class EntryUpdateInstant(val epochMilliseconds: Long)

actual fun entryUpdateInstantFromEpochMilliseconds(epochMilliseconds: Long): EntryUpdateInstant {
    return EntryUpdateInstant(epochMilliseconds)
}
