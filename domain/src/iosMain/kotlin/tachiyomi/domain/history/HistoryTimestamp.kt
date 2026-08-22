package tachiyomi.domain.history

actual data class HistoryTimestamp(val epochMilliseconds: Long)

actual fun historyTimestampFromEpochMilliseconds(epochMilliseconds: Long): HistoryTimestamp {
    return HistoryTimestamp(epochMilliseconds)
}

actual fun historyTimestampToEpochMilliseconds(timestamp: HistoryTimestamp): Long {
    return timestamp.epochMilliseconds
}
