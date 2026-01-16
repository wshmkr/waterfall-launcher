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
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun MenuOption(
    icon: Painter? = null,
    text: String,
    subtext: String? = null,
    onClick: () -> Unit,
    color: Color = Color.Black,
    indent: Int = 0,
    endContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(
                start = 16.dp * (indent + 1),
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                painter = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = color,
            )
            Spacer(modifier = Modifier.width(24.dp))
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                color = color,
            )
            subtext?.let {
                Text(
                    text = subtext,
                    fontSize = 16.sp,
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
fun MenuOptionSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    offText: String? = null,
    onText: String? = null,
) {
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
                        color = SwitchDefaults.colors().checkedIconColor
                    )
                }
            }
        },
        colors = if (offText != null && onText != null) {
            SwitchDefaults.colors(
                uncheckedThumbColor = SwitchDefaults.colors().checkedThumbColor,
                uncheckedTrackColor = SwitchDefaults.colors().checkedTrackColor,
                uncheckedBorderColor = SwitchDefaults.colors().checkedBorderColor,
            )
        } else {
            SwitchDefaults.colors()
        }
    )
}
