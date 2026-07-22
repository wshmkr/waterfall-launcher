package net.wshmkr.launcher.model

import android.graphics.drawable.Drawable
import android.os.UserHandle

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val userHandle: UserHandle,
    val isSystemApp: Boolean = false,

    val isFavorite: Boolean,
    val isHidden: Boolean,
    val doNotSuggest: Boolean,
    val isSuggested: Boolean = false,
    val notifications: List<NotificationInfo> = emptyList(),
) {
    val mostRecentNotification: NotificationInfo?
        get() = notifications.maxByOrNull { it.timestamp }

    val hasNotifications: Boolean
        get() = notifications.isNotEmpty()

    val key: String
        get() = keyFor(packageName, userHandle)
}

fun keyFor(packageName: String, userHandle: UserHandle): String {
    return "${packageName}_${userHandle.hashCode()}"
}

val String.sectionLetter: String
    get() = firstOrNull()?.uppercaseChar()?.toString() ?: "#"
