package net.wshmkr.launcher.ui.common.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Detects a still long press without blocking taps or swipes below, then consumes the rest
 * of the gesture so children never see a click (interop views get ACTION_CANCEL).
 * Emits a press on [interactionSource] once the long press is recognized.
 * [onTouchedChange] brackets the whole gesture, staying true until the last pointer
 * lifts even once the press itself has ended, so an ancestor gesture can defer to it.
 * [enabled] is evaluated at pointer-down. Callbacks update without restarting an active gesture.
 */
fun Modifier.captureLongPress(
    interactionSource: MutableInteractionSource,
    enabled: () -> Boolean = { true },
    onTouchedChange: (Boolean) -> Unit = {},
    onLongPress: () -> Unit,
): Modifier = this then CaptureLongPressElement(interactionSource, enabled, onTouchedChange, onLongPress)

private data class CaptureLongPressElement(
    val interactionSource: MutableInteractionSource,
    val enabled: () -> Boolean,
    val onTouchedChange: (Boolean) -> Unit,
    val onLongPress: () -> Unit,
) : ModifierNodeElement<CaptureLongPressNode>() {
    override fun create() = CaptureLongPressNode(interactionSource, enabled, onTouchedChange, onLongPress)

    override fun update(node: CaptureLongPressNode) {
        node.update(interactionSource, enabled, onTouchedChange, onLongPress)
    }
}

private class CaptureLongPressNode(
    private var interactionSource: MutableInteractionSource,
    private var enabled: () -> Boolean,
    private var onTouchedChange: (Boolean) -> Unit,
    private var onLongPress: () -> Unit,
) : DelegatingNode() {
    private val pointerInputNode = delegate(
        SuspendingPointerInputModifierNode { detectLongPressCapture() },
    )

    fun update(
        interactionSource: MutableInteractionSource,
        enabled: () -> Boolean,
        onTouchedChange: (Boolean) -> Unit,
        onLongPress: () -> Unit,
    ) {
        this.enabled = enabled
        this.onTouchedChange = onTouchedChange
        this.onLongPress = onLongPress
        if (this.interactionSource != interactionSource) {
            this.interactionSource = interactionSource
            pointerInputNode.resetPointerInputHandler()
        }
    }

    private suspend fun PointerInputScope.detectLongPressCapture() {
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

                val pressSource = interactionSource
                val press = PressInteraction.Press(down.position)
                pressSource.tryEmit(press)
                var released = false
                // finally also runs on ACTION_CANCEL / node detach, so the press is never left latched.
                try {
                    onLongPress()
                    consumeUntilAllPointersUp()
                    released = true
                } finally {
                    pressSource.tryEmit(
                        if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                    )
                }
            } finally {
                onTouchedChange(false)
            }
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
