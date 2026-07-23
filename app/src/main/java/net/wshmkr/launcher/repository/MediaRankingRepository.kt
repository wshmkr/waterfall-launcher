package net.wshmkr.launcher.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class MediaNotification(
    val packageName: String,
    val postTime: Long,
)

data class MediaRanking(
    // Primary signal; unlike a notification repost, a paused app can't inflate it
    // (YouTube reposts on screen-on).
    val lastPlayingTimes: Map<String, Long> = emptyMap(),
    // Cold-start fallback only.
    val notificationPostTimes: Map<String, Long> = emptyMap(),
)

@Singleton
class MediaRankingRepository @Inject constructor() {

    private val byNotificationKey = mutableMapOf<String, MediaNotification>()
    private val lastPlaying = mutableMapOf<String, Long>()

    private val _ranking = MutableStateFlow(MediaRanking())
    val ranking: StateFlow<MediaRanking> = _ranking.asStateFlow()

    fun onPosted(notificationKey: String, notification: MediaNotification) =
        update { byNotificationKey[notificationKey] = notification }

    fun onRemoved(notificationKey: String) = update { byNotificationKey.remove(notificationKey) }

    fun resetNotifications(entries: Map<String, MediaNotification>) = update {
        byNotificationKey.clear()
        byNotificationKey.putAll(entries)
    }

    fun onObservedPlaying(packageName: String, timestamp: Long) =
        update { lastPlaying[packageName] = timestamp }

    private fun update(block: () -> Unit) {
        synchronized(this) {
            block()
            _ranking.value = MediaRanking(
                lastPlayingTimes = lastPlaying.toMap(),
                notificationPostTimes = byNotificationKey.values
                    .groupingBy(MediaNotification::packageName)
                    .fold(Long.MIN_VALUE) { newest, entry -> maxOf(newest, entry.postTime) },
            )
        }
    }
}
