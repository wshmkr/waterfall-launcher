package net.wshmkr.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Softened outline so separators read as hairlines rather than lit edges.
@Composable
@ReadOnlyComposable
fun sheetDivider(): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA)

private const val DIVIDER_ALPHA = 0.5f
