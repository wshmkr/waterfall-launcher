package net.wshmkr.launcher.ui.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.dp
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
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(SEGMENT_HEIGHT)
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    colors = accentSegmentedColors(),
                    contentPadding = SEGMENT_CONTENT_PADDING,
                    // The filled container marks the selection; dropping the icon fits longer labels.
                    icon = {},
                ) {
                    Text(text = optionLabel(option), fontSize = dimensions.fontSmall, maxLines = 1)
                }
            }
        }
    }
}

// Material sizes segments for a toolbar; here they sit in a menu row, matching the scaled switch.
private val SEGMENT_HEIGHT = 32.dp

// Material's 8dp text inset outgrows the shorter row, leaving the label off center.
private val SEGMENT_CONTENT_PADDING = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
