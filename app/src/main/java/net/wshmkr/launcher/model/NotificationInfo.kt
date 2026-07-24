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
)

@Immutable
data class NotificationAction(
    val title: String,
    val actionIntent: PendingIntent?,
    val remoteInputs: ImmutableList<RemoteInput> = persistentListOf(),
)
