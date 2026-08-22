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

private const val PAUSE_TIMEOUT_MS = 150L

private const val SCROLLED_SWIPE_HEIGHT_FRACTION = 0.6f

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
            var scrolled = false
            var bandY = 0f
            var lastMoveUptime = 0L
            val velocityTracker = VelocityTracker()
            var flingJob: Job? = null
            fun settle() {
                coroutineScope.launch {
                    offsetY.animateTo(targetValue = 0f, animationSpec = spring())
                }
            }
            detectVerticalDragGestures(
                onDragStart = {
                    // dispatchRawDelta bypasses the scroll mutex, so a fling won't stop on its own.
                    flingJob?.cancel()
                    committed = 0f
                    scrolled = false
                    bandY = offsetY.value
                    velocityTracker.resetTracking()
                    lastMoveUptime = SystemClock.uptimeMillis()
                    coroutineScope.launch { offsetY.stop() }
                },
                onDragEnd = {
                    val pausedBeforeRelease =
                        SystemClock.uptimeMillis() - lastMoveUptime > PAUSE_TIMEOUT_MS
                    val swipeThreshold = if (scrolled) {
                        maxOf(thresholdPx, size.height * SCROLLED_SWIPE_HEIGHT_FRACTION)
                    } else {
                        thresholdPx
                    }
                    if (!pausedBeforeRelease) {
                        when {
                            committed > swipeThreshold -> currentOnSwipeDown?.invoke()
                            committed < -swipeThreshold -> currentOnSwipeUp?.invoke()
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
                    settle()
                },
                onDragCancel = { settle() },
                onVerticalDrag = { change, dragAmount ->
                    if (change.uptimeMillis - lastMoveUptime > PAUSE_TIMEOUT_MS) {
                        committed = 0f
                        scrolled = false
                        velocityTracker.resetTracking()
                    }
                    lastMoveUptime = change.uptimeMillis
                    velocityTracker.addPointerInputChange(change)
                    committed += dragAmount

                    // Unwind the band first, else reversing at an edge scrolls under a frozen band.
                    var remainder = dragAmount
                    if (bandY != 0f && sign(remainder) != sign(bandY)) {
                        val unwind = sign(remainder) * min(abs(remainder), abs(bandY))
                        bandY += unwind
                        remainder -= unwind
                    }
                    if (remainder != 0f) {
                        val consumedScroll = listState.dispatchRawDelta(-remainder)
                        if (consumedScroll != 0f) scrolled = true
                        bandY += remainder + consumedScroll
                    }
                    if (offsetY.value != bandY) {
                        coroutineScope.launch { offsetY.snapTo(bandY) }
                    }
                }
            )
        }
}
