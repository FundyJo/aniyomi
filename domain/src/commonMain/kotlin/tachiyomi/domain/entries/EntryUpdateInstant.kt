package tachiyomi.domain.entries

expect class EntryUpdateInstant

expect fun entryUpdateInstantFromEpochMilliseconds(epochMilliseconds: Long): EntryUpdateInstant
