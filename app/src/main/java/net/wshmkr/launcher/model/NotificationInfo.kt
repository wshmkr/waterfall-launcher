package net.wshmkr.launcher.model

import android.app.PendingIntent
import android.os.UserHandle
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class NotificationInfo(
    val key: String,
    val packageName: String,
    val userHandle: UserHandle,
    val title: String? = null,
    val text: String? = null,
    val subText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val postTime: Long,
    val actions: ImmutableList<NotificationAction> = persistentListOf(),
    val contentIntent: PendingIntent? = null,
    val isClearable: Boolean = true,
    val cancelsOnOpen: Boolean = false,
    val groupKey: String? = null,
    val isGroupSummary: Boolean = false,
    val detail: NotificationDetail? = null,
    val hasCustomView: Boolean = false,
) {
    // A custom view is unreadable to us but still means the app posted something worth showing.
    val hasContent: Boolean
        get() = !title.isNullOrBlank() || !text.isNullOrBlank() || detail != null || hasCustomView

    // A repost reuses the key, so the post time is what tells two instances apart.
    fun isSameInstanceAs(other: NotificationInfo): Boolean =
        key == other.key && postTime == other.postTime
}

// What the shade would reveal on expand. A notification uses at most one of these styles.
@Immutable
sealed interface NotificationDetail {

    @Immutable
    data class Conversation(
        val messages: ImmutableList<NotificationMessage>,
        val isGroup: Boolean = false,
    ) : NotificationDetail

    @Immutable
    data class Lines(val lines: ImmutableList<String>) : NotificationDetail

    @Immutable
    data class LongText(val text: String) : NotificationDetail
}

// A null sender marks a message the device user sent, including replies made from the panel.
@Immutable
data class NotificationMessage(
    val text: String,
    val sender: String? = null,
)

@Immutable
data class NotificationAction(
    val title: String,
    val actionIntent: PendingIntent,
    val reply: ReplyInput? = null,
)

// The one input a reply action expects us to fill.
@Immutable
data class ReplyInput(
    val resultKey: String,
    val hint: String? = null,
    val choices: ImmutableList<String> = persistentListOf(),
    val allowsFreeFormInput: Boolean = true,
)
