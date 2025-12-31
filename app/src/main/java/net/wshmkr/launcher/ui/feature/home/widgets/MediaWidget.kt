package net.wshmkr.launcher.ui.feature.home.widgets

import android.content.Intent
import android.media.session.MediaController
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
import net.wshmkr.launcher.ui.common.dialog.NotificationAccessDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.wshmkr.launcher.model.MediaInfo
import net.wshmkr.launcher.ui.common.icons.MusicNoteIcon
import net.wshmkr.launcher.util.MediaSessionHelper
import net.wshmkr.launcher.util.NotificationPanelHelper

@Composable
fun MediaWidget() {
    val context = LocalContext.current
    var mediaInfo by remember { mutableStateOf<MediaInfo?>(null) }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val hasPermission = remember { mutableStateOf(NotificationPanelHelper.isNotificationListenerEnabled(context)) }

    LaunchedEffect(Unit) {
        while (isActive) {
            hasPermission.value = NotificationPanelHelper.isNotificationListenerEnabled(context)
            if (hasPermission.value) {
                val info = MediaSessionHelper.getActiveMediaInfo(context)
                mediaInfo = info.first
                mediaController = info.second
            } else {
                mediaInfo = null
                mediaController = null
            }
            delay(500)
        }
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

    if (!hasPermission.value) {
        MediaPermissionPrompt(onRequestPermission = { showPermissionDialog = true })
        return
    }

    val currentMediaInfo = mediaInfo
    if (currentMediaInfo != null) {
        MediaControls(
            mediaInfo = currentMediaInfo,
            mediaController = mediaController,
            onMediaAppClick = {
                currentMediaInfo.packageName?.let { pkg ->
                    try {
                        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                        intent?.let {
                            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(it)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
    Spacer(modifier = Modifier.height(16.dp))
}
