package net.wshmkr.launcher.ui.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    targetAlpha: Float = 1f,
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    val itemLetter = appInfo.label.first().uppercaseChar().toString()
    val isActiveLetter = if (viewModel is net.wshmkr.launcher.viewmodel.HomeViewModel) {
        itemLetter == viewModel.activeLetter
    } else {
        false
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = if (isActiveLetter || targetAlpha < 1f) {
            tween(durationMillis = 0)
        } else {
            tween(durationMillis = 300)
        },
        label = "app_item_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    viewModel.launchApp(appInfo.packageName)
                },
                onLongClick = { showBottomSheet = true }
            )
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .alpha(animatedAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = appInfo.icon),
            contentDescription = appInfo.label,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (appInfo.hasNotifications) {
                NotificationPreview(appInfo)
            } else {
                AppTitle(appInfo.label, appInfo.isHidden)
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

@Composable
fun AppTitle(title: String, isHidden: Boolean) {
    Text(
        text = title,
        fontSize = 16.sp,
        color = Color.White,
        maxLines = 1,
        fontStyle = if (isHidden) FontStyle.Italic else FontStyle.Normal
    )
}
