package net.wshmkr.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Translucent scrim tinted from the dynamic color scheme, letting the wallpaper show through.
@Composable
@ReadOnlyComposable
fun launcherScrim(): Color = MaterialTheme.colorScheme.surface.copy(alpha = SCRIM_ALPHA)

private const val SCRIM_ALPHA = 0.6f
