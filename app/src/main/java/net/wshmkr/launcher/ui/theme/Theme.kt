package net.wshmkr.launcher.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
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
    val colorScheme = rememberSheetSurfaceScheme(
        rememberWallpaperColorScheme(wallpaperColors, paletteStyle, darkTheme)
    )
    SystemBarIcons(darkTheme)
    val widthDp = LocalConfiguration.current.screenWidthDp
    CompositionLocalProvider(LocalDimensions provides dimensionsFor(widthDp)) {
        MaterialTheme(colorScheme = colorScheme) { content() }
    }
}

// Material floats dialogs and the search bar a tone above the option sheets. Flattening the role
// keeps every surface that sits over the wallpaper on one tone, with no per-component overrides.
@Composable
private fun rememberSheetSurfaceScheme(colorScheme: ColorScheme): ColorScheme =
    remember(colorScheme) {
        colorScheme.copy(surfaceContainerHigh = colorScheme.surfaceContainerLow)
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
