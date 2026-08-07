package net.wshmkr.launcher.ui.feature.notifications

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
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

// Short enough to feel immediate, far enough that a scroll can't trip it.
private val COMMIT_DISTANCE = 64.dp

private val SETTLE_SPEC = AnchoredDraggableDefaults.SnapAnimationSpec

private val ROW_SHAPE = Corners.small

// Material's dragged state layer alpha.
private const val DRAG_TINT_ALPHA = 0.12f

private val ARROW_GAP = 4.dp

private const val EXPAND_LABEL = "Expand"
private const val DISMISS_LABEL = "Dismiss"
private const val UNCLEARABLE_LABEL = "Can't dismiss"

@Composable
fun NotificationSwipeBox(
    swipeTarget: NotificationInfo?,
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

    // Taken at commit rather than at release, so a repost landing mid-gesture is never cancelled.
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

    // A drag cancels the clickable's ripple, so the tint is painted here instead. A press has to
    // land at once, so only the release animates.
    var dragging by remember { mutableStateOf(false) }
    val tint = animateFloatAsState(
        targetValue = if (dragging) DRAG_TINT_ALPHA else 0f,
        animationSpec = if (dragging) snap() else SETTLE_SPEC,
        label = "dragTint",
    )
    val dragTint = MaterialTheme.colorScheme.onSurface

    // None of the underlay shows at rest, so rows only pay to compose it mid-gesture.
    var gesturing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            // Owned here, not by the caller, so the underlay cuts at the radius it clips to.
            .clip(ROW_SHAPE)
            .onSizeChanged { rowWidth = it.width }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                // Stays on through the settle so it isn't cut short by the last dismissal.
                enabled = swipeTarget != null || armedTarget != null,
                onDragStarted = {
                    settle?.cancel()
                    gesturing = true
                    dragging = true
                },
                // Also reached when the gesture is cancelled, so neither flag can stay latched.
                onDragStopped = {
                    dragging = false
                    if (abs(travel) >= commitPx) {
                        if (expandsRow(movedRight, layoutDirection)) {
                            if (latestTarget != null) onExpand()
                        } else {
                            armedTarget?.takeIf { it.isClearable }?.let(onDismissNotification)
                        }
                    }
                    settle = settleScope.launch {
                        animate(travel, 0f, animationSpec = SETTLE_SPEC) { value, _ ->
                            travel = value
                        }
                        armedTarget = null
                        gesturing = false
                    }
                },
            ),
        propagateMinConstraints = true,
    ) {
        val backgroundTarget = swipeTarget ?: armedTarget
        if (gesturing && backgroundTarget != null) {
            SwipeActionBackground(
                travel = { travel },
                movedRight = movedRight,
                canDismiss = backgroundTarget.isClearable,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                // Physical pixels, unlike Modifier.offset, which mirrors under RTL — the row
                // follows the finger. Read in the layer, so following it costs no recomposition.
                .graphicsLayer { translationX = travel }
                .drawWithCache {
                    val radius = rowCornerRadius()
                    onDrawWithContent {
                        drawContent()
                        if (tint.value <= 0f) return@onDrawWithContent
                        drawRoundRect(color = dragTint, cornerRadius = radius, alpha = tint.value)
                    }
                },
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

    Box(
        modifier = modifier
            // Purely a drag affordance; leaving it in the tree makes every row read out twice.
            .clearAndSetSemantics {}
            .drawWithCache {
                val atRest = Path()
                atRest.addRoundRect(RoundRect(0f, 0f, size.width, size.height, rowCornerRadius()))
                val outline = Path()
                onDrawWithContent {
                    val offset = travel()
                    if (offset == 0f) return@onDrawWithContent
                    outline.rewind()
                    outline.addPath(atRest, Offset(offset, 0f))
                    // The row's own corners cut the fill, so it reads as something on top.
                    clipPath(outline, ClipOp.Difference) {
                        drawRect(fill)
                        this@onDrawWithContent.drawContent()
                    }
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ARROW_GAP),
            // Pinned to the physical side the row uncovered, not a layout-relative one.
            modifier = Modifier
                .align(if (movedRight) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight)
                .padding(horizontal = Spacing.medium),
        ) {
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

// Sized in sp so it tracks the system font scale along with the label.
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

// Only topStart is consulted, so the row is assumed to be evenly rounded.
private fun CacheDrawScope.rowCornerRadius() = CornerRadius(ROW_SHAPE.topStart.toPx(size, this))
