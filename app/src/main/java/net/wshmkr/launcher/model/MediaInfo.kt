package net.wshmkr.launcher.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class MediaInfo(
    val title: String?,
    val artist: String?,
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
            packageName == other.packageName &&
            albumArt.hasSamePixelsAs(other.albumArt) &&
            artExpected == other.artExpected
    }
}

// Players re-publish metadata with a fresh Bitmap instance for the same art;
// compare pixels so duplicates dedupe instead of re-triggering UI transitions.
private fun Bitmap?.hasSamePixelsAs(other: Bitmap?): Boolean {
    if (this === other) return true
    if (this == null || other == null) return false
    return sameAs(other)
}
