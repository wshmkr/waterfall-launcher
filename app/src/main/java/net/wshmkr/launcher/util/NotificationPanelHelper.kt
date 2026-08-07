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

    // False when the cancel never reached the listener, so a caller hiding these optimistically
    // knows not to: rebinding the service leaves a window where there is nothing to cancel with.
    fun dismissNotifications(keys: List<String>): Boolean {
        if (keys.isEmpty()) return false
        val service = LauncherNotificationListenerService.getInstance() ?: return false
        return try {
            service.cancelNotifications(keys.toTypedArray())
            true
        } catch (ignored: Exception) {
            false
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
