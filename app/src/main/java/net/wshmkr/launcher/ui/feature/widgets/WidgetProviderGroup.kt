package net.wshmkr.launcher.ui.feature.widgets

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import net.wshmkr.launcher.ui.common.components.animateLetterFilterAlpha
import net.wshmkr.launcher.ui.common.icons.ArrowDropDownIcon
import net.wshmkr.launcher.ui.common.icons.ArrowDropUpIcon
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
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
    val dimensions = LocalDimensions.current
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
                    .padding(
                        start = Spacing.large,
                        end = dimensions.gutterLarge,
                        top = Spacing.small,
                    )
            ) {
                provider.widgets.forEachIndexed { index, widgetOption ->
                    key(widgetOption.info.provider) {
                        val onClick = remember(widgetOption, onWidgetSelected) {
                            { onWidgetSelected(widgetOption) }
                        }
                        WidgetListItem(
                            widgetOption = widgetOption,
                            modifier = Modifier.fillMaxWidth(),
                            targetAlpha = targetAlpha,
                            isActiveLetter = isActiveLetter,
                            onClick = onClick
                        )
                        if (index != provider.widgets.lastIndex) {
                            Spacer(modifier = Modifier.height(Spacing.small))
                        }
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
    targetAlpha: Float,
    isActiveLetter: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val animatedAlpha by animateLetterFilterAlpha(
        targetAlpha = targetAlpha,
        isActiveLetter = isActiveLetter,
        label = "widget_provider_alpha"
    )
    val dimensions = LocalDimensions.current

    Row(
        modifier = modifier
            .padding(start = Spacing.small, end = dimensions.gutterLarge)
            .fillMaxWidth()
            .clip(Corners.medium)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.medium, vertical = 12.dp)
            .alpha(animatedAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = provider.icon),
            contentDescription = provider.label,
            modifier = Modifier
                .size(dimensions.iconLarge)
                .clip(Corners.small)
        )
        Spacer(modifier = Modifier.width(Spacing.medium))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = provider.label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = dimensions.fontLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = widgetCountLabel(provider.widgetCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = dimensions.fontCaption,
            )
        }
        Icon(
            painter = if (isExpanded) ArrowDropUpIcon() else ArrowDropDownIcon(),
            contentDescription = if (isExpanded) "Collapse Widgets" else "Expand Widgets",
            modifier = Modifier.size(dimensions.iconSmall),
            tint = MaterialTheme.colorScheme.onSurface
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
