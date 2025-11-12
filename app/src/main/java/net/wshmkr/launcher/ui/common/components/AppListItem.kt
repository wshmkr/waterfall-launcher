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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.ui.feature.notifications.NotificationPreview
import net.wshmkr.launcher.viewmodel.LauncherViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    viewModel: LauncherViewModel,
    appInfo: AppInfo,
    alpha: Float = 1f,
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    viewModel.launchApp(appInfo.packageName)
                },
                onLongClick = { showBottomSheet = true }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .padding(12.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = appInfo.icon),
            contentDescription = appInfo.label,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (appInfo.hasNotifications) {
                NotificationPreview(appInfo)
            } else {
                Text(
                    text = appInfo.label,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    fontStyle = if (appInfo.isHidden) FontStyle.Italic else FontStyle.Normal
                )
            }
        }
    }
    
    if (showBottomSheet) {
        AppOptionsMenu(
            appInfo = appInfo,
            onDismiss = { showBottomSheet = false },
            viewModel = viewModel,
        )
    }
}
