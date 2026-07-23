package net.wshmkr.launcher.ui.common.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.ui.common.icons.CheckIcon
import net.wshmkr.launcher.ui.common.icons.CloseIcon
import net.wshmkr.launcher.ui.common.icons.DeleteIcon
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
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val dimensions = LocalDimensions.current

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
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = appInfo.icon,
                    contentDescription = appInfo.label,
                    modifier = Modifier.size(dimensions.iconLarge)
                )
                Spacer(modifier = Modifier.width(dimensions.iconGap))
                Text(
                    text = appInfo.label,
                    fontSize = dimensions.fontXLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
            )

            if (appInfo.isSuggested) {
                MenuOption(
                    icon = CloseIcon(),
                    text = "Stop suggesting",
                    subtext = "Don't show below favorites",
                    onClick = {
                        onToggleSuggest(appInfo)
                        onDismiss()
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
                        onDismiss()
                    }
                )
            }

            if (appInfo.isFavorite) {
                MenuOption(
                    icon = StarIcon(),
                    text = "Remove from favorites",
                    onClick = {
                        onToggleFavorite(appInfo)
                        onDismiss()
                    }
                )
            } else {
                MenuOption(
                    icon = StarFilledIcon(),
                    text = "Favorite",
                    onClick = {
                        onToggleFavorite(appInfo)
                        onDismiss()
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
                    onDismiss()
                }
            )

            if (appInfo.isHidden) {
                MenuOption(
                    icon = VisibilityIcon(),
                    text = "Show in app list",
                    onClick = {
                        onToggleHidden(appInfo)
                        onDismiss()
                    }
                )
            } else {
                MenuOption(
                    icon = VisibilityOffIcon(),
                    text = "Hide from app list",
                    onClick = {
                        onToggleHidden(appInfo)
                        onDismiss()
                    }
                )
            }

            if (appInfo.isSystemApp) {
                Text(
                    text = "This is a system app and can't be uninstalled.",
                    fontSize = dimensions.fontSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
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
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
