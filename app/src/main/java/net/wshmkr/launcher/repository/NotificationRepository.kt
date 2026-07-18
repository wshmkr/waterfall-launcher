package net.wshmkr.launcher.repository

import android.os.UserHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.wshmkr.launcher.model.NotificationInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor() {

    private val _notifications = MutableStateFlow<Map<String, Map<UserHandle, List<NotificationInfo>>>>(emptyMap())
    val notifications = _notifications.asStateFlow()

    fun addNotification(notification: NotificationInfo) {
        val currentNotifications = notifications.value.toMutableMap()
        val packageNotifications =
            currentNotifications[notification.packageName]?.toMutableMap() ?: mutableMapOf()
        val userNotifications =
            packageNotifications[notification.userHandle]?.toMutableList() ?: mutableListOf()

        userNotifications.removeAll { it.id == notification.id }
        userNotifications.add(notification)

        packageNotifications[notification.userHandle] = userNotifications
        currentNotifications[notification.packageName] = packageNotifications
        _notifications.value = currentNotifications
    }

    fun removeNotification(packageName: String, notificationId: Int, userHandle: UserHandle) {
        val currentNotifications = notifications.value.toMutableMap()
        val packageNotifications = currentNotifications[packageName]?.toMutableMap() ?: return
        val userNotifications = packageNotifications[userHandle]?.toMutableList() ?: return

        userNotifications.removeAll { it.id == notificationId }

        if (userNotifications.isEmpty()) {
            packageNotifications.remove(userHandle)
        } else {
            packageNotifications[userHandle] = userNotifications
        }

        if (packageNotifications.isEmpty()) {
            currentNotifications.remove(packageName)
        } else {
            currentNotifications[packageName] = packageNotifications
        }

        _notifications.value = currentNotifications
    }

    fun clearAll() {
        _notifications.value = emptyMap()
    }
}
