package net.wshmkr.launcher.ui.common.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

const val VERTICAL_SWIPE_SENSITIVITY = 500

fun verticalDragFeedback(dy: Float) = sqrt(abs(dy)) * sign(dy) * 5

@Composable
fun Modifier.verticalSwipeDetection(
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
): Modifier {
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    return this
        .graphicsLayer {
            translationY = verticalDragFeedback(offsetY)
        }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = {
                    totalDragY = 0f
                },
                onDragEnd = {
                    if (totalDragY > VERTICAL_SWIPE_SENSITIVITY && onSwipeDown != null) {
                        onSwipeDown()
                    } else if (totalDragY < -VERTICAL_SWIPE_SENSITIVITY && onSwipeUp != null) {
                        onSwipeUp()
                    }
                    totalDragY = 0f
                    coroutineScope.launch {
                        val animatable = Animatable(offsetY)
                        animatable.animateTo(
                            targetValue = 0f,
                            animationSpec = spring()
                        ) {
                            offsetY = value
                        }
                    }
                },
                onVerticalDrag = { _, dragAmount ->
                    totalDragY += dragAmount
                    offsetY = totalDragY
                }
            )
        }
}
