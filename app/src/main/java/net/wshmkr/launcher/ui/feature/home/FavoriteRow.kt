package net.wshmkr.launcher.ui.feature.home

import android.os.UserHandle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.ui.common.components.AppListItem
import net.wshmkr.launcher.ui.common.components.ReorderHandle
import net.wshmkr.launcher.ui.common.components.reorderActions
import net.wshmkr.launcher.viewmodel.HomeViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

internal const val FAVORITE_CONTENT_TYPE = "favorite_app"
internal const val SUGGESTION_CONTENT_TYPE = "suggested_app"

private const val SUGGESTION_DIM_ALPHA = 0.4f

@Composable
fun LazyItemScope.FavoriteRow(
    item: AppInfo,
    previousFavoriteKey: String?,
    nextFavoriteKey: String?,
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
    val rowAlpha = animateFloatAsState(
        targetValue = if (reordering && item.isSuggested) SUGGESTION_DIM_ALPHA else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "rowAlpha",
    )
    ReorderableItem(
        state = reorderState,
        key = item.key,
        enabled = draggable,
        animateItemModifier = if (reordering) Modifier.animateItem() else Modifier,
    ) {
        AppListItem(
            appInfo = item,
            modifier = if (draggable) {
                Modifier
                    .draggableHandle()
                    .semantics {
                        customActions = reorderActions(
                            key = item.key,
                            previousKey = previousFavoriteKey,
                            nextKey = nextFavoriteKey,
                            move = onMoveFavorite,
                        )
                    }
            } else Modifier,
            isActiveUser = isActiveUser,
            onClick = onClick,
            onToggleFavorite = onToggleFavorite,
            onToggleHidden = onToggleHidden,
            onToggleSuggest = onToggleSuggest,
            alphaProvider = rowAlpha::value,
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

