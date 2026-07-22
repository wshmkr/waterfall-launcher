package net.wshmkr.launcher.ui.feature.home.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import net.wshmkr.launcher.service.LauncherNotificationListenerService
import net.wshmkr.launcher.ui.common.dialog.NotificationAccessDialog
import net.wshmkr.launcher.ui.common.icons.MusicNoteIcon
import net.wshmkr.launcher.util.NotificationPanelHelper
import net.wshmkr.launcher.util.launchPackage
import net.wshmkr.launcher.viewmodel.MediaViewModel

@Composable
fun MediaWidget(enabled: Boolean = true) {
    if (!enabled) {
        return Spacer(modifier = Modifier.height(16.dp))
    }

    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(NotificationPanelHelper.isNotificationListenerEnabled(context))
    }
    val isListenerConnected by LauncherNotificationListenerService.isConnected.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = NotificationPanelHelper.isNotificationListenerEnabled(context)
    }
    LaunchedEffect(isListenerConnected) {
        hasPermission = NotificationPanelHelper.isNotificationListenerEnabled(context)
    }

    if (showPermissionDialog) {
        NotificationAccessDialog(
            onDismiss = { showPermissionDialog = false },
            onOpenSettings = {
                NotificationPanelHelper.openNotificationListenerSettings(context)
                showPermissionDialog = false
            }
        )
    }

    if (!hasPermission) {
        MediaPermissionPrompt(onRequestPermission = { showPermissionDialog = true })
        return
    }

    ActiveMediaControls()
}

@Composable
private fun ActiveMediaControls(viewModel: MediaViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val activeMediaSession by viewModel.activeMediaSession.collectAsState()

    val mediaInfo = activeMediaSession.mediaInfo
    if (mediaInfo != null) {
        MediaControls(
            mediaInfo = mediaInfo,
            mediaController = activeMediaSession.controller,
            onMediaAppClick = {
                mediaInfo.packageName?.let { pkg ->
                    launchPackage(context, pkg)
                }
            }
        )
    } else {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MediaPermissionPrompt(onRequestPermission: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onRequestPermission() }
            .background(Color.Black.copy(alpha = 0.5f))
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = MusicNoteIcon(),
            contentDescription = "Media",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enable notification access\nfor media controls",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
