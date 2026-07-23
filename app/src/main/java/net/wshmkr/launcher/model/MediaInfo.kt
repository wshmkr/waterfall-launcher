package net.wshmkr.launcher.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class MediaInfo(
    val title: String?,
    val artist: String?,
    val packageName: String?,
    val albumArt: Bitmap?,
    // A null albumArt with this set means the art is still loading, not absent.
    val artExpected: Boolean,
) {
    fun hasSameDisplayContentAs(other: MediaInfo?): Boolean {
        return other != null &&
            title == other.title &&
            artist == other.artist &&
            packageName == other.packageName &&
            albumArt.hasSamePixelsAs(other.albumArt) &&
            artExpected == other.artExpected
    }
}

// Players re-publish the same art as fresh Bitmap instances.
private fun Bitmap?.hasSamePixelsAs(other: Bitmap?): Boolean {
    if (this === other) return true
    if (this == null || other == null) return false
    return sameAs(other)
}
