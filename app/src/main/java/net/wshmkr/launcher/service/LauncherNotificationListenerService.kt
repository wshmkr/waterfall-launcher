package net.wshmkr.launcher.service

import android.app.Notification
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.app.NotificationCompat
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.model.NotificationAction
import net.wshmkr.launcher.model.NotificationDetail
import net.wshmkr.launcher.model.NotificationMessage
import net.wshmkr.launcher.model.ReplyInput
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

        @Volatile
        private var instance: LauncherNotificationListenerService? = null

        fun getInstance(): LauncherNotificationListenerService? = instance
    }

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var mediaRankingRepository: MediaRankingRepository

    private val playingKeys = mutableSetOf<String>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        _isConnected.value = true
        val active = activeNotifications?.toList() ?: emptyList()
        notificationRepository.reset(
            active.filter { it.isRowCandidate() }
                .map(::extractNotification)
                .filter { it.hasContent }
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
        if (instance == this) instance = null
        _isConnected.value = false
        notificationRepository.clearAll()
        playingKeys.clear()
        mediaRankingRepository.resetNotifications(emptyMap())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
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
            // A tracked row reposted as media would otherwise linger with its pre-media content.
            notificationRepository.removeNotification(
                statusBarNotification.packageName,
                statusBarNotification.key,
                statusBarNotification.user,
            )
            return
        }

        // A repost can strip a notification to nothing or turn it ongoing, so drop what we hold.
        val notification = statusBarNotification
            .takeIf { it.isRowCandidate() }
            ?.let(::extractNotification)
            ?.takeIf { it.hasContent }
        if (notification == null) {
            notificationRepository.removeNotification(
                statusBarNotification.packageName,
                statusBarNotification.key,
                statusBarNotification.user,
            )
            return
        }

        notificationRepository.addNotification(notification)
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
            playingKeys.remove(statusBarNotification.key)
            mediaRankingRepository.onRemoved(statusBarNotification.key)
            notificationRepository.removeNotification(
                statusBarNotification.packageName,
                statusBarNotification.key,
                statusBarNotification.user,
            )
        }
    }

    private fun extractNotification(sbn: StatusBarNotification): NotificationInfo {
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val detail = notification.extractDetail()
        // Apps often leave EXTRA_TEXT stale, or unset on a styled notification the preview needs.
        if (detail is NotificationDetail.Conversation || text.isNullOrBlank()) {
            text = detail?.collapsedText()
        }

        // Contextual actions trail the app's own buttons, matching how the shade ranks them.
        val actions = notification.actions
            ?.sortedBy { it.isContextual }
            ?.mapNotNull { it.toNotificationAction() }
            ?.toImmutableList() ?: persistentListOf()

        return NotificationInfo(
            key = sbn.key,
            packageName = sbn.packageName,
            userHandle = sbn.user,
            title = title,
            text = text,
            subText = subText,
            // `when` is the app's event time; apps that don't set one leave it 0.
            timestamp = if (notification.`when` > 0) notification.`when` else sbn.postTime,
            postTime = sbn.postTime,
            actions = actions,
            contentIntent = notification.contentIntent,
            isClearable = sbn.isClearable,
            cancelsOnOpen = (notification.flags and Notification.FLAG_AUTO_CANCEL) != 0,
            groupKey = sbn.groupKey,
            isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            detail = detail,
            hasCustomView = notification.hasCustomView(),
        )
    }

    // The deprecated fields are still where custom layouts land, whichever builder set them.
    @Suppress("DEPRECATION")
    private fun Notification.hasCustomView(): Boolean =
        contentView != null || bigContentView != null

    // Every detail is built with at least one line, so there is always something to collapse to.
    private fun NotificationDetail.collapsedText(): String = when (this) {
        is NotificationDetail.Conversation -> messages.last().text
        is NotificationDetail.Lines -> lines.first()
        is NotificationDetail.LongText -> text
    }

    private fun Notification.extractDetail(): NotificationDetail? {
        extractConversation()?.let { return it }

        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { line -> line?.toString()?.takeIf { it.isNotBlank() } }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return NotificationDetail.Lines(it.toImmutableList()) }

        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?.let { return NotificationDetail.LongText(it) }

        return null
    }

    private fun Notification.extractConversation(): NotificationDetail.Conversation? {
        val style = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(this) ?: return null
        // Historic messages precede the live ones, so together they read in order.
        val messages = (style.historicMessages + style.messages).mapNotNull { message ->
            message.text?.toString()?.takeIf { it.isNotBlank() }?.let { body ->
                NotificationMessage(
                    text = body,
                    sender = message.person?.name?.toString(),
                )
            }
        }
        if (messages.isEmpty()) return null

        return NotificationDetail.Conversation(
            messages = messages.toImmutableList(),
            isGroup = style.isGroupConversation,
        )
    }

    private fun Notification.Action.toNotificationAction(): NotificationAction? {
        val label = title?.toString()?.takeIf { it.isNotBlank() } ?: return null
        // A null intent renders a button that can never do anything.
        val intent = actionIntent ?: return null
        val inputs = remoteInputs?.toList().orEmpty()
        val fillable = inputs.firstOrNull { it.allowFreeFormInput || !it.choices.isNullOrEmpty() }
        // Every input wants data we can't produce, so firing this bare would send an empty reply.
        if (inputs.isNotEmpty() && fillable == null) return null

        return NotificationAction(
            title = label,
            actionIntent = intent,
            reply = fillable?.let { input ->
                ReplyInput(
                    resultKey = input.resultKey,
                    hint = input.label?.toString(),
                    choices = input.choices?.map { it.toString() }?.toImmutableList()
                        ?: persistentListOf(),
                    allowsFreeFormInput = input.allowFreeFormInput,
                )
            },
        )
    }

    // Ongoing and media notifications belong to other surfaces, so the rows never track them.
    private fun StatusBarNotification.isRowCandidate(): Boolean =
        (notification.flags and Notification.FLAG_ONGOING_EVENT) == 0 && mediaSessionToken() == null

    private fun StatusBarNotification.mediaSessionToken(): MediaSession.Token? =
        notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)

}
