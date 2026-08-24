package net.wshmkr.launcher.ui.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import net.wshmkr.launcher.repository.IconPackInfo
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.ui.common.icons.CheckIcon
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.OnOpaqueSurface
import net.wshmkr.launcher.ui.theme.Spacing

@Composable
fun IconPackPickerDialog(
    packs: List<IconPackInfo>?,
    selectedPackage: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    OnOpaqueSurface {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "Icon pack")
            },
            text = {
                LazyColumn {
                    item {
                        IconPackChoice(
                            label = "System default",
                            icon = null,
                            selected = selectedPackage == null,
                            onClick = { onSelect(null) },
                        )
                    }
                    items(packs.orEmpty(), key = { it.packageName }) { pack ->
                        IconPackChoice(
                            label = pack.label,
                            icon = pack.icon,
                            selected = selectedPackage == pack.packageName,
                            onClick = { onSelect(pack.packageName) },
                        )
                    }
                    if (packs?.isEmpty() == true) {
                        item {
                            Text(
                                text = "No icon packs installed.",
                                modifier = Modifier.padding(Spacing.medium),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun IconPackChoice(
    label: String,
    icon: Painter?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    MenuOption(
        leadingContent = {
            if (icon != null) {
                // Image, not Icon: pack app icons must render untinted.
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.iconMedium),
                )
            } else {
                Spacer(modifier = Modifier.size(dimensions.iconMedium))
            }
        },
        text = label,
        onClick = onClick,
        endContent = {
            if (selected) {
                Icon(
                    painter = CheckIcon(),
                    contentDescription = "Selected",
                    modifier = Modifier.size(dimensions.iconSmall),
                )
            }
        },
    )
}
