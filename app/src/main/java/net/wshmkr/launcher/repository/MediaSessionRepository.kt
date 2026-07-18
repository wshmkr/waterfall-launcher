package net.wshmkr.launcher.repository

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.wshmkr.launcher.model.MediaInfo
import net.wshmkr.launcher.service.LauncherNotificationListenerService
import javax.inject.Inject
import javax.inject.Singleton

data class ActiveMediaSession(
    val mediaInfo: MediaInfo? = null,
    val controller: MediaController? = null,
)

@Singleton
class MediaSessionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationListenerComponent =
        ComponentName(context, LauncherNotificationListenerService::class.java)

    // Requires notification listener access; without it the flow just emits an
    // empty session. Collectors gate on the permission and resubscribe once granted.
    val activeMediaSession: Flow<ActiveMediaSession> = callbackFlow {
        val mediaSessionManager =
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        if (mediaSessionManager == null) {
            trySend(ActiveMediaSession())
            awaitClose { }
            return@callbackFlow
        }

        val handler = Handler(Looper.getMainLooper())
        var trackedControllers: List<Pair<MediaController, MediaController.Callback>> = emptyList()
        var lastMediaInfo: MediaInfo? = null

        fun publish() {
            val controller = trackedControllers
                .map { (controller, _) -> controller }
                .firstOrNull { it.metadata != null && it.playbackState != null }
            val extracted = controller?.let(::extractMediaInfo)
            val mediaInfo = if (extracted?.hasSameDisplayContentAs(lastMediaInfo) == true) {
                lastMediaInfo
            } else {
                extracted
            }
            lastMediaInfo = mediaInfo
            trySend(ActiveMediaSession(mediaInfo, controller))
        }

        var trackControllers: (List<MediaController>) -> Unit = { }

        fun refreshActiveSessions() {
            val controllers = try {
                mediaSessionManager.getActiveSessions(notificationListenerComponent)
            } catch (e: SecurityException) {
                emptyList()
            }
            trackControllers(controllers)
        }

        trackControllers = { controllers ->
            trackedControllers.forEach { (controller, callback) ->
                controller.unregisterCallback(callback)
            }
            trackedControllers = controllers.map { controller ->
                val callback = object : MediaController.Callback() {
                    override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                    override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                    override fun onSessionDestroyed() = refreshActiveSessions()
                }
                controller.registerCallback(callback, handler)
                controller to callback
            }
            publish()
        }

        val sessionsChangedListener =
            MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                trackControllers(controllers ?: emptyList())
            }

        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                notificationListenerComponent,
                handler
            )
        } catch (e: SecurityException) {
            // Notification access is not granted; refreshActiveSessions below emits empty.
        }
        refreshActiveSessions()

        awaitClose {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            trackedControllers.forEach { (controller, callback) ->
                controller.unregisterCallback(callback)
            }
        }
    }

    private fun extractMediaInfo(controller: MediaController): MediaInfo {
        val metadata = controller.metadata
        val playbackState = controller.playbackState

        return MediaInfo(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = controller.packageName,
            albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        )
    }
}
