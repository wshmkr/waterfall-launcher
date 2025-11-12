package net.wshmkr.launcher.model

import android.app.PendingIntent

data class NotificationInfo(
    val id: Int,
    val packageName: String,
    val title: String,
    val text: String,
    val bigText: String? = null,
    val subText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val actions: List<NotificationAction> = emptyList(),
    val contentIntent: PendingIntent? = null,
    val isOngoing: Boolean = false,
    val isMedia: Boolean = false,
)

data class NotificationAction(
    val title: String,
    val actionIntent: PendingIntent?,
)
