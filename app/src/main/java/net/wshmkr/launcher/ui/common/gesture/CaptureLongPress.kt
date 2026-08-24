package net.wshmkr.launcher.ui.common.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Detects a still long press without blocking taps or swipes below, then consumes the rest
 * of the gesture so children never see a click (interop views get ACTION_CANCEL).
 * Emits a press on [interactionSource] once the long press is recognized.
 * [onTouchedChange] brackets the whole gesture, staying true until the last pointer
 * lifts even once the press itself has ended, so an ancestor gesture can defer to it.
 * [enabled] is evaluated at pointer-down.
 */
fun Modifier.captureLongPress(
    interactionSource: MutableInteractionSource,
    enabled: () -> Boolean = { true },
    onTouchedChange: (Boolean) -> Unit = {},
    onLongPress: () -> Unit,
): Modifier =
    pointerInput(interactionSource) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            if (!enabled()) return@awaitEachGesture

            onTouchedChange(true)
            try {
                val slop = viewConfiguration.touchSlop
                // null means the timeout elapsed with the finger still down: a long press.
                val movedOutOfSlop = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    awaitSlopOrRelease(down, slop * slop)
                }

                if (movedOutOfSlop != null) {
                    awaitAllPointersUp()
                    return@awaitEachGesture
                }

                val press = PressInteraction.Press(down.position)
                interactionSource.tryEmit(press)
                onLongPress()
                var released = false
                // finally also runs on ACTION_CANCEL / node detach, so the press is never left latched.
                try {
                    consumeUntilAllPointersUp()
                    released = true
                } finally {
                    interactionSource.tryEmit(
                        if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                    )
                }
            } finally {
                onTouchedChange(false)
            }
        }
    }

/** True once the pointer leaves the slop radius, false if it lifts or is lost first. */
private suspend fun AwaitPointerEventScope.awaitSlopOrRelease(
    down: PointerInputChange,
    slopSquared: Float,
): Boolean {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == down.id } ?: return false
        if (!change.pressed) return false
        val dx = change.position.x - down.position.x
        val dy = change.position.y - down.position.y
        if (dx * dx + dy * dy > slopSquared) return true
    }
}

private suspend fun AwaitPointerEventScope.consumeUntilAllPointersUp() {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        event.changes.forEach { it.consume() }
        if (event.changes.none { it.pressed }) break
    }
}

/** Drains the gesture without consuming, so [onTouchedChange] stays true until the last finger lifts. */
private suspend fun AwaitPointerEventScope.awaitAllPointersUp() {
    while (currentEvent.changes.any { it.pressed }) {
        awaitPointerEvent(PointerEventPass.Initial)
    }
}
