package net.wshmkr.launcher.ui.feature.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import net.wshmkr.launcher.viewmodel.WidgetOption

@Composable
fun WidgetListItem(
    widgetOption: WidgetOption,
    modifier: Modifier = Modifier,
    targetAlpha: Float = 1f,
    isActiveLetter: Boolean,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    val previewDrawable = remember(widgetOption.info, densityDpi) {
        try {
            widgetOption.info.loadPreviewImage(context, densityDpi)
                ?: widgetOption.info.loadIcon(context, densityDpi)
        } catch (e: Exception) {
            null
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = if (isActiveLetter || targetAlpha < 1f) {
            snap()
        } else {
            tween(durationMillis = 300)
        },
        label = "widget_list_item_alpha"
    )

    Column(
        modifier = modifier
            .alpha(animatedAlpha)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            val density = LocalDensity.current
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val fallbackHeightPx = with(density) { 120.dp.toPx() }

            val intrinsicWidthPx = previewDrawable?.intrinsicWidth?.takeIf { it > 0 }?.toFloat()
            val intrinsicHeightPx = previewDrawable?.intrinsicHeight?.takeIf { it > 0 }?.toFloat()

            val targetWidthPx = intrinsicWidthPx?.coerceAtMost(maxWidthPx) ?: maxWidthPx
            val targetHeightPx = if (intrinsicWidthPx != null && intrinsicHeightPx != null) {
                intrinsicHeightPx * (targetWidthPx / intrinsicWidthPx)
            } else {
                fallbackHeightPx
            }

            val previewModifier = Modifier
                .width(with(density) { targetWidthPx.toDp() })
                .height(with(density) { targetHeightPx.toDp() })
                .clip(RoundedCornerShape(8.dp))

            if (previewDrawable != null) {
                Image(
                    painter = rememberDrawablePainter(drawable = previewDrawable),
                    contentDescription = widgetOption.label,
                    modifier = previewModifier,
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(
                    modifier = previewModifier.background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Preview unavailable",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = widgetOption.label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}