package net.wshmkr.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import net.wshmkr.launcher.model.HomeTextColor
import net.wshmkr.launcher.model.ThemeMode

@Composable
fun WaterfallLauncherTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    homeTextColor: HomeTextColor = HomeTextColor.AUTO,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    val widthDp = LocalConfiguration.current.screenWidthDp
    CompositionLocalProvider(
        LocalDimensions provides dimensionsFor(widthDp),
        LocalWallpaperContentColors provides rememberWallpaperContentColors(homeTextColor),
    ) {
        MaterialTheme(colorScheme = colorScheme) { content() }
    }
}
