package net.wshmkr.launcher.ui.common.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.ui.common.icons.CheckIcon
import net.wshmkr.launcher.ui.common.icons.CloseIcon
import net.wshmkr.launcher.ui.common.icons.DeleteIcon
import net.wshmkr.launcher.ui.common.icons.InfoIcon
import net.wshmkr.launcher.ui.common.icons.StarFilledIcon
import net.wshmkr.launcher.ui.common.icons.StarIcon
import net.wshmkr.launcher.ui.common.icons.VisibilityIcon
import net.wshmkr.launcher.ui.common.icons.VisibilityOffIcon
import net.wshmkr.launcher.viewmodel.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOptionsMenu(
    viewModel: LauncherViewModel,
    appInfo: AppInfo,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberDrawablePainter(drawable = appInfo.icon),
                    contentDescription = appInfo.label,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = appInfo.label,
                    fontSize = 20.sp,
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
                        viewModel.toggleSuggest(appInfo)
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
                        viewModel.toggleSuggest(appInfo)
                        onDismiss()
                    }
                )
            }

            if (appInfo.isFavorite) {
                MenuOption(
                    icon = StarIcon(),
                    text = "Remove from favorites",
                    onClick = {
                        viewModel.toggleFavorite(appInfo)
                        onDismiss()
                    }
                )
            } else {
                MenuOption(
                    icon = StarFilledIcon(),
                    text = "Favorite",
                    onClick = {
                        viewModel.toggleFavorite(appInfo)
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
                        viewModel.toggleHidden(appInfo)
                        onDismiss()
                    }
                )
            } else {
                MenuOption(
                    icon = VisibilityOffIcon(),
                    text = "Hide from app list",
                    onClick = {
                        viewModel.toggleHidden(appInfo)
                        onDismiss()
                    }
                )
            }

            if (appInfo.isSystemApp) {
                Text(
                    text = "This is a system app and can't be uninstalled.",
                    fontSize = 14.sp,
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

@Composable
fun MenuOption(
    icon: Painter? = null,
    text: String,
    subtext: String? = null,
    onClick: () -> Unit,
    switch: Boolean? = null,
    onToggle: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                painter = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
        }
        Column {
            Text(
                text = text,
                fontSize = 18.sp
            )
            subtext?.let {
                Text(
                    text = subtext,
                    fontSize = 16.sp,
                    color = Color.Gray,
                )
            }
        }
        switch?.let {
            Spacer(modifier = Modifier.weight(1f))
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Switch(
                    modifier = Modifier.scale(0.8f),
                    checked = switch,
                    onCheckedChange = { onToggle?.invoke() },
                )
            }
        }
    }
}
