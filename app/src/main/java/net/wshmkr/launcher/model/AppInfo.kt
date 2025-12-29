package net.wshmkr.launcher.model

import android.graphics.drawable.Drawable
import android.os.UserHandle

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val userHandle: UserHandle,
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

    val key: String
        get() = keyFor(packageName, userHandle)
}

fun keyFor(packageName: String, userHandle: UserHandle): String {
    return "${packageName}_${userHandle.hashCode()}"
}
