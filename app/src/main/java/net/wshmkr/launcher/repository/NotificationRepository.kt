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
            val packageNotifications = current[packageName]?.toMutableMap() ?: return@update current
            val userNotifications = packageNotifications[userHandle]?.toMutableList() ?: return@update current

            userNotifications.removeAll { it.key == key }

            if (userNotifications.isEmpty()) {
                packageNotifications.remove(userHandle)
            } else {
                packageNotifications[userHandle] = userNotifications
            }

            current.toMutableMap().apply {
                if (packageNotifications.isEmpty()) remove(packageName)
                else put(packageName, packageNotifications)
            }
        }
    }

    fun reset(seed: Iterable<NotificationInfo>) {
        val seedList = seed.toList()
        val seedKeys = seedList.mapTo(HashSet()) { it.key }
        _notifications.update { existing ->
            val next = mutableMapOf<String, MutableMap<UserHandle, MutableList<NotificationInfo>>>()
            for (notification in seedList) {
                val userMap = next.getOrPut(notification.packageName) { mutableMapOf() }
                val list = userMap.getOrPut(notification.userHandle) { mutableListOf() }
                list.removeAll { it.key == notification.key }
                list.add(notification)
            }
            // Preserve entries added concurrently between seed capture and this update.
            for ((packageName, userMap) in existing) {
                for ((userHandle, notifications) in userMap) {
                    for (notification in notifications) {
                        if (notification.key !in seedKeys) {
                            val destUsers = next.getOrPut(packageName) { mutableMapOf() }
                            val destList = destUsers.getOrPut(userHandle) { mutableListOf() }
                            destList.add(notification)
                        }
                    }
                }
            }
            next
        }
    }

    fun clearAll() {
        _notifications.value = persistentMapOf()
    }

    val notifications: StateFlow<NotificationMap> = _notifications.asStateFlow()
}
