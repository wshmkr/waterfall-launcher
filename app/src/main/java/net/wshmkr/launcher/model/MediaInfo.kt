package net.wshmkr.launcher.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class MediaInfo(
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean,
    val packageName: String?,
    val albumArt: Bitmap?,
    // Metadata references art (bitmap or URI); a null albumArt with this set
    // means the bitmap hasn't loaded yet rather than the track having no art.
    val artExpected: Boolean,
) {
    fun hasSameDisplayContentAs(other: MediaInfo?): Boolean {
        return other != null &&
            title == other.title &&
            artist == other.artist &&
            isPlaying == other.isPlaying &&
            packageName == other.packageName &&
            albumArt === other.albumArt &&
            artExpected == other.artExpected
    }
}
