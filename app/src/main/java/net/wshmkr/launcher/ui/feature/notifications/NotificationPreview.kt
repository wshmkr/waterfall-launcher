package net.wshmkr.launcher.ui.feature.notifications

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.ui.common.components.AppTitle
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
    val notification = remember(notifications) { notifications.maxByOrNull { it.timestamp } }

    NotificationAppTitle(
        label = label,
        isHidden = isHidden,
        notificationTimestamp = notification?.timestamp,
    )

    notification?.title?.let {
        if (it.isNotBlank()) {
            Text(
                text = it,
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    notification?.text?.let {
        if (it.isNotBlank()) {
            Text(
                text = it,
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 2,
                lineHeight = 15.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Leaf that owns the age tick — parent siblings don't recompose on time changes.
@Composable
private fun NotificationAppTitle(label: String, isHidden: Boolean, notificationTimestamp: Long?) {
    if (notificationTimestamp == null) {
        AppTitle(label, isHidden)
        return
    }
    val currentTime by rememberNotificationAgeTicker(notificationTimestamp)
    val display = remember(label, notificationTimestamp, currentTime) {
        "$label · ${timeSince(notificationTimestamp)}"
    }
    AppTitle(display, isHidden)
}

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

