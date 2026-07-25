package net.wshmkr.launcher.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import net.wshmkr.launcher.model.PaletteStyle
import net.wshmkr.launcher.model.ThemeMode

@Composable
fun WaterfallLauncherTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    paletteStyle: PaletteStyle = PaletteStyle.VIBRANT,
    content: @Composable () -> Unit,
) {
    val wallpaperColors = rememberSystemWallpaperColors()
    val darkTheme = when (themeMode) {
        ThemeMode.AUTO -> wallpaperIsDark(wallpaperColors)
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = rememberWallpaperColorScheme(wallpaperColors, paletteStyle, darkTheme)
    SystemBarIcons(darkTheme)
    val widthDp = LocalConfiguration.current.screenWidthDp
    CompositionLocalProvider(LocalDimensions provides dimensionsFor(widthDp)) {
        MaterialTheme(colorScheme = colorScheme) { content() }
    }
}

// The bars sit over the wallpaper, so their icons follow the same lightness as app content.
@Composable
private fun SystemBarIcons(darkTheme: Boolean) {
    val view = LocalView.current
    SideEffect {
        if (view.isInEditMode) return@SideEffect
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
