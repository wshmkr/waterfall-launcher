package net.wshmkr.launcher.ui.feature.notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.min

private const val COMMIT_FRACTION = 0.4f

private const val EXPAND_LABEL = "Expand →"
private const val DISMISS_LABEL = "← Dismiss"
private const val UNCLEARABLE_LABEL = "Can't dismiss"

// Drag right to open the notification panel, left to dismiss the previewed notification. Both
// directions throw the row off the edge and settle it back once the action has run.
@Composable
fun NotificationSwipeBox(
    canSwipe: Boolean,
    canDismiss: Boolean,
    onExpand: () -> Unit,
    onDismissNotification: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val swipeState = rememberSwipeToDismissBoxState(positionalThreshold = { it * COMMIT_FRACTION })
    val coroutineScope = rememberCoroutineScope()

    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = { SwipeActionBackground(swipeState, canDismiss) },
        modifier = modifier,
        enableDismissFromStartToEnd = canSwipe,
        enableDismissFromEndToStart = canSwipe,
        onDismiss = { direction ->
            when {
                direction == SwipeToDismissBoxValue.StartToEnd -> onExpand()
                direction == SwipeToDismissBoxValue.EndToStart && canDismiss -> onDismissNotification()
            }
            coroutineScope.launch { swipeState.reset() }
        },
    ) {
        content()
    }
}

// Rows are transparent over the wallpaper, so only the strip the row has vacated may be painted —
// everything here reads the offset in the draw phase, which also keeps a drag off the recomposer.
@Composable
private fun SwipeActionBackground(swipeState: SwipeToDismissBoxState, canDismiss: Boolean) {
    val expandColor = MaterialTheme.colorScheme.secondaryContainer
    val dismissColor = when {
        canDismiss -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val offset = swipeState.offsetOrZero()
                if (offset == 0f) return@drawBehind
                drawRect(
                    color = if (offset > 0f) expandColor else dismissColor,
                    topLeft = Offset(revealedLeft(offset), 0f),
                    size = Size(revealedWidth(offset), size.height),
                )
            }
            .drawWithContent {
                val offset = swipeState.offsetOrZero()
                if (offset == 0f) return@drawWithContent
                val left = revealedLeft(offset)
                clipRect(left = left, right = left + revealedWidth(offset)) {
                    this@drawWithContent.drawContent()
                }
            }
    ) {
        SwipeActionLabel(
            text = EXPAND_LABEL,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        SwipeActionLabel(
            text = if (canDismiss) DISMISS_LABEL else UNCLEARABLE_LABEL,
            color = when {
                canDismiss -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

// Anchored to the edge rather than to the row, so it is uncovered in place as the row travels.
@Composable
private fun SwipeActionLabel(text: String, color: Color, modifier: Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = LocalDimensions.current.fontCaption,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = modifier.padding(horizontal = Spacing.medium),
    )
}

// Anchors are in place by the draw phase, but a launcher row must never be the thing that crashes.
private fun SwipeToDismissBoxState.offsetOrZero(): Float =
    runCatching { requireOffset() }.getOrDefault(0f)

private fun DrawScope.revealedWidth(offset: Float): Float = min(abs(offset), size.width)

private fun DrawScope.revealedLeft(offset: Float): Float =
    if (offset > 0f) 0f else size.width - revealedWidth(offset)
