package net.wshmkr.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Translucent scrim tinted from the dynamic color scheme, letting the wallpaper show through.
@Composable
@ReadOnlyComposable
fun launcherScrim(): Color = MaterialTheme.colorScheme.surface.copy(alpha = SCRIM_ALPHA)

// Softened outline so separators read as hairlines rather than lit edges.
@Composable
@ReadOnlyComposable
fun sheetDivider(): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA)

// Opacity for content that is present but unavailable, per Material's disabled state.
object ContentAlpha {
    const val disabled = 0.38f
}

private const val SCRIM_ALPHA = 0.6f

private const val DIVIDER_ALPHA = 0.5f
