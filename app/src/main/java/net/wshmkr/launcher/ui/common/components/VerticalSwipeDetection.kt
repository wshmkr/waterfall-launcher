package net.wshmkr.launcher.ui.common.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

val VERTICAL_SWIPE_THRESHOLD = 180.dp

// Standing still this long mid-drag resets the swipe ("hold to scroll") and stales the fling velocity.
private const val PAUSE_TIMEOUT_MS = 150L

fun verticalDragFeedback(dy: Float) = sqrt(abs(dy)) * sign(dy) * 5

@Composable
fun Modifier.verticalSwipeDetection(
    listState: LazyListState,
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
): Modifier {
    val thresholdPx = with(LocalDensity.current) { VERTICAL_SWIPE_THRESHOLD.toPx() }

    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentOnSwipeDown by rememberUpdatedState(onSwipeDown)
    val flingBehavior = ScrollableDefaults.flingBehavior()

    return this
        .graphicsLayer {
            translationY = verticalDragFeedback(offsetY.value)
        }
        .pointerInput(listState) {
            var committed = 0f
            var bandY = 0f
            var lastMoveUptime = 0L
            val velocityTracker = VelocityTracker()
            var flingJob: Job? = null
            detectVerticalDragGestures(
                onDragStart = {
                    // dispatchRawDelta bypasses the scroll mutex; a live fling would keep
                    // running under the finger.
                    flingJob?.cancel()
                    committed = 0f
                    bandY = offsetY.value
                    velocityTracker.resetTracking()
                    lastMoveUptime = SystemClock.uptimeMillis()
                    coroutineScope.launch { offsetY.stop() }
                },
                onDragEnd = {
                    val pausedBeforeRelease =
                        SystemClock.uptimeMillis() - lastMoveUptime > PAUSE_TIMEOUT_MS
                    if (!pausedBeforeRelease) {
                        when {
                            committed > thresholdPx -> currentOnSwipeDown?.invoke()
                            committed < -thresholdPx -> currentOnSwipeUp?.invoke()
                            else -> {
                                val flingVelocity = -velocityTracker.calculateVelocity().y
                                flingJob = coroutineScope.launch {
                                    listState.scroll {
                                        with(flingBehavior) { performFling(flingVelocity) }
                                    }
                                }
                            }
                        }
                    }
                    committed = 0f
                    bandY = 0f
                    coroutineScope.launch {
                        offsetY.animateTo(targetValue = 0f, animationSpec = spring())
                    }
                },
                onDragCancel = {
                    committed = 0f
                    bandY = 0f
                    coroutineScope.launch {
                        offsetY.animateTo(targetValue = 0f, animationSpec = spring())
                    }
                },
                onVerticalDrag = { change, dragAmount ->
                    if (change.uptimeMillis - lastMoveUptime > PAUSE_TIMEOUT_MS) {
                        committed = 0f
                        velocityTracker.resetTracking()
                    }
                    lastMoveUptime = change.uptimeMillis
                    velocityTracker.addPointerInputChange(change)
                    committed += dragAmount

                    // Unwind a stretched band before scrolling, else reversing at an edge
                    // scrolls the list under a frozen band.
                    var remainder = dragAmount
                    if (bandY != 0f && sign(remainder) != sign(bandY)) {
                        val unwind = sign(remainder) * min(abs(remainder), abs(bandY))
                        bandY += unwind
                        remainder -= unwind
                    }
                    if (remainder != 0f) {
                        // Finger down (positive) scrolls the list backward, hence the negation.
                        val consumedScroll = listState.dispatchRawDelta(-remainder)
                        bandY += remainder + consumedScroll
                    }
                    coroutineScope.launch { offsetY.snapTo(bandY) }
                }
            )
        }
}
