package net.wshmkr.launcher.ui.common.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Detects a still long press without blocking taps or swipes below, then consumes the rest
 * of the gesture so children never see a click (interop views get ACTION_CANCEL).
 * When [interactionSource] is set, emits press interactions so an indication (e.g. ripple)
 * can show the touched area. [enabled] is evaluated at pointer-down.
 */
fun Modifier.captureLongPress(
    enabled: () -> Boolean = { true },
    interactionSource: MutableInteractionSource? = null,
    onLongPress: () -> Unit,
): Modifier =
    pointerInput(interactionSource) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            if (!enabled()) return@awaitEachGesture

            val press = interactionSource?.let {
                PressInteraction.Press(down.position).also { p -> it.tryEmit(p) }
            }
            fun endPress(cancelled: Boolean) {
                val started = press ?: return
                interactionSource?.tryEmit(
                    if (cancelled) PressInteraction.Cancel(started) else PressInteraction.Release(started),
                )
            }

            val slop = viewConfiguration.touchSlop
            val slopSquared = slop * slop
            val startPos = down.position
            // null once the long-press timeout elapses; true/false if the gesture ends first.
            val movedBeforeTimeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                        ?: return@withTimeoutOrNull false
                    if (!change.pressed) return@withTimeoutOrNull false
                    val dx = change.position.x - startPos.x
                    val dy = change.position.y - startPos.y
                    if (dx * dx + dy * dy > slopSquared) return@withTimeoutOrNull true
                }
                @Suppress("UNREACHABLE_CODE")
                false
            }

            if (movedBeforeTimeout != null) {
                endPress(cancelled = movedBeforeTimeout)
                return@awaitEachGesture
            }

            onLongPress()
            // Hold the press until the finger lifts, so a concurrent ancestor long-press can
            // observe it and defer; also consumes so the gesture never completes below.
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
                if (event.changes.none { it.pressed }) break
            }
            endPress(cancelled = false)
        }
    }
