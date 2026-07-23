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
import kotlinx.coroutines.launch
import net.wshmkr.launcher.model.MediaInfo
import net.wshmkr.launcher.service.LauncherNotificationListenerService
import javax.inject.Inject
import javax.inject.Singleton

private const val ART_SETTLE_TIMEOUT_MS = 2_000L

data class ActiveMediaSession(
    val mediaInfo: MediaInfo? = null,
    val isPlaying: Boolean = false,
    val controller: MediaController? = null,
)

@Singleton
class MediaSessionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRankingRepository: MediaRankingRepository,
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
            initialRanking = mediaRankingRepository.ranking.value,
            onObservedPlaying = { packageName ->
                mediaRankingRepository.onObservedPlaying(packageName, System.currentTimeMillis())
            },
            emit = { trySend(it) },
        )
        tracker.start()
        launch {
            mediaRankingRepository.ranking.collect { ranking ->
                handler.post { tracker.onRankingChanged(ranking) }
            }
        }
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
        initialRanking: MediaRanking,
        private val onObservedPlaying: (String) -> Unit,
        private val emit: (ActiveMediaSession) -> Unit,
    ) {
        // All fields are read/written only on the handler thread; start/stop post to the handler
        // so callers on any thread converge onto the same serialised access.
        private var trackedControllers: List<Pair<MediaController, MediaController.Callback>> = emptyList()
        private var mediaRanking: MediaRanking = initialRanking
        private var playingPackages: Set<String> = emptySet()
        private var lastMediaInfo: MediaInfo? = null
        private var lastControllerRef: MediaController? = null
        // The art URI extracted together with lastMediaInfo; keeps art loads paired with their track.
        private var lastArtUri: String? = null
        private var metadataStale: Boolean = true
        // A session that has supplied bitmap art is expected to supply it for later tracks too.
        private var selectedSessionSuppliesArt: Boolean = false
        private var artLoadUri: String? = null
        private var artLoadGeneration = 0
        private var artLoadDisposable: Disposable? = null
        private var failedArtUri: String? = null
        private var loadedArtUri: String? = null
        private val artSettleRunnable = Runnable { settleMissingArt() }

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
                handler.removeCallbacks(artSettleRunnable)
                onStopped()
            }
        }

        fun onRankingChanged(ranking: MediaRanking) {
            if (ranking == mediaRanking) return
            mediaRanking = ranking
            publish()
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
                        // Only the shown session's cached extraction depends on its metadata.
                        if (controller.sameSessionAs(lastControllerRef)) metadataStale = true
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
            // The metadata/playbackState getters are binder calls.
            val candidates = trackedControllers.mapNotNull { (controller, _) ->
                val metadata = controller.metadata ?: return@mapNotNull null
                val playbackState = controller.playbackState ?: return@mapNotNull null
                SessionSnapshot(controller, metadata, playbackState)
            }

            val holdLastShown = lastMediaInfo != null && trackedControllers.any { (controller, _) ->
                controller.sameSessionAs(lastControllerRef)
            }
            val lastShownPresent = candidates.any { it.controller.sameSessionAs(lastControllerRef) }

            val nowPlaying = candidates
                .filter { it.playbackState.state == PlaybackState.STATE_PLAYING }
                .map { it.controller.packageName }
                .toSet()
            (nowPlaying - playingPackages).forEach(onObservedPlaying)
            playingPackages = nowPlaying

            // Rank like the system controls: playing first, then most recently played. A paused
            // app reposting its notification must not win, so notification time is a fallback only.
            val selectionOrder = compareBy<SessionSnapshot>(
                { it.playbackState.state == PlaybackState.STATE_PLAYING },
                { mediaRanking.lastPlayingTimes[it.controller.packageName] ?: Long.MIN_VALUE },
                { mediaRanking.notificationPostTimes[it.controller.packageName] ?: Long.MIN_VALUE },
                { it.controller.sameSessionAs(lastControllerRef) },
            )
            val top = candidates.maxWithOrNull(selectionOrder)
            val selected = when {
                top == null -> null
                top.playbackState.state == PlaybackState.STATE_PLAYING -> top
                // Only real playback steals the widget from a shown session in a transient metadata gap.
                holdLastShown && !lastShownPresent -> null
                else -> top
            }

            if (selected == null) {
                // Hold through transient metadata gaps; clear once the shown session itself is gone.
                if (!holdLastShown) {
                    lastControllerRef = null
                    lastMediaInfo = null
                    lastArtUri = null
                    loadedArtUri = null
                    metadataStale = true
                    selectedSessionSuppliesArt = false
                    failedArtUri = null
                    cancelArtLoad()
                    handler.removeCallbacks(artSettleRunnable)
                    emit(ActiveMediaSession(mediaInfo = null, controller = null))
                }
                return
            }

            if (!selected.controller.sameSessionAs(lastControllerRef)) {
                selectedSessionSuppliesArt = false
                failedArtUri = null
                loadedArtUri = null
            }

            val cacheValid = !metadataStale &&
                selected.controller.sameSessionAs(lastControllerRef) &&
                lastMediaInfo != null
            val mediaInfo = if (cacheValid) {
                lastMediaInfo
            } else {
                metadataStale = false
                lastArtUri = selected.metadata.artUri()
                val extracted = extractMediaInfo(selected.controller, selected.metadata, lastArtUri)
                    .reusingArtFrom(lastMediaInfo)
                if (extracted.hasSameDisplayContentAs(lastMediaInfo)) lastMediaInfo else extracted
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

            syncArtLoad(mediaInfo, lastArtUri)
            scheduleArtSettle(mediaInfo)
        }

        // Resolves URI-only art: success supplies the bitmap, failure settles the track as artless.
        private fun syncArtLoad(mediaInfo: MediaInfo?, artUri: String?) {
            val wantedUri = artUri?.takeIf {
                mediaInfo?.albumArt == null && mediaInfo?.artExpected == true && it != failedArtUri
            }
            if (wantedUri == artLoadUri) return
            cancelArtLoad()
            artLoadUri = wantedUri ?: return
            val generation = artLoadGeneration
            artLoadDisposable = imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(wantedUri)
                    // Software bitmaps keep pixel comparison in reusingArtFrom safe.
                    .allowHardware(false)
                    .target(
                        onSuccess = { art -> handler.post { onArtLoadFinished(generation, wantedUri, art.toBitmap()) } },
                        onError = { handler.post { onArtLoadFinished(generation, wantedUri, null) } },
                    )
                    .build()
            )
        }

        private fun onArtLoadFinished(generation: Int, uri: String, art: Bitmap?) {
            // A cancelled load's queued callback must not be taken for the live one.
            if (generation != artLoadGeneration) return
            artLoadUri = null
            artLoadDisposable = null
            val current = lastMediaInfo ?: return
            lastMediaInfo = if (art != null) {
                loadedArtUri = uri
                current.copy(albumArt = art)
            } else {
                failedArtUri = uri
                current.copy(artExpected = false)
            }
            publish()
        }

        private fun cancelArtLoad() {
            artLoadGeneration++
            artLoadDisposable?.dispose()
            artLoadDisposable = null
            artLoadUri = null
        }

        // Art promised by the session heuristic may never arrive; settle as artless after a grace period.
        private fun scheduleArtSettle(mediaInfo: MediaInfo?) {
            handler.removeCallbacks(artSettleRunnable)
            val awaitingHeuristicArt = mediaInfo != null && mediaInfo.albumArt == null &&
                mediaInfo.artExpected && artLoadUri == null
            if (awaitingHeuristicArt) handler.postDelayed(artSettleRunnable, ART_SETTLE_TIMEOUT_MS)
        }

        private fun settleMissingArt() {
            val current = lastMediaInfo ?: return
            if (current.albumArt != null || !current.artExpected || artLoadUri != null) return
            // Disarm the heuristic too, or the next metadata republish would re-expect art.
            selectedSessionSuppliesArt = false
            lastMediaInfo = current.copy(artExpected = false)
            publish()
        }

        private fun extractMediaInfo(
            controller: MediaController,
            metadata: MediaMetadata,
            artUri: String?,
        ): MediaInfo {
            val albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                // Metadata republishes carry no bitmap for URI-art players; reattach instead of reloading.
                ?: lastMediaInfo?.albumArt?.takeIf { artUri != null && artUri == loadedArtUri }
            val artUriFailed = artUri != null && artUri == failedArtUri
            return MediaInfo(
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
                packageName = controller.packageName,
                albumArt = albumArt,
                artExpected = albumArt != null ||
                    (!artUriFailed && (artUri != null || selectedSessionSuppliesArt))
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
