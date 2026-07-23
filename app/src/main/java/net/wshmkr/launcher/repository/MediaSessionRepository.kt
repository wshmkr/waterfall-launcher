package net.wshmkr.launcher.repository

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.HandlerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeSession: StateFlow<ActiveMediaSession> =
        LauncherNotificationListenerService.isConnected
            .flatMapLatest { connected ->
                if (!connected) flowOf(ActiveMediaSession()) else activeMediaSessionFlow()
            }
            .stateIn(repoScope, SharingStarted.WhileSubscribed(5_000), ActiveMediaSession())

    val mediaInfo: Flow<MediaInfo?> =
        activeSession.map { it.mediaInfo }.distinctUntilChanged()

    val isPlaying: Flow<Boolean> =
        activeSession.map { it.mediaInfo?.isPlaying == true }.distinctUntilChanged()

    val controller: Flow<MediaController?> =
        activeSession
            .map { it.controller }
            .distinctUntilChanged { old, new -> old === new }

    private fun activeMediaSessionFlow(): Flow<ActiveMediaSession> = callbackFlow {
        val mediaSessionManager =
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        if (mediaSessionManager == null) {
            trySend(ActiveMediaSession())
            awaitClose { }
            return@callbackFlow
        }

        // A fresh HandlerThread per subscription so we can quit it deterministically on close.
        val listenerThread = HandlerThread("MediaSessionListener").apply { start() }
        val handler = Handler(listenerThread.looper)
        val tracker = ActiveSessionTracker(
            manager = mediaSessionManager,
            listenerComponent = notificationListenerComponent,
            handler = handler,
            emit = { trySend(it) },
        )
        tracker.start()
        awaitClose {
            tracker.stop {
                listenerThread.quitSafely()
            }
        }
    }

    private class ActiveSessionTracker(
        private val manager: MediaSessionManager,
        private val listenerComponent: ComponentName,
        private val handler: Handler,
        private val emit: (ActiveMediaSession) -> Unit,
    ) {
        // All fields are read/written only on the handler thread; start/stop post to the handler
        // so callers on any thread converge onto the same serialised access.
        private var trackedControllers: List<Pair<MediaController, MediaController.Callback>> = emptyList()
        private var lastMediaInfo: MediaInfo? = null
        private var lastMetadata: MediaMetadata? = null
        private var lastPlaybackState: PlaybackState? = null
        private var lastControllerRef: MediaController? = null

        private val sessionsChangedListener =
            MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                trackControllers(controllers ?: emptyList())
            }

        fun start() {
            handler.post {
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
        }

        fun stop(onStopped: () -> Unit) {
            handler.post {
                try {
                    manager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
                } catch (e: SecurityException) {
                    // Ignore.
                }
                trackedControllers.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
                trackedControllers = emptyList()
                onStopped()
            }
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

            if (controller == null) {
                lastMetadata = null
                lastPlaybackState = null
                lastControllerRef = null
                lastMediaInfo = null
                emit(ActiveMediaSession(mediaInfo = null, controller = null))
                return
            }

            val metadata = controller.metadata
            val playbackState = controller.playbackState

            val snapshotUnchanged =
                controller === lastControllerRef &&
                metadata === lastMetadata &&
                playbackState === lastPlaybackState
            val mediaInfo = if (snapshotUnchanged && lastMediaInfo != null) {
                lastMediaInfo
            } else {
                val extracted = extractMediaInfo(controller, metadata, playbackState)
                if (extracted.hasSameDisplayContentAs(lastMediaInfo)) lastMediaInfo else extracted
            }

            lastControllerRef = controller
            lastMetadata = metadata
            lastPlaybackState = playbackState
            lastMediaInfo = mediaInfo

            emit(ActiveMediaSession(mediaInfo, controller))
        }

        private fun extractMediaInfo(
            controller: MediaController,
            metadata: MediaMetadata?,
            playbackState: PlaybackState?,
        ): MediaInfo {
            val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            return MediaInfo(
                title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
                packageName = controller.packageName,
                albumArt = albumArt,
                artExpected = albumArt != null || metadata.referencesArtUri()
            )
        }

        private fun MediaMetadata?.referencesArtUri(): Boolean {
            if (this == null) return false
            return getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) != null ||
                getString(MediaMetadata.METADATA_KEY_ART_URI) != null ||
                getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI) != null
        }
    }
}
