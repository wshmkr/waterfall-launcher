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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMediaSession: Flow<ActiveMediaSession> =
        LauncherNotificationListenerService.isConnected.flatMapLatest { connected ->
            if (!connected) flowOf(ActiveMediaSession()) else activeMediaSessionFlow()
        }

    private fun activeMediaSessionFlow(): Flow<ActiveMediaSession> = callbackFlow {
        val mediaSessionManager =
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        if (mediaSessionManager == null) {
            trySend(ActiveMediaSession())
            awaitClose { }
            return@callbackFlow
        }

        val tracker = ActiveSessionTracker(
            manager = mediaSessionManager,
            listenerComponent = notificationListenerComponent,
            handler = Handler(Looper.getMainLooper()),
            emit = { trySend(it) },
        )
        tracker.start()
        awaitClose { tracker.stop() }
    }

    private class ActiveSessionTracker(
        private val manager: MediaSessionManager,
        private val listenerComponent: ComponentName,
        private val handler: Handler,
        private val emit: (ActiveMediaSession) -> Unit,
    ) {
        private var trackedControllers: List<Pair<MediaController, MediaController.Callback>> = emptyList()
        private var lastMediaInfo: MediaInfo? = null

        private val sessionsChangedListener =
            MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                trackControllers(controllers ?: emptyList())
            }

        fun start() {
            try {
                manager.addOnActiveSessionsChangedListener(
                    sessionsChangedListener,
                    listenerComponent,
                    handler
                )
            } catch (e: SecurityException) {
                // Listener isn't bound yet; the outer flatMapLatest restarts us when it connects.
            }
            refreshActiveSessions()
        }

        fun stop() {
            try {
                manager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            } catch (e: SecurityException) {
                // Ignore.
            }
            trackedControllers.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
            trackedControllers = emptyList()
        }

        private fun refreshActiveSessions() {
            val controllers = try {
                manager.getActiveSessions(listenerComponent)
            } catch (e: SecurityException) {
                emptyList()
            }
            trackControllers(controllers)
        }

        private fun trackControllers(controllers: List<MediaController>) {
            trackedControllers.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
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

        private fun publish() {
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
            emit(ActiveMediaSession(mediaInfo, controller))
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
}
