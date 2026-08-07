package net.wshmkr.launcher.ui.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.ui.common.icons.ArrowLeftAltIcon
import net.wshmkr.launcher.ui.common.icons.ArrowRightAltIcon
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.min

private const val COMMIT_FRACTION = 0.4f

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
    val swipeState = rememberSwipeToDismissBoxState(positionalThreshold = { it * COMMIT_FRACTION })
    val coroutineScope = rememberCoroutineScope()

    // The row is thrown off the edge before onDismiss fires, so the target is taken the moment the
    // drag passes the threshold — whatever repost replaces the preview mid-throw is not what the
    // user acted on, and cancelling it would take content they never saw.
    val latestTarget by rememberUpdatedState(swipeTarget)
    var armedTarget by remember { mutableStateOf<NotificationInfo?>(null) }
    LaunchedEffect(swipeState) {
        snapshotFlow { swipeState.targetValue }
            .filter { it != SwipeToDismissBoxValue.Settled }
            .collect { armedTarget = latestTarget }
    }

    // Anchors have to outlive the gesture, or the row loses them mid-settle.
    val settling = swipeState.currentValue != SwipeToDismissBoxValue.Settled
    val backgroundTarget = swipeTarget ?: armedTarget

    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            if (backgroundTarget != null) {
                SwipeActionBackground(swipeState, backgroundTarget.isClearable)
            }
        },
        modifier = modifier,
        enableDismissFromStartToEnd = swipeTarget != null || settling,
        enableDismissFromEndToStart = swipeTarget != null || settling,
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> if (latestTarget != null) onExpand()
                SwipeToDismissBoxValue.EndToStart ->
                    armedTarget?.takeIf { it.isClearable }?.let(onDismissNotification)

                SwipeToDismissBoxValue.Settled -> Unit
            }
            coroutineScope.launch {
                swipeState.reset()
                armedTarget = null
            }
        },
    ) {
        content()
    }
}

// Rows are transparent over the wallpaper, so only the strip the row has vacated may be painted —
// the geometry is read in the draw phase, which keeps a drag off the recomposer.
@Composable
private fun SwipeActionBackground(swipeState: SwipeToDismissBoxState, canDismiss: Boolean) {
    // Only the sign of the offset picks the action, so it flips at most once per gesture — cheap
    // enough to drive the label from composition rather than show both and clip one away.
    val dismissing by remember(swipeState) {
        derivedStateOf { swipeState.offsetOrZero() < 0f }
    }

    val fill = when {
        !dismissing -> MaterialTheme.colorScheme.primary
        canDismiss -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        !dismissing -> MaterialTheme.colorScheme.onPrimary
        canDismiss -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Purely a drag affordance; leaving it in the tree makes every row read out twice.
            .clearAndSetSemantics {}
            .drawWithContent {
                val offset = swipeState.offsetOrZero()
                if (offset == 0f) return@drawWithContent
                val left = revealedLeft(offset)
                val width = revealedWidth(offset)
                drawRect(
                    color = fill,
                    topLeft = Offset(left, 0f),
                    size = Size(width, size.height),
                )
                clipRect(left = left, right = left + width) {
                    this@drawWithContent.drawContent()
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ARROW_GAP),
            // Anchored to the edge rather than the row, so it is uncovered in place as the row travels.
            modifier = Modifier
                .align(if (dismissing) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = Spacing.medium),
        ) {
            // The arrow points the way the row is travelling, so it stays put under RTL.
            if (dismissing && canDismiss) SwipeActionArrow(ArrowLeftAltIcon(), contentColor)
            Text(
                text = when {
                    !dismissing -> EXPAND_LABEL
                    canDismiss -> DISMISS_LABEL
                    else -> UNCLEARABLE_LABEL
                },
                color = contentColor,
                fontSize = LocalDimensions.current.fontMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            if (!dismissing) SwipeActionArrow(ArrowRightAltIcon(), contentColor)
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

// Anchors are in place by the draw phase, but a launcher row must never be the thing that crashes.
private fun SwipeToDismissBoxState.offsetOrZero(): Float =
    runCatching { requireOffset() }.getOrDefault(0f)

private fun DrawScope.revealedWidth(offset: Float): Float = min(abs(offset), size.width)

// A positive offset is a start-to-end swipe, which travels leftwards under an RTL layout.
private fun DrawScope.revealedLeft(offset: Float): Float =
    if ((offset > 0f) == (layoutDirection == LayoutDirection.Ltr)) {
        0f
    } else {
        size.width - revealedWidth(offset)
    }
