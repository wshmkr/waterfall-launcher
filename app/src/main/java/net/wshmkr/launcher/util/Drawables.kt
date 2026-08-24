package net.wshmkr.launcher.util

import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import net.wshmkr.launcher.ui.theme.maxAppIconSize
import kotlin.math.roundToInt

internal fun maxAppIconSizePx(resources: Resources): Int =
    (maxAppIconSize.value * resources.displayMetrics.density).roundToInt()

internal fun Drawable.toBitmapPainter(maxSizePx: Int): BitmapPainter {
    val intrinsicWidth = intrinsicWidth.takeIf { it > 0 } ?: maxSizePx
    val intrinsicHeight = intrinsicHeight.takeIf { it > 0 } ?: maxSizePx
    val scale = minOf(1f, maxSizePx.toFloat() / maxOf(intrinsicWidth, intrinsicHeight))

    val bitmap = toBitmap(
        width = (intrinsicWidth * scale).roundToInt().coerceAtLeast(1),
        height = (intrinsicHeight * scale).roundToInt().coerceAtLeast(1),
    )
    return BitmapPainter(bitmap.asImageBitmap())
}
