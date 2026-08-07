package net.wshmkr.launcher.ui.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.ui.common.icons.ChevronRightIcon
import net.wshmkr.launcher.ui.feature.notifications.NotificationPanel
import net.wshmkr.launcher.ui.feature.notifications.NotificationPreview
import net.wshmkr.launcher.ui.feature.notifications.NotificationSwipeBox
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing

@Composable
fun AppListItem(
    appInfo: AppInfo,
    isActiveUser: Boolean,
    onClick: (AppInfo) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onToggleHidden: (AppInfo) -> Unit,
    onToggleSuggest: (AppInfo) -> Unit,
    onLongClick: ((AppInfo) -> Unit)? = null,
    alphaProvider: () -> Float = { 1f },
    notifications: ImmutableList<NotificationInfo> = persistentListOf(),
    onClearNotifications: (List<NotificationInfo>) -> Boolean = { false },
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showNotificationPanel by remember { mutableStateOf(false) }
    var pendingDismissals by remember { mutableStateOf(emptyList<NotificationInfo>()) }

    // Hides swiped notifications until the system callback lands, so the preview has already
    // swapped to the next one by the time the row settles back.
    val visibleNotifications = remember(notifications, pendingDismissals) {
        if (pendingDismissals.isEmpty()) return@remember notifications
        notifications
            .filterNot { shown -> pendingDismissals.any { shown.matches(it) } }
            .toImmutableList()
    }

    LaunchedEffect(notifications) {
        pendingDismissals = pendingDismissals.filter { pending ->
            notifications.any { it.matches(pending) }
        }
    }

    val inactiveFilter = remember(isActiveUser) {
        if (!isActiveUser) {
            ColorFilter.colorMatrix(ColorMatrix().apply {
                setToSaturation(0f)
            })
        } else {
            null
        }
    }

    val dimensions = LocalDimensions.current

    // The chevron consumes the gesture, so the row's long press has to be repeated on it.
    val openOptions = {
        onLongClick?.invoke(appInfo)
        showBottomSheet = true
    }

    NotificationSwipeBox(
        swipeTarget = visibleNotifications.firstOrNull(),
        onExpand = { showNotificationPanel = true },
        // Only hidden once the cancel is away; a dropped one leaves it on screen, where it belongs.
        onDismissNotification = {
            if (onClearNotifications(listOf(it))) pendingDismissals = pendingDismissals + it
        },
        modifier = Modifier
            .padding(start = Spacing.small, end = dimensions.gutterLarge)
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alphaProvider() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick(appInfo) },
                    onLongClick = openOptions,
                )
                .padding(Spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = appInfo.icon,
                contentDescription = appInfo.label,
                modifier = Modifier.size(dimensions.iconLarge),
                colorFilter = inactiveFilter
            )
            Spacer(modifier = Modifier.width(dimensions.iconGap))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Called unconditionally so it can animate the lines away rather than cut them off.
                NotificationPreview(
                    label = appInfo.label,
                    isHidden = appInfo.isHidden,
                    notifications = visibleNotifications,
                    trailing = {
                        NotificationChevron(
                            size = dimensions.iconLarge,
                            onOpen = { showNotificationPanel = true },
                            onLongClick = openOptions,
                        )
                    },
                )
            }
        }
    }

    if (showBottomSheet) {
        AppOptionsMenu(
            appInfo = appInfo,
            onDismiss = { showBottomSheet = false },
            onToggleFavorite = onToggleFavorite,
            onToggleHidden = onToggleHidden,
            onToggleSuggest = onToggleSuggest,
        )
    }

    if (showNotificationPanel) {
        NotificationPanel(
            appInfo = appInfo,
            notifications = visibleNotifications,
            // The panel tracks its own dismissals, so it has no use for the outcome.
            onClearNotifications = { onClearNotifications(it) },
            onDismiss = { showNotificationPanel = false },
        )
    }
}

@Composable
private fun NotificationChevron(size: Dp, onOpen: () -> Unit, onLongClick: () -> Unit) {
    Icon(
        painter = ChevronRightIcon(),
        contentDescription = "Show notifications",
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(start = Spacing.small)
            .size(size)
            .combinedClickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onOpen,
                onLongClick = onLongClick,
            )
            .padding(Spacing.small),
    )
}

// A repost reuses the key, so the timestamp is what tells the two apart.
private fun NotificationInfo.matches(other: NotificationInfo) =
    key == other.key && timestamp == other.timestamp

@Composable
fun AppTitle(title: String, isHidden: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier,
        fontSize = LocalDimensions.current.fontMedium,
        maxLines = 1,
        fontStyle = if (isHidden) FontStyle.Italic else FontStyle.Normal,
        overflow = TextOverflow.Ellipsis,
    )
}
