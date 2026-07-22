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
import kotlinx.coroutines.launch
import net.wshmkr.launcher.ui.theme.LocalDimensions
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

@Composable
fun Modifier.verticalSwipeDetection(
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
): Modifier {
    val dimensions = LocalDimensions.current
    val thresholdPx = with(LocalDensity.current) { dimensions.verticalSwipeThreshold.toPx() }
    val feedbackScale = dimensions.verticalDragFeedbackScale

    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentOnSwipeDown by rememberUpdatedState(onSwipeDown)

    return this
        .graphicsLayer {
            translationY = sqrt(abs(offsetY.value)) * sign(offsetY.value) * feedbackScale
        }
        .pointerInput(thresholdPx) {
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
