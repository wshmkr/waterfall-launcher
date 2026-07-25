package net.wshmkr.launcher.ui.theme

import android.app.WallpaperColors
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import net.wshmkr.launcher.model.HomeTextColor

// Colors for content painted over the wallpaper (clock, widgets, app labels), kept out of the
// Material scheme so light/dark theme never makes them invisible on an arbitrary wallpaper.
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

fun wallpaperContentColors(
    homeTextColor: HomeTextColor,
    wallpaperColors: WallpaperColors?,
): WallpaperContentColors = when (homeTextColor) {
    HomeTextColor.LIGHT -> LightContent
    HomeTextColor.DARK -> DarkContent
    HomeTextColor.AUTO -> if (wallpaperColors.isLight()) DarkContent else LightContent
}

private fun WallpaperColors?.isLight(): Boolean =
    (this?.primaryColor?.luminance() ?: 0f) > LIGHT_WALLPAPER_LUMINANCE

private const val LIGHT_WALLPAPER_LUMINANCE = 0.5f
