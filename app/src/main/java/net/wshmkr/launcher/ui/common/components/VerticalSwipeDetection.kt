package net.wshmkr.launcher.ui.common.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

val VERTICAL_SWIPE_THRESHOLD = 180.dp

fun verticalDragFeedback(dy: Float) = sqrt(abs(dy)) * sign(dy) * 5

@Composable
fun Modifier.verticalSwipeDetection(
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
): Modifier {
    val thresholdPx = with(LocalDensity.current) { VERTICAL_SWIPE_THRESHOLD.toPx() }

    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentOnSwipeDown by rememberUpdatedState(onSwipeDown)

    return this
        .graphicsLayer {
            translationY = verticalDragFeedback(offsetY.value)
        }
        .pointerInput(Unit) {
            var totalDragY = 0f
            detectVerticalDragGestures(
                onDragStart = {
                    totalDragY = 0f
                },
                onDragEnd = {
                    if (totalDragY > thresholdPx) {
                        currentOnSwipeDown?.invoke()
                    } else if (totalDragY < -thresholdPx) {
                        currentOnSwipeUp?.invoke()
                    }
                    totalDragY = 0f
                    coroutineScope.launch {
                        offsetY.animateTo(targetValue = 0f, animationSpec = spring())
                    }
                },
                onVerticalDrag = { _, dragAmount ->
                    totalDragY += dragAmount
                    coroutineScope.launch {
                        offsetY.snapTo(totalDragY)
                    }
                }
            )
        }
}
