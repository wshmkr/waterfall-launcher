package net.wshmkr.launcher.util

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import net.wshmkr.launcher.model.MediaInfo
import net.wshmkr.launcher.service.LauncherNotificationListenerService

object MediaSessionHelper {
    fun getActiveMediaInfo(context: Context): Pair<MediaInfo?, MediaController?> {
        try {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val controllers = mediaSessionManager?.getActiveSessions(
                ComponentName(context, LauncherNotificationListenerService::class.java)
            )

            val activeController = controllers?.firstOrNull { controller ->
                controller.metadata != null && controller.playbackState != null
            }
            
            activeController?.let { controller ->
                val mediaInfo = extractMediaInfo(controller)
                return Pair(mediaInfo, controller)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return Pair(null, null)
    }

    private fun extractMediaInfo(controller: MediaController): MediaInfo {
        val (metadata, playbackState) = controller.metadata to controller.playbackState
        
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
