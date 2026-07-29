package net.wshmkr.launcher.ui.feature.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.NotificationAction
import net.wshmkr.launcher.model.NotificationDetail
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.model.ReplyInput
import net.wshmkr.launcher.ui.common.components.AppSheet
import net.wshmkr.launcher.ui.common.icons.CloseIcon
import net.wshmkr.launcher.ui.common.icons.SendIcon
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.ui.theme.sheetDivider
import net.wshmkr.launcher.util.sendPendingIntent
import net.wshmkr.launcher.util.sendReply
import net.wshmkr.launcher.util.timeSince

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
    val scope = rememberCoroutineScope()

    // Dismissals flow back through the repository, emptying this list; close when nothing remains.
    LaunchedEffect(notifications.isEmpty()) {
        if (notifications.isEmpty()) {
            sheetState.hide()
            onDismiss()
        }
    }

    // Ongoing and no-clear notifications reject cancelNotification, so never offer to clear them.
    val clearableKeys = remember(notifications) {
        notifications.filter { it.isClearable }.map { it.key }
    }

    AppSheet(
        appInfo = appInfo,
        onDismiss = onDismiss,
        // The sheet has its own window, so the activity's adjustResize doesn't reach the reply
        // field; without this the keyboard covers it.
        modifier = Modifier.imePadding(),
        sheetState = sheetState,
        headerAction = {
            if (clearableKeys.isNotEmpty()) {
                TextButton(
                    onClick = {
                        scope.launch {
                            // Slide out with the list intact; clearing first empties the sheet
                            // before it has moved, so it reads as vanishing rather than closing.
                            sheetState.hide()
                            onClearAll(clearableKeys)
                            onDismiss()
                        }
                    }
                ) {
                    Text("Clear all")
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Keyed so a card's dismissal state follows its notification rather than its slot.
            notifications.forEachIndexed { index, notification ->
                key(notification.key) {
                    NotificationCard(
                        notification = notification,
                        hasDividerAbove = index > 0,
                        onOpen = onDismiss,
                        onDismissNotification = onDismissNotification,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationInfo,
    hasDividerAbove: Boolean,
    onOpen: () -> Unit,
    onDismissNotification: (String) -> Unit,
) {
    val visibleState = remember { MutableTransitionState(true) }

    // Cancel only once the card has collapsed, so the rows below don't jump up mid-animation.
    LaunchedEffect(visibleState.isIdle) {
        if (visibleState.isIdle && !visibleState.currentState) {
            onDismissNotification(notification.key)
        }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column {
            if (hasDividerAbove) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.small),
                    color = sheetDivider(),
                )
            }
            NotificationCardContent(
                notification = notification,
                // Opening closes the sheet, which would cancel the exit animation before it
                // could dismiss, so this path cancels directly.
                onOpened = {
                    if (notification.cancelsOnOpen) onDismissNotification(notification.key)
                    onOpen()
                },
                onDismiss = { visibleState.targetState = false },
            )
        }
    }
}

@Composable
private fun NotificationCardContent(
    notification: NotificationInfo,
    onOpened: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val dimensions = LocalDimensions.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Corners.small)
            .clickable {
                // The shade applies FLAG_AUTO_CANCEL itself; firing the intent from a listener
                // doesn't, so the notification would linger after opening the app.
                if (sendPendingIntent(context, notification.contentIntent)) onOpened()
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
            // `text` is the collapsed one-liner restating the detail, so only one of them shows.
            when (val detail = notification.detail) {
                null -> notification.text?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, fontSize = dimensions.fontSmall)
                }

                is NotificationDetail.Conversation -> ConversationDetail(detail)

                is NotificationDetail.Lines -> Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    detail.lines.forEach { line ->
                        Text(text = line, fontSize = dimensions.fontSmall)
                    }
                }

                is NotificationDetail.LongText -> Text(
                    text = detail.text,
                    fontSize = dimensions.fontSmall,
                )
            }
            notification.subText?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    fontSize = dimensions.fontCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = timeSince(notification.timestamp),
                fontSize = dimensions.fontCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (notification.actions.isNotEmpty()) {
                NotificationActions(notification.actions)
            }
        }
        if (notification.isClearable) {
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = CloseIcon(),
                    contentDescription = "Dismiss notification",
                    modifier = Modifier.size(dimensions.iconSmall),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConversationDetail(conversation: NotificationDetail.Conversation) {
    val dimensions = LocalDimensions.current

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        conversation.messages.forEach { message ->
            Column {
                // In a one-to-one thread the other party is already the notification title, so
                // only their name in a group, and yours, are worth spelling out.
                val speaker = when {
                    message.sender == null -> "You"
                    conversation.isGroup -> message.sender
                    else -> null
                }
                speaker?.let {
                    Text(
                        text = it,
                        fontSize = dimensions.fontCaption,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(text = message.text, fontSize = dimensions.fontSmall)
            }
        }
    }
}

@Composable
private fun NotificationActions(actions: ImmutableList<NotificationAction>) {
    val context = LocalContext.current
    var activeReply by remember { mutableStateOf<NotificationAction?>(null) }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        actions.forEach { action ->
            TextButton(
                onClick = {
                    if (action.reply == null) {
                        sendPendingIntent(context, action.actionIntent)
                    } else {
                        activeReply = action.takeIf { it != activeReply }
                    }
                }
            ) {
                Text(action.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    val action = activeReply
    val reply = action?.reply
    if (action != null && reply != null) {
        ReplyComposer(
            reply = reply,
            onSend = { response, fromChoice ->
                if (sendReply(context, action, response, fromChoice)) activeReply = null
            },
        )
    }
}

@Composable
private fun ReplyComposer(
    reply: ReplyInput,
    onSend: (response: String, fromChoice: Boolean) -> Unit,
) {
    val dimensions = LocalDimensions.current
    var draft by remember(reply) { mutableStateOf("") }

    if (reply.choices.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            reply.choices.forEach { choice ->
                SuggestionChip(
                    onClick = { onSend(choice, true) },
                    label = { Text(choice, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }

    if (reply.allowsFreeFormInput) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.small),
            placeholder = { Text(reply.hint ?: "Reply") },
            textStyle = LocalTextStyle.current.copy(fontSize = dimensions.fontSmall),
            singleLine = true,
            shape = Corners.medium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = { if (draft.isNotBlank()) onSend(draft, false) }
            ),
            trailingIcon = {
                IconButton(
                    onClick = { onSend(draft, false) },
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(
                        painter = SendIcon(),
                        contentDescription = "Send reply",
                        modifier = Modifier.size(dimensions.iconSmall),
                    )
                }
            },
        )
    }
}
