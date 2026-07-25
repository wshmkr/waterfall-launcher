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

// Tones at or below this are dark enough to need light content on top.
private const val DARK_TONE_LIMIT = 50.0

// Medium contrast lifts onSurface to tone 100 in dark schemes, matching the maximum contrast
// of hand-picked white while leaving surface tones — and so the scrim — untouched.
private val CONTRAST = Contrast.Medium.value

// The system palette only follows the wallpaper while the user has wallpaper theming enabled,
// so the scheme is generated from the wallpaper's own seed color instead.
@Composable
fun rememberWallpaperColorScheme(
    wallpaperColors: WallpaperColors?,
    paletteStyle: PaletteStyle,
    darkTheme: Boolean,
): ColorScheme {
    val context = LocalContext.current
    val seed = wallpaperColors.seed()
    // Non-composable builder: calling one per branch would shift the slot table.
    return remember(seed, paletteStyle, darkTheme, context) {
        if (seed == null) {
            systemColorScheme(context, darkTheme)
        } else {
            dynamicColorScheme(
                seedColor = Color(seed),
                isDark = darkTheme,
                style = paletteStyle.toKolorStyle(),
                contrastLevel = CONTRAST,
            )
        }
    }
}

// Drives every color in the app, so content always contrasts with what is behind it.
fun wallpaperIsDark(wallpaperColors: WallpaperColors?): Boolean {
    val seed = wallpaperColors.seed() ?: return true
    return Hct.fromInt(seed).tone <= DARK_TONE_LIMIT
}

private fun WallpaperColors?.seed(): Int? = this?.primaryColor?.toArgb()

private fun systemColorScheme(context: Context, darkTheme: Boolean): ColorScheme =
    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

private fun PaletteStyle.toKolorStyle(): KolorPaletteStyle = when (this) {
    PaletteStyle.FIDELITY -> KolorPaletteStyle.Fidelity
    PaletteStyle.EXPRESSIVE -> KolorPaletteStyle.Expressive
    PaletteStyle.NEUTRAL -> KolorPaletteStyle.Neutral
}
