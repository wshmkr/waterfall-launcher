package net.wshmkr.launcher.repository

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.HandlerThread
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
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
    val isPlaying: Boolean = false,
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
        activeSession.map { it.isPlaying }.distinctUntilChanged()

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
            context = context,
            imageLoader = context.imageLoader,
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
        private val context: Context,
        private val imageLoader: ImageLoader,
        private val emit: (ActiveMediaSession) -> Unit,
    ) {
        // All fields are read/written only on the handler thread; start/stop post to the handler
        // so callers on any thread converge onto the same serialised access.
        private var trackedControllers: List<Pair<MediaController, MediaController.Callback>> = emptyList()
        private var lastMediaInfo: MediaInfo? = null
        private var lastControllerRef: MediaController? = null
        // Playback-only events reuse lastMediaInfo; extraction reruns only when metadata may have changed.
        private var metadataStale: Boolean = true
        // A session that has supplied bitmap art is expected to supply it for later tracks too.
        private var selectedSessionSuppliesArt: Boolean = false
        // In-flight art URI load; a failed URI is remembered so the track settles as artless.
        private var artLoadUri: String? = null
        private var artLoadDisposable: Disposable? = null
        private var failedArtUri: String? = null

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
                cancelArtLoad()
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
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        metadataStale = true
                        publish()
                    }
                    override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                    override fun onSessionDestroyed() = refreshActiveSessions()
                }
                controller.registerCallback(callback, handler)
                controller to callback
            }
            // Metadata may have changed during the re-registration gap.
            metadataStale = true
            publish()
        }

        private fun publish() {
            // Snapshot each controller's state once; the getters are binder calls.
            val candidates = trackedControllers.mapNotNull { (controller, _) ->
                val metadata = controller.metadata ?: return@mapNotNull null
                val playbackState = controller.playbackState ?: return@mapNotNull null
                SessionSnapshot(controller, metadata, playbackState)
            }

            // Falling back to the last shown session keeps pausing from jumping to another app.
            val selected = candidates.firstOrNull { it.playbackState.isActive }
                ?: candidates.firstOrNull { it.controller.sameSessionAs(lastControllerRef) }
                ?: candidates.firstOrNull()

            if (selected == null) {
                // Sessions can transiently report no metadata mid-track-change; keep the last
                // session on screen and clear only once the sessions themselves are gone.
                if (trackedControllers.isEmpty() || lastMediaInfo == null) {
                    lastControllerRef = null
                    lastMediaInfo = null
                    metadataStale = true
                    selectedSessionSuppliesArt = false
                    failedArtUri = null
                    cancelArtLoad()
                    emit(ActiveMediaSession(mediaInfo = null, controller = null))
                }
                return
            }

            if (!selected.controller.sameSessionAs(lastControllerRef)) {
                selectedSessionSuppliesArt = false
                failedArtUri = null
            }

            val cacheValid = !metadataStale &&
                selected.controller.sameSessionAs(lastControllerRef) &&
                lastMediaInfo != null
            val mediaInfo = if (cacheValid) {
                lastMediaInfo
            } else {
                metadataStale = false
                val extracted = extractMediaInfo(selected.controller, selected.metadata)
                if (extracted.hasSameDisplayContentAs(lastMediaInfo)) {
                    lastMediaInfo
                } else {
                    extracted.reusingArtFrom(lastMediaInfo)
                }
            }

            if (mediaInfo?.albumArt != null) {
                selectedSessionSuppliesArt = true
            }
            lastControllerRef = selected.controller
            lastMediaInfo = mediaInfo

            emit(
                ActiveMediaSession(
                    mediaInfo = mediaInfo,
                    // isActive includes transient states (buffering, connecting) so the button reflects intent immediately.
                    isPlaying = selected.playbackState.isActive,
                    controller = selected.controller,
                )
            )

            syncArtLoad(mediaInfo, selected.metadata)
        }

        // Resolves URI-only art: success supplies the bitmap, failure settles the track as artless.
        private fun syncArtLoad(mediaInfo: MediaInfo?, metadata: MediaMetadata) {
            val wantedUri = metadata.artUri()?.takeIf {
                mediaInfo?.albumArt == null && mediaInfo?.artExpected == true && it != failedArtUri
            }
            if (wantedUri == artLoadUri) return
            cancelArtLoad()
            artLoadUri = wantedUri ?: return
            artLoadDisposable = imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(wantedUri)
                    // Software bitmaps keep pixel comparison in hasSameDisplayContentAs safe.
                    .allowHardware(false)
                    .target(
                        onSuccess = { art -> handler.post { onArtLoadFinished(wantedUri, art.toBitmap()) } },
                        onError = { handler.post { onArtLoadFinished(wantedUri, null) } },
                    )
                    .build()
            )
        }

        private fun onArtLoadFinished(uri: String, art: Bitmap?) {
            if (uri != artLoadUri) return
            artLoadUri = null
            artLoadDisposable = null
            val current = lastMediaInfo ?: return
            lastMediaInfo = if (art != null) {
                current.copy(albumArt = art)
            } else {
                failedArtUri = uri
                current.copy(artExpected = false)
            }
            publish()
        }

        private fun cancelArtLoad() {
            artLoadDisposable?.dispose()
            artLoadDisposable = null
            artLoadUri = null
        }

        private fun extractMediaInfo(controller: MediaController, metadata: MediaMetadata): MediaInfo {
            val albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            val artUri = metadata.artUri()
            // A failed URI load settles the track as artless; history must not re-hold it.
            val artUnavailable = artUri != null && artUri == failedArtUri
            return MediaInfo(
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
                packageName = controller.packageName,
                albumArt = albumArt,
                artExpected = albumArt != null ||
                    (!artUnavailable && (artUri != null || selectedSessionSuppliesArt))
            )
        }

        // Tokens identify a session across the controller instances recreated on session-list changes.
        private fun MediaController.sameSessionAs(other: MediaController?): Boolean {
            return other != null && sessionToken == other.sessionToken
        }

        private fun MediaMetadata.artUri(): String? {
            return getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: getString(MediaMetadata.METADATA_KEY_ART_URI)
                ?: getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
        }

        private data class SessionSnapshot(
            val controller: MediaController,
            val metadata: MediaMetadata,
            val playbackState: PlaybackState,
        )
    }
}
