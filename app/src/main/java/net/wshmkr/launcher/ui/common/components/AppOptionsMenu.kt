package net.wshmkr.launcher.ui.common.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.ui.common.icons.CheckIcon
import net.wshmkr.launcher.ui.common.icons.CloseIcon
import net.wshmkr.launcher.ui.common.icons.DeleteIcon
import net.wshmkr.launcher.ui.common.icons.DragIndicatorIcon
import net.wshmkr.launcher.ui.common.icons.InfoIcon
import net.wshmkr.launcher.ui.common.icons.StarFilledIcon
import net.wshmkr.launcher.ui.common.icons.StarIcon
import net.wshmkr.launcher.ui.common.icons.VisibilityIcon
import net.wshmkr.launcher.ui.common.icons.VisibilityOffIcon
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOptionsMenu(
    appInfo: AppInfo,
    onDismiss: () -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onToggleHidden: (AppInfo) -> Unit,
    onToggleSuggest: (AppInfo) -> Unit,
    onReorderFavorites: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val dimensions = LocalDimensions.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    fun slideOutThenDismiss() {
        scope.launch {
            try {
                sheetState.hide()
            } finally {
                onDismiss()
            }
        }
    }

    AppSheet(appInfo = appInfo, onDismiss = onDismiss, sheetState = sheetState) {
        if (appInfo.isSuggested) {
            MenuOption(
                icon = CloseIcon(),
                text = "Stop suggesting",
                subtext = "Don't show below favorites",
                onClick = {
                    onToggleSuggest(appInfo)
                    slideOutThenDismiss()
                }
            )
        }
        if (appInfo.doNotSuggest) {
            MenuOption(
                icon = CheckIcon(),
                text = "Suggest again",
                subtext = "Suggestions appear below favorites",
                onClick = {
                    onToggleSuggest(appInfo)
                    slideOutThenDismiss()
                }
            )
        }

        if (appInfo.isFavorite) {
            MenuOption(
                icon = StarIcon(),
                text = "Remove from favorites",
                onClick = {
                    onToggleFavorite(appInfo)
                    slideOutThenDismiss()
                }
            )
            ReorderFavoritesMenuOption(onReorderFavorites, ::slideOutThenDismiss)
        } else {
            MenuOption(
                icon = StarFilledIcon(),
                text = "Favorite",
                onClick = {
                    onToggleFavorite(appInfo)
                    slideOutThenDismiss()
                }
            )
        }

        MenuOption(
            icon = InfoIcon(),
            text = "App info",
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", appInfo.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                slideOutThenDismiss()
            }
        )

        if (appInfo.isHidden) {
            MenuOption(
                icon = VisibilityIcon(),
                text = "Show in app list",
                onClick = {
                    onToggleHidden(appInfo)
                    slideOutThenDismiss()
                }
            )
        } else {
            MenuOption(
                icon = VisibilityOffIcon(),
                text = "Hide from app list",
                onClick = {
                    onToggleHidden(appInfo)
                    slideOutThenDismiss()
                }
            )
        }

        if (appInfo.isSystemApp) {
            Text(
                text = "This is a system app and can't be uninstalled.",
                fontSize = dimensions.fontSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = Spacing.medium)
            )
        } else {
            MenuOption(
                icon = DeleteIcon(),
                text = "Uninstall",
                onClick = {
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.fromParts("package", appInfo.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    slideOutThenDismiss()
                }
            )
        }
    }
}

@Composable
fun ReorderFavoritesMenuOption(onReorderFavorites: (() -> Unit)?, onDismiss: () -> Unit) {
    if (onReorderFavorites == null) return
    MenuOption(
        icon = DragIndicatorIcon(),
        text = "Reorder favorites",
        onClick = {
            onReorderFavorites()
            onDismiss()
        }
    )
}
