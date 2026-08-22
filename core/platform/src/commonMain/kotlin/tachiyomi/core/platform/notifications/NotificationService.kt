package tachiyomi.core.platform.notifications

@JvmInline
value class NotificationId(val value: String)

data class NotificationAction(
    val id: String,
    val label: String,
)

data class NotificationRequest(
    val id: NotificationId,
    val title: String,
    val body: String,
    val actions: List<NotificationAction> = emptyList(),
)

interface NotificationService {
    suspend fun show(request: NotificationRequest)

    suspend fun dismiss(id: NotificationId)
}
