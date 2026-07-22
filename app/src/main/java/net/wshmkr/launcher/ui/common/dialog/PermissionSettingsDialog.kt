package net.wshmkr.launcher.ui.common.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PermissionSettingsDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AccessibilityServiceDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    PermissionSettingsDialog(
        title = "Enable Accessibility Service",
        message = "To expand the notification drawer, please enable the Launcher accessibility service in Settings.\n\n" +
                "Go to: Settings > Accessibility > Launcher",
        onDismiss = onDismiss,
        onOpenSettings = onOpenSettings
    )
}

@Composable
fun NotificationAccessDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    PermissionSettingsDialog(
        title = "Enable Notification Access",
        message = "To control media playback, please enable notification access for Launcher in Settings.\n\n" +
                "Go to: Settings > Apps > Special app access > Notification access > Launcher",
        onDismiss = onDismiss,
        onOpenSettings = onOpenSettings
    )
}
