package net.wshmkr.launcher.service

import android.app.Notification
import android.media.session.MediaSession
import android.os.Bundle
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
        activeNotifications?.forEach {
            onNotificationPosted(it)
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
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        extractMessageText(extras)?.let { text = it }

        val actions = notification.actions?.mapNotNull { action ->
            action.title?.toString()?.let { title ->
                NotificationAction(
                    title = title,
                    actionIntent = action.actionIntent
                )
            }
        } ?: emptyList()
        
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isMedia = extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java) != null
        
        return NotificationInfo(
            id = sbn.id,
            packageName = sbn.packageName,
            title = title,
            text = text,
            subText = subText,
            timestamp = sbn.postTime,
            actions = actions,
            contentIntent = notification.contentIntent,
            isOngoing = isOngoing,
            isMedia = isMedia
        )
    }

    private fun extractMessageText(extras: Bundle): String? {
        if (!extras.containsKey(Notification.EXTRA_MESSAGES)) {
            return null
        }

        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES, Bundle::class.java)

        if (!messages.isNullOrEmpty()) {
            val lastMessageBundle = messages.last() as Bundle
            val lastMessageText = lastMessageBundle.getCharSequence("text")

            if (!lastMessageText.isNullOrBlank()) {
                return lastMessageText.toString()
            }
        }

        return null
    }
}
