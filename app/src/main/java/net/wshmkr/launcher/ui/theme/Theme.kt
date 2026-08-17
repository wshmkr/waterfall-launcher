package net.wshmkr.launcher.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import net.wshmkr.launcher.model.LauncherFont
import net.wshmkr.launcher.model.PaletteStyle

@Composable
fun WaterfallLauncherTheme(
    paletteStyle: PaletteStyle,
    launcherFont: LauncherFont = LauncherFont.Bundled,
    content: @Composable () -> Unit,
) {
    val wallpaperColors = rememberSystemWallpaperColors()
    // A forced polarity would leave content illegible over the wallpaper.
    val darkTheme = remember(wallpaperColors) { wallpaperIsDark(wallpaperColors) }
    val colorScheme = rememberSheetSurfaceScheme(
        rememberWallpaperColorScheme(wallpaperColors, paletteStyle, darkTheme)
    )
    SystemBarIcons(darkTheme)
    val widthDp = LocalConfiguration.current.screenWidthDp
    val fontFamily = rememberFontFamily(launcherFont)
    CompositionLocalProvider(
        LocalDimensions provides dimensionsFor(widthDp),
        // No Surface sets the ambient ink, since the app draws straight over the wallpaper.
        LocalContentColor provides colorScheme.onSurface,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = rememberLauncherTypography(fontFamily),
        ) { content() }
    }
}

// Material floats dialogs a tone above the option sheets; over the wallpaper they should match.
@Composable
private fun rememberSheetSurfaceScheme(colorScheme: ColorScheme): ColorScheme =
    remember(colorScheme) {
        colorScheme.copy(surfaceContainerHigh = colorScheme.surfaceContainerLow)
    }

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
