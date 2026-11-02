package net.wshmkr.launcher.repository

import android.os.UserHandle
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.wshmkr.launcher.model.NotificationInfo
import javax.inject.Inject
import javax.inject.Singleton

typealias NotificationMap = Map<String, Map<UserHandle, List<NotificationInfo>>>

@Singleton
class NotificationRepository @Inject constructor() {

    private val _notifications = MutableStateFlow<NotificationMap>(persistentMapOf())

    fun addNotification(notification: NotificationInfo) {
        _notifications.update { current ->
            val packageNotifications =
                current[notification.packageName]?.toMutableMap() ?: mutableMapOf()
            val userNotifications =
                packageNotifications[notification.userHandle]?.toMutableList() ?: mutableListOf()

            userNotifications.removeAll { it.key == notification.key }
            userNotifications.add(notification)

            packageNotifications[notification.userHandle] = userNotifications

            current.toMutableMap().apply {
                put(notification.packageName, packageNotifications)
            }
        }
    }

    fun removeNotification(packageName: String, key: String, userHandle: UserHandle) {
        _notifications.update { current ->
            val packageNotifications = current[packageName] ?: return@update current
            val userNotifications = packageNotifications[userHandle] ?: return@update current
            // Notifications the rows never held are removed on every repost; don't rebuild for them.
            if (userNotifications.none { it.key == key }) return@update current

            val remaining = userNotifications.filterNot { it.key == key }
            val nextUsers = packageNotifications.toMutableMap()
            if (remaining.isEmpty()) nextUsers.remove(userHandle)
            else nextUsers[userHandle] = remaining

            current.toMutableMap().apply {
                if (nextUsers.isEmpty()) remove(packageName)
                else put(packageName, nextUsers)
            }
        }
    }

    fun reset(seed: Iterable<NotificationInfo>) {
        _notifications.value = seed
            .groupBy { it.packageName }
            .mapValues { (_, inPackage) -> inPackage.groupBy { it.userHandle } }
    }

    fun clearAll() {
        _notifications.value = persistentMapOf()
    }

    val notifications: StateFlow<NotificationMap> = _notifications.asStateFlow()
}
