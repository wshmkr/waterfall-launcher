package net.wshmkr.launcher.ui.feature.notifications

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.ui.common.icons.ArrowLeftAltIcon
import net.wshmkr.launcher.ui.common.icons.ArrowRightAltIcon
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.roundToInt

// Far enough that a scroll or a brush past the row can't reach it, short enough that the gesture
// is over almost as soon as it has started.
private val COMMIT_DISTANCE = 64.dp

// The action has already run by the time the row lets go, so this is only tidying up after it.
private const val RETURN_MS = 220

private val ROW_SHAPE = Corners.small

private val ARROW_GAP = 4.dp

private const val EXPAND_LABEL = "Expand"
private const val DISMISS_LABEL = "Dismiss"
private const val UNCLEARABLE_LABEL = "Can't dismiss"

@Composable
fun NotificationSwipeBox(
    swipeTarget: NotificationInfo?,
    interactionSource: MutableInteractionSource,
    onExpand: () -> Unit,
    onDismissNotification: (NotificationInfo) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val commitPx = with(LocalDensity.current) { COMMIT_DISTANCE.toPx() }

    var travel by remember { mutableFloatStateOf(0f) }
    var rowWidth by remember { mutableIntStateOf(0) }
    val movedRight by remember { derivedStateOf { travel > 0f } }

    // Taken the moment the gesture commits rather than when it is released, so a repost arriving
    // under the finger is never what gets cancelled.
    val latestTarget by rememberUpdatedState(swipeTarget)
    var armedTarget by remember { mutableStateOf<NotificationInfo?>(null) }

    val dragState = rememberDraggableState { delta ->
        val edge = rowWidth.toFloat()
        val next = (travel + delta).coerceIn(-edge, edge)
        if (abs(next) >= commitPx && abs(travel) < commitPx) armedTarget = latestTarget
        travel = next
    }

    // draggable runs onDragStopped on its own scope and never cancels it, so the settle is held
    // here instead — otherwise it would keep writing travel underneath the next drag.
    val settleScope = rememberCoroutineScope()
    var settle by remember { mutableStateOf<Job?>(null) }

    // draggable's own interactionSource reports a DragInteraction, which renders as the dragged
    // state layer rather than a press, so the row is held pressed explicitly instead.
    var press by remember { mutableStateOf<PressInteraction.Press?>(null) }

    Box(
        modifier = modifier
            // Owned here rather than by the caller, so the underlay cuts the row out at the same
            // radius the row is clipped to.
            .clip(ROW_SHAPE)
            .onSizeChanged { rowWidth = it.width }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                // Stays on through the settle so it isn't cut short by the last dismissal.
                enabled = swipeTarget != null || armedTarget != null,
                onDragStarted = { startedPosition ->
                    settle?.cancel()
                    press = PressInteraction.Press(startedPosition)
                        .also { interactionSource.tryEmit(it) }
                },
                // Also reached when the gesture is cancelled, so the press can't stay latched.
                onDragStopped = {
                    press?.let { interactionSource.tryEmit(PressInteraction.Release(it)) }
                    press = null

                    val towardsRight = travel > 0f
                    val committed = abs(travel) >= commitPx
                    if (committed) {
                        if (expandsRow(towardsRight, layoutDirection)) {
                            if (latestTarget != null) onExpand()
                        } else {
                            armedTarget?.takeIf { it.isClearable }?.let(onDismissNotification)
                        }
                    }
                    settle = settleScope.launch {
                        animate(travel, 0f, animationSpec = tween(RETURN_MS, easing = FastOutSlowInEasing)) { value, _ ->
                            travel = value
                        }
                        armedTarget = null
                    }
                },
            ),
        propagateMinConstraints = true,
    ) {
        val backgroundTarget = swipeTarget ?: armedTarget
        if (backgroundTarget != null) {
            SwipeActionBackground(
                travel = { travel },
                movedRight = movedRight,
                canDismiss = backgroundTarget.isClearable,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .horizontalTravel { travel }
                // Carried by the row rather than by its clickable, which is cancelled the moment
                // the drag takes the pointer — this keeps it lit for the whole gesture.
                .indication(interactionSource, ripple()),
        ) {
            content()
        }
    }
}

// Rows are transparent over the wallpaper, so only the strip the row has vacated may be painted —
// the geometry is read in the draw phase, which keeps a drag off the recomposer.
@Composable
private fun SwipeActionBackground(
    travel: () -> Float,
    movedRight: Boolean,
    canDismiss: Boolean,
    modifier: Modifier,
) {
    val expanding = expandsRow(movedRight, LocalLayoutDirection.current)

    val fill = when {
        expanding -> MaterialTheme.colorScheme.primary
        canDismiss -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        expanding -> MaterialTheme.colorScheme.onPrimary
        canDismiss -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val rowOutline = remember { Path() }

    Box(
        modifier = modifier
            // Purely a drag affordance; leaving it in the tree makes every row read out twice.
            .clearAndSetSemantics {}
            .drawWithContent {
                val offset = travel()
                if (offset == 0f) return@drawWithContent
                rowOutline.rewind()
                rowOutline.addRoundRect(
                    RoundRect(
                        left = offset,
                        top = 0f,
                        right = size.width + offset,
                        bottom = size.height,
                        cornerRadius = CornerRadius(ROW_SHAPE.topStart.toPx(size, this)),
                    )
                )
                // Everything the row still covers is left alone, so its corners read as the edge
                // of something lying on top rather than as a straight cut.
                clipPath(rowOutline, ClipOp.Difference) {
                    drawRect(fill)
                    this@drawWithContent.drawContent()
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ARROW_GAP),
            // Pinned to the edge the row uncovers, which is a physical side rather than a relative
            // one — the strip is wherever the finger went.
            modifier = Modifier
                .align(if (movedRight) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                .padding(horizontal = Spacing.medium),
        ) {
            // The arrow points the way the row is travelling.
            if (!movedRight) SwipeActionArrow(ArrowLeftAltIcon(), contentColor)
            Text(
                text = when {
                    expanding -> EXPAND_LABEL
                    canDismiss -> DISMISS_LABEL
                    else -> UNCLEARABLE_LABEL
                },
                color = contentColor,
                fontSize = LocalDimensions.current.fontMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            if (movedRight) SwipeActionArrow(ArrowRightAltIcon(), contentColor)
        }
    }
}

// Sized off the label so it tracks both the dimension profile and the system font scale.
@Composable
private fun SwipeActionArrow(painter: Painter, color: Color) {
    val size = with(LocalDensity.current) { LocalDimensions.current.fontMedium.toDp() }
    Icon(
        painter = painter,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(size),
    )
}

// Start-to-end opens the panel, and that gesture travels rightwards only under an LTR layout.
private fun expandsRow(movedRight: Boolean, layoutDirection: LayoutDirection) =
    movedRight == (layoutDirection == LayoutDirection.Ltr)

// Physical pixels, unlike Modifier.offset, which mirrors under RTL — the row follows the finger.
// Read in the layout phase, so following it never costs a recomposition.
private fun Modifier.horizontalTravel(travel: () -> Float) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.placeWithLayer(travel().roundToInt(), 0)
    }
}
