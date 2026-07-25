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
import com.materialkolor.dynamicColorScheme
import net.wshmkr.launcher.model.PaletteStyle
import com.materialkolor.PaletteStyle as KolorPaletteStyle

// The system palette only follows the wallpaper while the user has wallpaper theming enabled,
// so the scheme is generated from the wallpaper's own seed color instead.
@Composable
fun rememberWallpaperColorScheme(
    wallpaperColors: WallpaperColors?,
    paletteStyle: PaletteStyle,
    darkTheme: Boolean,
): ColorScheme {
    val context = LocalContext.current
    val seed = wallpaperColors?.primaryColor?.toArgb()
    // Non-composable builder: calling one per branch would shift the slot table.
    return remember(seed, paletteStyle, darkTheme, context) {
        if (seed == null) {
            systemColorScheme(context, darkTheme)
        } else {
            dynamicColorScheme(
                seedColor = Color(seed),
                isDark = darkTheme,
                style = paletteStyle.toKolorStyle(),
            )
        }
    }
}

private fun systemColorScheme(context: Context, darkTheme: Boolean): ColorScheme =
    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

private fun PaletteStyle.toKolorStyle(): KolorPaletteStyle = when (this) {
    PaletteStyle.FIDELITY -> KolorPaletteStyle.Fidelity
    PaletteStyle.EXPRESSIVE -> KolorPaletteStyle.Expressive
    PaletteStyle.NEUTRAL -> KolorPaletteStyle.Neutral
}
