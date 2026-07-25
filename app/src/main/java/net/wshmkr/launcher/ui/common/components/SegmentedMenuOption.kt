package net.wshmkr.launcher.ui.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.ui.theme.accentSegmentedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedMenuOption(
    text: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val dimensions = LocalDimensions.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
    ) {
        Text(text = text, fontSize = dimensions.fontLarge, color = color)
        Spacer(modifier = Modifier.height(Spacing.small))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    colors = accentSegmentedColors(),
                    // The filled container marks the selection; dropping the icon fits longer labels.
                    icon = {},
                ) {
                    Text(text = optionLabel(option), maxLines = 1)
                }
            }
        }
    }
}
