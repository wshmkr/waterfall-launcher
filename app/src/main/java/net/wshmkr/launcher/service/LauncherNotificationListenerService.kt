package net.wshmkr.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.model.NotificationAction
import net.wshmkr.launcher.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LauncherNotificationListenerService : NotificationListenerService() {
    
    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.forEach { sbn ->
            val notification = extractNotification(sbn)

            if (!notification.isOngoing && !notification.isMedia) {
                notificationRepository.addNotification(notification)
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        notificationRepository.clearAll()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { statusBarNotification ->
            val notification = extractNotification(statusBarNotification)

            if (!notification.isOngoing && !notification.isMedia) {
                notificationRepository.addNotification(notification)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let { statusBarNotification ->
            val packageName = statusBarNotification.packageName
            val notificationId = statusBarNotification.id

            notificationRepository.removeNotification(packageName, notificationId)
        }
    }

    private fun extractNotification(sbn: StatusBarNotification): NotificationInfo {
        val androidNotification = sbn.notification
        val extras = androidNotification.extras

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString()
        val subText = extras.getCharSequence("android.subText")?.toString()

        val actions = androidNotification.actions?.mapNotNull { action ->
            action.title?.toString()?.let { title ->
                NotificationAction(
                    title = title,
                    actionIntent = action.actionIntent
                )
            }
        } ?: emptyList()
        
        val isOngoing = (androidNotification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        val isMedia = extras.getParcelable("android.mediaSession", android.media.session.MediaSession.Token::class.java) != null
        
        return NotificationInfo(
            id = sbn.id,
            packageName = sbn.packageName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            timestamp = sbn.postTime,
            actions = actions,
            contentIntent = androidNotification.contentIntent,
            isOngoing = isOngoing,
            isMedia = isMedia
        )
    }
}
