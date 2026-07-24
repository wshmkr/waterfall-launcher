package net.wshmkr.launcher.ui.feature.notifications

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.NotificationAction
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.ui.common.icons.CloseIcon
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.util.timeSince

private const val CARD_TEXT_MAX_LINES = 12

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPanel(
    appInfo: AppInfo,
    notifications: ImmutableList<NotificationInfo>,
    onDismissNotification: (String) -> Unit,
    onClearAll: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val dimensions = LocalDimensions.current

    // Dismissals flow back through the repository, emptying this list; close when nothing remains.
    LaunchedEffect(notifications.isEmpty()) {
        if (notifications.isEmpty()) {
            sheetState.hide()
            onDismiss()
        }
    }

    val ordered = remember(notifications) { notifications.sortedByDescending { it.timestamp } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium)
                .padding(top = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = appInfo.icon,
                    contentDescription = appInfo.label,
                    modifier = Modifier.size(dimensions.iconLarge)
                )
                Spacer(modifier = Modifier.width(Spacing.medium))
                Text(
                    text = appInfo.label,
                    fontSize = dimensions.fontXLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onClearAll(ordered.map { it.key }) }) {
                    Text("Clear all")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.small))

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                ordered.forEachIndexed { index, notification ->
                    NotificationCard(
                        notification = notification,
                        onOpen = onDismiss,
                        onDismissNotification = onDismissNotification,
                    )
                    if (index < ordered.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.small))
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationInfo,
    onOpen: () -> Unit,
    onDismissNotification: (String) -> Unit,
) {
    val context = LocalContext.current
    val dimensions = LocalDimensions.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Corners.small)
            .clickable {
                sendPendingIntent(context, notification.contentIntent)
                onOpen()
            }
            .padding(Spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            notification.title?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    fontSize = dimensions.fontMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            notification.text?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    fontSize = dimensions.fontSmall,
                    maxLines = CARD_TEXT_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            notification.subText?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    fontSize = dimensions.fontCaption,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = timeSince(notification.timestamp),
                fontSize = dimensions.fontCaption,
                color = Color.Gray,
            )
            if (notification.actions.isNotEmpty()) {
                NotificationActions(notification.actions)
            }
        }
        IconButton(onClick = { onDismissNotification(notification.key) }) {
            Icon(
                painter = CloseIcon(),
                contentDescription = "Dismiss notification",
                modifier = Modifier.size(dimensions.iconSmall),
                tint = Color.Gray,
            )
        }
    }
}

@Composable
private fun NotificationActions(actions: ImmutableList<NotificationAction>) {
    val context = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        actions.forEach { action ->
            TextButton(onClick = { sendPendingIntent(context, action.actionIntent) }) {
                Text(action.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// Fires a captured PendingIntent, allowing background activity starts (API 34+) since the
// launcher usually isn't the top app. Returns false when the intent is null or already cancelled.
internal fun sendPendingIntent(
    context: Context,
    pendingIntent: PendingIntent?,
    fillInIntent: Intent? = null,
): Boolean {
    if (pendingIntent == null) return false
    return try {
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
                .toBundle()
        } else {
            null
        }
        pendingIntent.send(context, 0, fillInIntent, null, null, null, options)
        true
    } catch (e: Exception) {
        false
    }
}
