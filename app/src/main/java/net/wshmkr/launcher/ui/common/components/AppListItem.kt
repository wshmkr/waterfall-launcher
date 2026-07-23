package net.wshmkr.launcher.ui.common.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.ui.feature.notifications.NotificationPreview
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    appInfo: AppInfo,
    isActiveUser: Boolean,
    onClick: (AppInfo) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onToggleHidden: (AppInfo) -> Unit,
    onToggleSuggest: (AppInfo) -> Unit,
    onLongClick: ((AppInfo) -> Unit)? = null,
    targetAlpha: Float = 1f,
    isActiveLetter: Boolean = false,
    notifications: ImmutableList<NotificationInfo> = persistentListOf(),
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    val animatedAlpha by animateLetterFilterAlpha(
        targetAlpha = targetAlpha,
        isActiveLetter = isActiveLetter,
        label = "app_item_alpha"
    )

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

    Row(
        modifier = Modifier
            .padding(start = Spacing.small, end = dimensions.gutterLarge)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onClick(appInfo) },
                onLongClick = {
                    onLongClick?.invoke(appInfo)
                    showBottomSheet = true
                }
            )
            .padding(Spacing.small)
            .alpha(animatedAlpha),
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
            if (notifications.isNotEmpty()) {
                NotificationPreview(appInfo.label, appInfo.isHidden, notifications)
            } else {
                AppTitle(appInfo.label, appInfo.isHidden)
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
}

@Composable
fun AppTitle(title: String, isHidden: Boolean) {
    Text(
        text = title,
        fontSize = LocalDimensions.current.fontMedium,
        color = Color.White,
        maxLines = 1,
        fontStyle = if (isHidden) FontStyle.Italic else FontStyle.Normal,
        overflow = TextOverflow.Ellipsis,
    )
}
