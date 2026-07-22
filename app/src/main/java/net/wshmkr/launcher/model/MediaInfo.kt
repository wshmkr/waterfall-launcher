package net.wshmkr.launcher.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class MediaInfo(
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean,
    val packageName: String?,
    val albumArt: Bitmap?
) {
    fun hasSameDisplayContentAs(other: MediaInfo?): Boolean {
        return other != null &&
            title == other.title &&
            artist == other.artist &&
            isPlaying == other.isPlaying &&
            packageName == other.packageName &&
            albumArt === other.albumArt
    }
}
