package net.wshmkr.launcher.ui.common.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import net.wshmkr.launcher.ui.common.icons.DragIndicatorIcon
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing

@Composable
fun ReorderHandle(label: String, modifier: Modifier = Modifier) {
    Icon(
        painter = DragIndicatorIcon(),
        contentDescription = "Reorder $label",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .size(LocalDimensions.current.iconLarge)
            .padding(Spacing.small),
    )
}

// Drag gestures are invisible to accessibility services, so reordering is mirrored as custom actions.
fun <K> reorderActions(
    key: K,
    previousKey: K?,
    nextKey: K?,
    move: (K, K) -> Unit,
): List<CustomAccessibilityAction> = buildList {
    if (previousKey != null) {
        add(CustomAccessibilityAction("Move up") { move(key, previousKey); true })
    }
    if (nextKey != null) {
        add(CustomAccessibilityAction("Move down") { move(key, nextKey); true })
    }
}
