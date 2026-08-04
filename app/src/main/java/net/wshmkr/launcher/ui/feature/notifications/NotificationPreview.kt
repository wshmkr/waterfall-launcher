package net.wshmkr.launcher.ui.feature.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.ui.common.components.AppTitle
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.util.ONE_DAY
import net.wshmkr.launcher.util.ONE_HOUR
import net.wshmkr.launcher.util.ONE_MINUTE
import net.wshmkr.launcher.util.ONE_WEEK
import net.wshmkr.launcher.util.timeSince

// Preferred call shape — stable params so the composable stays skippable.
@Composable
fun NotificationPreview(
    label: String,
    isHidden: Boolean,
    notifications: ImmutableList<NotificationInfo>,
) {
    val notification = notifications.firstOrNull()

    // Held through the exit so the lines have something to draw while they collapse.
    var retained by remember { mutableStateOf(notification) }
    if (notification != null) retained = notification

    val visibleState = remember { MutableTransitionState(notification != null) }
    visibleState.targetState = notification != null

    LaunchedEffect(visibleState.isIdle) {
        if (visibleState.isIdle && !visibleState.currentState) retained = null
    }

    NotificationAppTitle(
        label = label,
        isHidden = isHidden,
        notificationTimestamp = retained?.timestamp,
        count = notifications.size,
    )

    val previewFont = LocalDimensions.current.fontCaption

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            retained?.title?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    fontSize = previewFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            retained?.text?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    fontSize = previewFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    lineHeight = previewFont * 1.25f,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// Leaf that owns the age tick — parent siblings don't recompose on time changes.
@Composable
private fun NotificationAppTitle(
    label: String,
    isHidden: Boolean,
    notificationTimestamp: Long?,
    count: Int,
) {
    if (notificationTimestamp == null) {
        AppTitle(label, isHidden)
        return
    }
    val currentTime by rememberNotificationAgeTicker(notificationTimestamp)
    val display = remember(label, notificationTimestamp, currentTime) {
        "$label · ${timeSince(notificationTimestamp)}"
    }

    // A single notification is already spelled out below the title, so the count only earns its
    // place once something is stacked behind the preview.
    val stacked = count > 1
    var retainedCount by remember { mutableIntStateOf(count) }
    if (stacked) retainedCount = count

    Row(verticalAlignment = Alignment.CenterVertically) {
        AppTitle(display, isHidden, Modifier.weight(1f, fill = false))
        AnimatedVisibility(visible = stacked) {
            NotificationCountBadge(retainedCount)
        }
    }
}

@Composable
private fun NotificationCountBadge(count: Int) {
    Text(
        text = count.toString(),
        fontSize = LocalDimensions.current.fontCaption,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        modifier = Modifier
            .padding(start = BADGE_GAP)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .padding(horizontal = BADGE_HORIZONTAL_PADDING, vertical = BADGE_VERTICAL_PADDING),
    )
}

private val BADGE_GAP = 6.dp
private val BADGE_HORIZONTAL_PADDING = 6.dp
private val BADGE_VERTICAL_PADDING = 1.dp

// Cadence matches the age bucket — fresh notifications tick often, week-old ones rarely.
@Composable
private fun rememberNotificationAgeTicker(timestamp: Long): State<Long> =
    produceState(initialValue = System.currentTimeMillis(), key1 = timestamp) {
        while (isActive) {
            val age = System.currentTimeMillis() - timestamp
            val refreshInterval = when {
                age < ONE_HOUR -> ONE_MINUTE / 2
                age < ONE_DAY -> ONE_HOUR / 2
                age < ONE_WEEK -> ONE_DAY / 2
                else -> ONE_WEEK / 2
            }
            delay(refreshInterval.toLong())
            value = System.currentTimeMillis()
        }
    }

