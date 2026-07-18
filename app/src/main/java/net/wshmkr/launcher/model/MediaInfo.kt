package net.wshmkr.launcher.model

import android.graphics.Bitmap


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
            (albumArt == null) == (other.albumArt == null)
    }
}
