package net.wshmkr.launcher.service

import android.app.Notification
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.model.NotificationAction
import net.wshmkr.launcher.repository.MediaNotification
import net.wshmkr.launcher.repository.MediaRankingRepository
import net.wshmkr.launcher.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LauncherNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    }

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var mediaRankingRepository: MediaRankingRepository

    private val playingKeys = mutableSetOf<String>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        val active = activeNotifications?.toList() ?: emptyList()
        notificationRepository.reset(
            active.map(::extractNotification).filter { !it.isOngoing && !it.isMedia }
        )
        playingKeys.clear()
        mediaRankingRepository.resetNotifications(
            active.mapNotNull { sbn ->
                sbn.mediaSessionToken()?.let { token ->
                    recordPlaybackActivity(sbn.key, sbn.packageName, token)
                    sbn.key to MediaNotification(sbn.packageName, sbn.postTime)
                }
            }.toMap()
        )
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
        notificationRepository.clearAll()
        playingKeys.clear()
        mediaRankingRepository.resetNotifications(emptyMap())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val statusBarNotification = sbn ?: return
        val token = statusBarNotification.mediaSessionToken()
        if (token != null) {
            mediaRankingRepository.onPosted(
                statusBarNotification.key,
                MediaNotification(statusBarNotification.packageName, statusBarNotification.postTime),
            )
            recordPlaybackActivity(statusBarNotification.key, statusBarNotification.packageName, token)
            return
        }

        val notification = extractNotification(statusBarNotification)
        if (!notification.isOngoing) {
            notificationRepository.addNotification(notification)
        }
    }

    // Sampling on each repost catches play transitions even while the launcher UI isn't running.
    private fun recordPlaybackActivity(notificationKey: String, packageName: String, token: MediaSession.Token) {
        val state = try {
            MediaController(this, token).playbackState?.state
        } catch (e: Exception) {
            null
        }
        if (state == PlaybackState.STATE_PLAYING) {
            if (playingKeys.add(notificationKey)) {
                mediaRankingRepository.onObservedPlaying(packageName, System.currentTimeMillis())
            }
        } else {
            playingKeys.remove(notificationKey)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let { statusBarNotification ->
            val packageName = statusBarNotification.packageName
            val notificationId = statusBarNotification.id
            val userHandle = statusBarNotification.user

            playingKeys.remove(statusBarNotification.key)
            mediaRankingRepository.onRemoved(statusBarNotification.key)
            notificationRepository.removeNotification(packageName, notificationId, userHandle)
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
        }?.toImmutableList() ?: persistentListOf()
        
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isMedia = sbn.mediaSessionToken() != null

        return NotificationInfo(
            id = sbn.id,
            packageName = sbn.packageName,
            userHandle = sbn.user,
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

    private fun StatusBarNotification.mediaSessionToken(): MediaSession.Token? =
        notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)

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
