package tachiyomi.domain.history

import java.util.Date

actual typealias HistoryTimestamp = Date

actual fun historyTimestampFromEpochMilliseconds(epochMilliseconds: Long): HistoryTimestamp {
    return Date(epochMilliseconds)
}

actual fun historyTimestampToEpochMilliseconds(timestamp: HistoryTimestamp): Long {
    return timestamp.time
}
