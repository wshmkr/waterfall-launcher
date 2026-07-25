package net.wshmkr.launcher.ui.theme

import android.app.WallpaperColors
import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.Contrast
import com.materialkolor.dynamicColorScheme
import com.materialkolor.hct.Hct
import net.wshmkr.launcher.model.PaletteStyle
import com.materialkolor.PaletteStyle as KolorPaletteStyle

private const val DARK_TONE_LIMIT = 50.0

// Medium lifts onSurface to tone 100 in dark schemes, leaving surface tones — and the scrim — alone.
private val CONTRAST = Contrast.Medium.value

// The system palette only follows the wallpaper while Material You theming is on, so seed our own.
@Composable
fun rememberWallpaperColorScheme(
    wallpaperColors: WallpaperColors?,
    paletteStyle: PaletteStyle,
    darkTheme: Boolean,
): ColorScheme {
    val context = LocalContext.current
    val wallpaperSeed = wallpaperColors.seed()
    return remember(wallpaperSeed, paletteStyle, darkTheme, context) {
        dynamicColorScheme(
            // Wallpapers that report no colors still get the chosen style, seeded from the system.
            seedColor = wallpaperSeed ?: systemSeed(context, darkTheme),
            isDark = darkTheme,
            style = paletteStyle.toKolorStyle(),
            contrastLevel = CONTRAST,
        )
    }
}

// The hints measure the whole wallpaper, which its dominant color can contradict, but only a
// bitmap fills both in — live wallpaper colors carry neither and fall through to tone.
fun wallpaperIsDark(wallpaperColors: WallpaperColors?): Boolean {
    val colors = wallpaperColors ?: return true
    val hints = colors.colorHints
    if (hints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0) return false
    if (hints and WallpaperColors.HINT_SUPPORTS_DARK_THEME != 0) return true
    return Hct.fromInt(colors.primaryColor.toArgb()).tone <= DARK_TONE_LIMIT
}

private fun WallpaperColors?.seed(): Color? = this?.primaryColor?.let { Color(it.toArgb()) }

private fun systemSeed(context: Context, darkTheme: Boolean): Color =
    if (darkTheme) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary

private fun PaletteStyle.toKolorStyle(): KolorPaletteStyle = when (this) {
    PaletteStyle.VIBRANT -> KolorPaletteStyle.Vibrant
    PaletteStyle.EXPRESSIVE -> KolorPaletteStyle.Expressive
    PaletteStyle.NEUTRAL -> KolorPaletteStyle.Neutral
}
