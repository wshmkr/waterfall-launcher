package net.wshmkr.launcher.util

import net.wshmkr.launcher.service.LauncherNotificationListenerService

object NotificationActionHelper {
    fun dismiss(key: String) {
        LauncherNotificationListenerService.getInstance()?.dismiss(key)
    }

    fun dismissAll(keys: List<String>) {
        LauncherNotificationListenerService.getInstance()?.dismissAll(keys)
    }
}
