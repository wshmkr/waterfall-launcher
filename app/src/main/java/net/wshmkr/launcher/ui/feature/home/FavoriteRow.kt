package net.wshmkr.launcher.ui.feature.home

import android.os.UserHandle
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.ui.common.components.AppListItem
import net.wshmkr.launcher.ui.common.icons.DragIndicatorIcon
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.viewmodel.HomeViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

internal const val FAVORITE_CONTENT_TYPE = "favorite_app"
internal const val SUGGESTION_CONTENT_TYPE = "suggested_app"

private val FULL_ALPHA: () -> Float = { 1f }
private val DIMMED_ALPHA: () -> Float = { 0.4f }

@Composable
fun LazyItemScope.FavoriteRow(
    index: Int,
    item: AppInfo,
    favoriteApps: List<AppInfo>,
    reordering: Boolean,
    reorderState: ReorderableLazyListState,
    activeProfiles: Set<UserHandle>,
    viewModel: HomeViewModel,
    onClick: (AppInfo) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onToggleHidden: (AppInfo) -> Unit,
    onToggleSuggest: (AppInfo) -> Unit,
    onClearNotifications: (List<NotificationInfo>) -> Boolean,
    onMoveFavorite: (String, String) -> Unit,
    onStartReorder: (() -> Unit)?,
) {
    val isActiveUser = remember(item.userHandle, activeProfiles) {
        item.userHandle in activeProfiles
    }
    val notifications by remember(item.key) {
        viewModel.notificationsFor(item.packageName, item.userHandle)
    }

    val draggable = reordering && !item.isSuggested
    ReorderableItem(
        state = reorderState,
        key = item.key,
        enabled = draggable,
        animateItemModifier = if (reordering) Modifier.animateItem() else Modifier,
    ) {
        AppListItem(
            appInfo = item,
            // The drag gesture is invisible to accessibility services, so
            // reordering is mirrored as custom actions on the row.
            modifier = if (draggable) {
                Modifier
                    .draggableHandle()
                    .semantics {
                        customActions = reorderActions(
                            item = item,
                            previous = favoriteApps.getOrNull(index - 1),
                            next = favoriteApps.getOrNull(index + 1),
                            moveFavorite = onMoveFavorite,
                        )
                    }
            } else Modifier,
            isActiveUser = isActiveUser,
            onClick = onClick,
            onToggleFavorite = onToggleFavorite,
            onToggleHidden = onToggleHidden,
            onToggleSuggest = onToggleSuggest,
            alphaProvider = if (reordering && item.isSuggested) DIMMED_ALPHA else FULL_ALPHA,
            notifications = notifications,
            onClearNotifications = onClearNotifications,
            onReorderFavorites = if (!reordering && !item.isSuggested) onStartReorder else null,
            clickEnabled = !reordering,
            dragHandle = if (draggable) {
                { ReorderHandle(item.label) }
            } else null,
        )
    }
}

private fun reorderActions(
    item: AppInfo,
    previous: AppInfo?,
    next: AppInfo?,
    moveFavorite: (String, String) -> Unit,
): List<CustomAccessibilityAction> = buildList {
    if (previous != null && !previous.isSuggested) {
        add(CustomAccessibilityAction("Move up") { moveFavorite(item.key, previous.key); true })
    }
    if (next != null && !next.isSuggested) {
        add(CustomAccessibilityAction("Move down") { moveFavorite(item.key, next.key); true })
    }
}

@Composable
private fun ReorderHandle(label: String, modifier: Modifier = Modifier) {
    Icon(
        painter = DragIndicatorIcon(),
        contentDescription = "Reorder $label",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .size(LocalDimensions.current.iconLarge)
            .padding(Spacing.small),
    )
}
