package net.wshmkr.launcher.ui.feature.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import net.wshmkr.launcher.viewmodel.WidgetAppListItem
import net.wshmkr.launcher.viewmodel.WidgetOption


@Composable
fun WidgetProviderGroup(
    provider: WidgetAppListItem.Provider,
    isExpanded: Boolean,
    targetAlpha: Float,
    isActiveLetter: Boolean,
    onProviderClick: () -> Unit,
    onWidgetSelected: (WidgetOption) -> Unit,
) {
    Column {
        WidgetProviderRow(
            provider = provider,
            isExpanded = isExpanded,
            targetAlpha = targetAlpha,
            isActiveLetter = isActiveLetter,
            onClick = onProviderClick
        )
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 32.dp, top = 8.dp)
            ) {
                provider.widgets.forEachIndexed { index, widgetOption ->
                    WidgetListItem(
                        widgetOption = widgetOption,
                        modifier = Modifier.fillMaxWidth(),
                        targetAlpha = targetAlpha,
                        isActiveLetter = isActiveLetter,
                        onClick = { onWidgetSelected(widgetOption) }
                    )
                    if (index != provider.widgets.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetProviderRow(
    provider: WidgetAppListItem.Provider,
    isExpanded: Boolean,
    targetAlpha: Float = 1f,
    isActiveLetter: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = if (isActiveLetter || targetAlpha < 1f) {
            snap()
        } else {
            tween(durationMillis = 250)
        },
        label = "widget_row_alpha"
    )

    Row(
        modifier = modifier
            .padding(start = 8.dp, end = 32.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(animatedAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = provider.icon),
            contentDescription = provider.label,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = provider.label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = widgetCountLabel(provider.widgetCount),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown,
            contentDescription = if (isExpanded) "Collapse Widgets" else "Expand Widgets",
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun widgetCountLabel(count: Int): String {
    return if (count == 1) {
        "1 widget available"
    } else {
        "$count widgets available"
    }
}