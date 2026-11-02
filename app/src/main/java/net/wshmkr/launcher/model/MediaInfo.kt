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
    // Players re-publish the same art as fresh Bitmap instances; reuse keeps identity-keyed consumers stable.
    fun reusingArtFrom(other: MediaInfo?): MediaInfo {
        val previousArt = other?.albumArt ?: return this
        val pixelsUnchanged = albumArt !== previousArt && albumArt?.sameAs(previousArt) == true
        return if (pixelsUnchanged) copy(albumArt = previousArt) else this
    }
}
