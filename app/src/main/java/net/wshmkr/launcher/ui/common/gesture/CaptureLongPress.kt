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
 * Emits press interactions on [interactionSource] so an indication (e.g. ripple) can show the
 * touched area. [onTouchedChange] brackets the whole gesture, including after slop movement
 * ends the press, so an ancestor gesture can defer to it. [enabled] is evaluated at pointer-down.
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

            var press: PressInteraction.Press? = PressInteraction.Press(down.position)
                .also { interactionSource.tryEmit(it) }
            fun endPress(cancelled: Boolean) {
                val started = press ?: return
                press = null
                interactionSource.tryEmit(
                    if (cancelled) PressInteraction.Cancel(started) else PressInteraction.Release(started),
                )
            }

            onTouchedChange(true)
            // Cancellation (ACTION_CANCEL, node detach) unwinds here, so the shared
            // interaction source can never be left latched in the pressed state.
            try {
                val slop = viewConfiguration.touchSlop
                // null means the timeout elapsed with the finger still down: a long press.
                val movedOutOfSlop = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    awaitSlopOrRelease(down, slop * slop)
                }

                if (movedOutOfSlop != null) {
                    endPress(cancelled = movedOutOfSlop)
                    if (movedOutOfSlop) awaitAllPointersUp()
                    return@awaitEachGesture
                }

                onLongPress()
                // Consume the rest of the gesture so it never completes below.
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { it.consume() }
                    if (event.changes.none { it.pressed }) break
                }
                endPress(cancelled = false)
            } finally {
                endPress(cancelled = true)
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

/** Drains the gesture without consuming, so [onTouchedChange] stays true until the finger lifts. */
private suspend fun AwaitPointerEventScope.awaitAllPointersUp() {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        if (event.changes.none { it.pressed }) return
    }
}
