package net.wshmkr.launcher.model

import android.app.PendingIntent
import android.app.RemoteInput
import android.os.UserHandle
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class NotificationInfo(
    val key: String,
    val id: Int,
    val packageName: String,
    val userHandle: UserHandle,
    val title: String? = null,
    val text: String? = null,
    val subText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val actions: ImmutableList<NotificationAction> = persistentListOf(),
    val contentIntent: PendingIntent? = null,
    val isOngoing: Boolean = false,
    val isMedia: Boolean = false,
    val isClearable: Boolean = true,
    val cancelsOnOpen: Boolean = false,
    val groupKey: String? = null,
    val isGroupSummary: Boolean = false,
)

@Immutable
data class NotificationAction(
    val title: String,
    val actionIntent: PendingIntent,
    val remoteInputs: ImmutableList<RemoteInput> = persistentListOf(),
    val reply: ReplyInput? = null,
    val isContextual: Boolean = false,
)

// The one input a reply action expects us to fill. Actions whose inputs only accept data
// (images, stickers) can't be satisfied from the panel and are dropped during extraction.
@Immutable
data class ReplyInput(
    val resultKey: String,
    val hint: String? = null,
    val choices: ImmutableList<String> = persistentListOf(),
    val allowsFreeFormInput: Boolean = true,
)
