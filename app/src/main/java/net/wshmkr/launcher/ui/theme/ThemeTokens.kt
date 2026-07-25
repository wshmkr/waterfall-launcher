package net.wshmkr.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

@Composable
@ReadOnlyComposable
fun launcherScrim(): Color = MaterialTheme.colorScheme.surface.copy(alpha = SCRIM_ALPHA)

@Composable
@ReadOnlyComposable
fun sheetDivider(): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA)

object ContentAlpha {
    const val disabled = 0.38f
}

private const val SCRIM_ALPHA = 0.6f

private const val DIVIDER_ALPHA = 0.5f
