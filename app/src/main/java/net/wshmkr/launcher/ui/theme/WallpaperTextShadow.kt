package net.wshmkr.launcher.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// Home and app-list text lands straight on the wallpaper, where a contrasting ink alone can fail.
@Composable
fun OverWallpaper(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(shadow = wallpaperTextShadow()),
        content = content,
    )
}

// Sheets paint their own opaque surface, and a shadow there only reads as muddy.
@Composable
fun OnOpaqueSurface(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(shadow = null),
        content = content,
    )
}

@Composable
private fun wallpaperTextShadow(): Shadow {
    val ink = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    return remember(ink, density) {
        Shadow(
            // The shadow has to oppose the ink, which already tracks the wallpaper's own tone.
            color = (if (ink.luminance() > MID_LUMINANCE) Color.Black else Color.White)
                .copy(alpha = SHADOW_ALPHA),
            offset = with(density) { Offset(0f, SHADOW_OFFSET.toPx()) },
            blurRadius = with(density) { SHADOW_BLUR.toPx() },
        )
    }
}

private val SHADOW_OFFSET = 1.dp

private val SHADOW_BLUR = 3.dp

private const val SHADOW_ALPHA = 0.5f

private const val MID_LUMINANCE = 0.5f
