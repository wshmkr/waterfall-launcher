package net.wshmkr.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun WaterfallLauncherTheme(content: @Composable () -> Unit) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    CompositionLocalProvider(LocalDimensions provides dimensionsFor(widthDp)) {
        MaterialTheme { content() }
    }
}
