package net.wshmkr.launcher.ui.common.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Detects a still long press without blocking taps or swipes below, then consumes the rest
 * of the gesture so children never see a click (interop views get ACTION_CANCEL).
 * [enabled] is evaluated at pointer-down.
 */
fun Modifier.captureLongPress(
    enabled: () -> Boolean = { true },
    onLongPress: () -> Unit,
): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            if (!enabled()) return@awaitEachGesture
            val slop = viewConfiguration.touchSlop
            val slopSquared = slop * slop
            val startPos = down.position
            val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                        ?: return@withTimeoutOrNull Unit
                    if (!change.pressed) return@withTimeoutOrNull Unit
                    val dx = change.position.x - startPos.x
                    val dy = change.position.y - startPos.y
                    if (dx * dx + dy * dy > slopSquared) return@withTimeoutOrNull Unit
                }
                @Suppress("UNREACHABLE_CODE")
                Unit
            }
            if (outcome != null) return@awaitEachGesture
            onLongPress()
            // Consume until every pointer lifts so the gesture never completes below.
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
                if (event.changes.none { it.pressed }) break
            }
        }
    }
