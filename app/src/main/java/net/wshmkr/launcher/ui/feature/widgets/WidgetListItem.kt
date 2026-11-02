package net.wshmkr.launcher.ui.feature.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.viewmodel.WidgetOption

@Composable
fun WidgetListItem(
    widgetOption: WidgetOption,
    modifier: Modifier = Modifier,
    alphaProvider: () -> Float = { 1f },
    onClick: () -> Unit = {}
) {
    val drawable = rememberWidgetPreviewDrawable(widgetOption.info).value

    val dimensions = LocalDimensions.current

    Column(
        modifier = modifier
            .graphicsLayer { this.alpha = alphaProvider() }
            .clip(Corners.small)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.medium, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        val intrinsicWidthPx = drawable?.intrinsicWidth?.takeIf { it > 0 }
        val intrinsicHeightPx = drawable?.intrinsicHeight?.takeIf { it > 0 }
        val previewSizeModifier = if (intrinsicWidthPx != null && intrinsicHeightPx != null) {
            val intrinsicWidth = with(LocalDensity.current) { intrinsicWidthPx.toDp() }
            Modifier
                .widthIn(max = intrinsicWidth)
                .fillMaxWidth()
                .aspectRatio(intrinsicWidthPx.toFloat() / intrinsicHeightPx)
        } else {
            Modifier
                .fillMaxWidth()
                .height(120.dp)
        }
        val previewModifier = previewSizeModifier.clip(Corners.small)

        if (drawable != null) {
            Image(
                painter = rememberDrawablePainter(drawable = drawable),
                contentDescription = widgetOption.label,
                modifier = previewModifier,
                contentScale = ContentScale.FillBounds
            )
        } else {
            Box(
                modifier = previewModifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Preview unavailable",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = dimensions.fontCaption
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = widgetOption.label,
            fontSize = dimensions.fontMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
