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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import net.wshmkr.launcher.model.HomeTextColor

// Colors for content painted directly over the wallpaper (clock, widgets, app labels).
// Kept independent of the Material color scheme so light/dark theme never turns this
// content invisible over an arbitrary wallpaper.
data class WallpaperContentColors(
    val primary: Color,
    val secondary: Color,
)

private fun contentColors(dark: Boolean): WallpaperContentColors {
    val base = if (dark) Color(0xFF1B1B1B) else Color.White
    return WallpaperContentColors(primary = base, secondary = base.copy(alpha = 0.7f))
}

private val LightContent = contentColors(dark = false)
private val DarkContent = contentColors(dark = true)

val LocalWallpaperContentColors = staticCompositionLocalOf { LightContent }

@Composable
fun rememberWallpaperContentColors(homeTextColor: HomeTextColor): WallpaperContentColors =
    when (homeTextColor) {
        HomeTextColor.LIGHT -> LightContent
        HomeTextColor.DARK -> DarkContent
        HomeTextColor.AUTO -> if (wallpaperIsLight()) DarkContent else LightContent
    }

@Composable
private fun wallpaperIsLight(): Boolean {
    val context = LocalContext.current
    val wallpaperManager = remember(context) { WallpaperManager.getInstance(context) }
    var isLight by remember { mutableStateOf(wallpaperManager.systemWallpaperIsLight()) }
    DisposableEffect(wallpaperManager) {
        val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                isLight = colors?.isLight() ?: false
            }
        }
        wallpaperManager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { wallpaperManager.removeOnColorsChangedListener(listener) }
    }
    return isLight
}

private fun WallpaperManager.systemWallpaperIsLight(): Boolean =
    getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.isLight() ?: false

private fun WallpaperColors.isLight(): Boolean =
    primaryColor.luminance() > LIGHT_WALLPAPER_LUMINANCE

private const val LIGHT_WALLPAPER_LUMINANCE = 0.5f
