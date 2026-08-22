package tachiyomi.domain.entries

import java.time.Instant

actual typealias EntryUpdateInstant = Instant

actual fun entryUpdateInstantFromEpochMilliseconds(epochMilliseconds: Long): EntryUpdateInstant {
    return Instant.ofEpochMilli(epochMilliseconds)
}
