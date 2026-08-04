package net.wshmkr.launcher.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import net.wshmkr.launcher.service.LauncherAccessibilityService
import net.wshmkr.launcher.service.LauncherNotificationListenerService

object NotificationPanelHelper {
    fun expandNotificationPanel(): Boolean {
        val service = LauncherAccessibilityService.getInstance()
        return service?.expandNotificationPanel() ?: false
    }

    fun dismissNotifications(keys: List<String>) {
        if (keys.isEmpty()) return
        val service = LauncherNotificationListenerService.getInstance() ?: return
        try {
            service.cancelNotifications(keys.toTypedArray())
        } catch (ignored: Exception) {
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(packageName)
    }
}
