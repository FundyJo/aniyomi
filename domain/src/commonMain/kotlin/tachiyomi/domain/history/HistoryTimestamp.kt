package tachiyomi.domain.history

expect class HistoryTimestamp

expect fun historyTimestampFromEpochMilliseconds(epochMilliseconds: Long): HistoryTimestamp

expect fun historyTimestampToEpochMilliseconds(timestamp: HistoryTimestamp): Long
