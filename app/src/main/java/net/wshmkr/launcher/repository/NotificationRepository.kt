package net.wshmkr.launcher.repository

import net.wshmkr.launcher.model.NotificationInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor() {

    private val _notifications = MutableStateFlow<Map<String, List<NotificationInfo>>>(emptyMap())
    val notifications: StateFlow<Map<String, List<NotificationInfo>>> = _notifications.asStateFlow()

    fun addNotification(notification: NotificationInfo) {
        val currentNotifications = _notifications.value.toMutableMap()
        val packageNotifications = currentNotifications[notification.packageName]?.toMutableList() ?: mutableListOf()

        packageNotifications.removeAll { it.id == notification.id }
        packageNotifications.add(notification)
        
        currentNotifications[notification.packageName] = packageNotifications
        _notifications.value = currentNotifications
    }

    fun removeNotification(packageName: String, notificationId: Int) {
        val currentNotifications = _notifications.value.toMutableMap()
        val packageNotifications = currentNotifications[packageName]?.toMutableList()
        
        if (packageNotifications != null) {
            packageNotifications.removeAll { it.id == notificationId }
            if (packageNotifications.isEmpty()) {
                currentNotifications.remove(packageName)
            } else {
                currentNotifications[packageName] = packageNotifications
            }
            _notifications.value = currentNotifications
        }
    }

    fun clearAll() {
        _notifications.value = emptyMap()
    }
}
