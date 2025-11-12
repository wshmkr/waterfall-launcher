package net.wshmkr.launcher.model

import android.graphics.Bitmap


data class MediaInfo(
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean,
    val packageName: String?,
    val albumArt: Bitmap?
)
