package net.wshmkr.launcher.ui.feature.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

// Stable params throughout, so the composable stays skippable for every row in the list.
@Composable
fun NotificationPreview(
    label: String,
    isHidden: Boolean,
    notifications: ImmutableList<NotificationInfo>,
    trailing: @Composable () -> Unit = {},
) {
    val notification = notifications.firstOrNull()

    // Held through the exit so the lines have something to draw while they collapse.
    var retained by remember { mutableStateOf(notification) }
    if (notification != null) retained = notification

    // One state can't back two AnimatedVisibility, and these sit in different parts of the layout.
    val titleState = rememberPresenceState(notification != null)
    val bodyState = rememberPresenceState(notification != null)

    LaunchedEffect(titleState.isIdle && bodyState.isIdle) {
        if (titleState.isIdle && bodyState.isIdle && !titleState.currentState) retained = null
    }

    val previewFont = LocalDimensions.current.fontCaption

    // Trailing content spans both title lines, so a touch target fits without stretching either.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            NotificationAppTitle(
                label = label,
                isHidden = isHidden,
                notificationTimestamp = retained?.timestamp,
                count = notifications.size,
                // Live rather than retained, so the suffix leaves as the lines start to.
                hasNotification = notification != null,
            )

            AnimatedVisibility(
                visibleState = titleState,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                retained?.title?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        fontSize = previewFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // Driven here so it leaves on the same axis as the suffix beside it.
        AnimatedVisibility(
            visible = notification != null,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            trailing()
        }
    }

    AnimatedVisibility(
        visibleState = bodyState,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
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

// Leaf that owns the age tick — parent siblings don't recompose on time changes.
@Composable
private fun NotificationAppTitle(
    label: String,
    isHidden: Boolean,
    notificationTimestamp: Long?,
    count: Int,
    hasNotification: Boolean,
) {
    // Above the early return so the state survives the null branch to animate a later arrival.
    val suffixState = rememberPresenceState(hasNotification)

    if (notificationTimestamp == null) {
        AppTitle(label, isHidden)
        return
    }
    val currentTime by rememberNotificationAgeTicker(notificationTimestamp)

    // Held alongside the retained timestamp so the title doesn't drop the count mid-collapse.
    var retainedCount by remember { mutableIntStateOf(count) }
    if (count > 0) retainedCount = count

    val suffix = remember(retainedCount, notificationTimestamp, currentTime) {
        // One notification is already spelled out below, so a count only earns its place stacked.
        val stack = if (retainedCount > 1) " ($retainedCount)" else ""
        "$stack · ${timeSince(notificationTimestamp)}"
    }

    // Only the name is weighted, so a long one ellipsizes into the space the suffix leaves behind.
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppTitle(label, isHidden, Modifier.weight(1f, fill = false))
        AnimatedVisibility(
            visibleState = suffixState,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            AppTitle(suffix, isHidden)
        }
    }
}

// Seeded so first composition — a row scrolling into view — snaps rather than animating in.
@Composable
private fun rememberPresenceState(present: Boolean): MutableTransitionState<Boolean> =
    remember { MutableTransitionState(present) }.apply { targetState = present }

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

