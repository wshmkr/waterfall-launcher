package net.wshmkr.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import net.wshmkr.launcher.model.HomeTextColor
import net.wshmkr.launcher.model.PaletteStyle
import net.wshmkr.launcher.model.ThemeMode

@Composable
fun WaterfallLauncherTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    homeTextColor: HomeTextColor = HomeTextColor.AUTO,
    paletteStyle: PaletteStyle = PaletteStyle.FIDELITY,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val wallpaperColors = rememberSystemWallpaperColors()
    val colorScheme = rememberWallpaperColorScheme(wallpaperColors, paletteStyle, darkTheme)
    val contentColors = wallpaperContentColors(homeTextColor, wallpaperColors)
    val widthDp = LocalConfiguration.current.screenWidthDp
    CompositionLocalProvider(
        LocalDimensions provides dimensionsFor(widthDp),
        LocalWallpaperContentColors provides contentColors,
    ) {
        MaterialTheme(colorScheme = colorScheme) { content() }
    }
}
