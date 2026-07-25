package net.wshmkr.launcher.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Controls share one language: an outlined shell that fills with primaryContainer when active.
@Composable
fun accentButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

// The thumb carries the accent, so the rail behind it stays on a neutral surface tone.
// Labeled switches keep their fill while off so the thumb text keeps its contrast.
@Composable
fun accentSwitchColors(alwaysFilled: Boolean = false): SwitchColors {
    val colorScheme = MaterialTheme.colorScheme
    val rail = colorScheme.surfaceContainerHighest
    return SwitchDefaults.colors(
        checkedThumbColor = colorScheme.primaryContainer,
        checkedTrackColor = rail,
        checkedBorderColor = colorScheme.outline,
        uncheckedThumbColor = if (alwaysFilled) colorScheme.primaryContainer else colorScheme.outline,
        uncheckedTrackColor = if (alwaysFilled) rail else Color.Transparent,
        uncheckedBorderColor = colorScheme.outline,
    )
}

// The search bar floats over a scrim tinted like the app surface, so matching that surface would
// leave it barely visible. Inverting the pair keeps the field legible against its own backdrop.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun invertedSearchBarColors(): SearchBarColors = SearchBarDefaults.colors(
    containerColor = MaterialTheme.colorScheme.inverseSurface,
    inputFieldColors = invertedSearchInputColors(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun invertedSearchInputColors(): TextFieldColors {
    val colorScheme = MaterialTheme.colorScheme
    return SearchBarDefaults.inputFieldColors(
        focusedTextColor = colorScheme.inverseOnSurface,
        unfocusedTextColor = colorScheme.inverseOnSurface,
        cursorColor = colorScheme.inversePrimary,
        focusedLeadingIconColor = colorScheme.inverseOnSurface,
        unfocusedLeadingIconColor = colorScheme.inverseOnSurface,
        focusedTrailingIconColor = colorScheme.inverseOnSurface,
        unfocusedTrailingIconColor = colorScheme.inverseOnSurface,
        // OutlineVariant flips with the scheme like the inverted ink does, landing a step softer
        // than the query text instead of halfway to the container.
        focusedPlaceholderColor = colorScheme.outlineVariant,
        unfocusedPlaceholderColor = colorScheme.outlineVariant,
        focusedContainerColor = colorScheme.inverseSurface,
        unfocusedContainerColor = colorScheme.inverseSurface,
        disabledContainerColor = colorScheme.inverseSurface,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun accentSegmentedColors(): SegmentedButtonColors = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    activeBorderColor = MaterialTheme.colorScheme.outline,
    inactiveContainerColor = Color.Transparent,
    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    inactiveBorderColor = MaterialTheme.colorScheme.outline,
)
