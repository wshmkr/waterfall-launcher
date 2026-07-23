@file:Suppress("MatchingDeclarationName")

package net.wshmkr.launcher.ui.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing

enum class MenuOptionTextSize {
    Small, Medium, Large
}

@Composable
fun MenuOption(
    icon: Painter? = null,
    text: String,
    subtext: String? = null,
    onClick: () -> Unit,
    color: Color = Color.Black,
    indent: Int = 0,
    endContent: (@Composable () -> Unit)? = null,
    textSize: MenuOptionTextSize = MenuOptionTextSize.Medium,
) {
    val dimensions = LocalDimensions.current
    val fontSize = when (textSize) {
        MenuOptionTextSize.Small -> dimensions.fontMedium
        MenuOptionTextSize.Medium -> dimensions.fontLarge
        MenuOptionTextSize.Large -> dimensions.fontXLarge
    }

    val subtextSize = when (textSize) {
        MenuOptionTextSize.Small -> dimensions.fontSmall
        MenuOptionTextSize.Medium -> dimensions.fontMedium
        MenuOptionTextSize.Large -> dimensions.fontLarge
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Corners.small)
            .clickable(onClick = onClick)
            .padding(
                start = Spacing.medium * (indent + 1),
                end = Spacing.medium,
                top = Spacing.small,
                bottom = Spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                painter = icon,
                contentDescription = text,
                modifier = Modifier.size(dimensions.iconSmall),
                tint = color,
            )
            Spacer(modifier = Modifier.width(dimensions.iconGap))
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = text,
                fontSize = fontSize,
                color = color,
            )
            subtext?.let {
                Text(
                    text = subtext,
                    fontSize = subtextSize,
                    color = Color.Gray,
                )
            }
        }
        endContent?.let {
            it()
        }
    }
}

@Composable
fun ToggleMenuOption(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: Painter? = null,
    subtext: String? = null,
    color: Color = Color.Black,
    indent: Int = 0,
    textSize: MenuOptionTextSize = MenuOptionTextSize.Medium,
    offText: String? = null,
    onText: String? = null,
) {
    MenuOption(
        icon = icon,
        text = text,
        subtext = subtext,
        onClick = { onCheckedChange(!checked) },
        color = color,
        indent = indent,
        textSize = textSize,
        endContent = {
            MenuOptionSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                offText = offText,
                onText = onText,
            )
        }
    )
}

@Composable
fun MenuOptionSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    offText: String? = null,
    onText: String? = null,
) {
    val defaultColors = SwitchDefaults.colors()
    Switch(
        modifier = modifier
            .scale(0.8f)
            .heightIn(max = 24.dp),
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            Box(
                modifier = Modifier.size(SwitchDefaults.IconSize),
                contentAlignment = Alignment.Center
            ) {
                if (offText != null && onText != null) {
                    Text(
                        text = if (checked) onText else offText,
                        fontSize = 13.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = defaultColors.checkedIconColor
                    )
                }
            }
        },
        colors = if (offText != null && onText != null) {
            SwitchDefaults.colors(
                uncheckedThumbColor = defaultColors.checkedThumbColor,
                uncheckedTrackColor = defaultColors.checkedTrackColor,
                uncheckedBorderColor = defaultColors.checkedBorderColor,
            )
        } else {
            defaultColors
        }
    )
}
