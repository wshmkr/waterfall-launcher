package net.wshmkr.launcher.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val isSystemApp: Boolean = false,

    var isFavorite: Boolean,
    var isHidden: Boolean,
    var doNotSuggest: Boolean,
    var isSuggested: Boolean = false,
    var notifications: List<NotificationInfo> = emptyList(),
) {
    val mostRecentNotification: NotificationInfo?
        get() = notifications.maxByOrNull { it.timestamp }

    val hasNotifications: Boolean
        get() = !notifications.isEmpty()
}
