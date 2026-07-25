package net.wshmkr.launcher.ui.theme

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

// Null when the wallpaper reports no colors, as some live wallpapers do.
@Composable
fun rememberSystemWallpaperColors(): WallpaperColors? {
    val context = LocalContext.current
    val wallpaperManager = remember(context) { WallpaperManager.getInstance(context) }
    var colors by remember(wallpaperManager) {
        mutableStateOf<WallpaperColors?>(
            wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        )
    }
    DisposableEffect(wallpaperManager) {
        val listener = WallpaperManager.OnColorsChangedListener { changed, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                colors = changed
            }
        }
        wallpaperManager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { wallpaperManager.removeOnColorsChangedListener(listener) }
    }
    return colors
}
