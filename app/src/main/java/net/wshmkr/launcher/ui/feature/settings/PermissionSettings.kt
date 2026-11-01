package net.wshmkr.launcher.ui.feature.settings

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import net.wshmkr.launcher.service.LauncherAccessibilityService
import net.wshmkr.launcher.ui.common.components.ToggleMenuOption
import net.wshmkr.launcher.util.NotificationPanelHelper
import net.wshmkr.launcher.util.isPermissionPermanentlyDenied
import net.wshmkr.launcher.util.openAppDetailsSettings

@Composable
fun PermissionSettings(
    context: Context
) {
    Column {
        SettingsSectionHeader("Permissions")

        SpecialAccessOption(
            context = context,
            text = "Accessibility service",
            subtext = "Used for notification drawer control",
            settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
            isEnabled = ::isAccessibilityServiceEnabled,
        )

        SpecialAccessOption(
            context = context,
            text = "Notification access",
            subtext = "Used for media controls",
            settingsAction = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
            isEnabled = NotificationPanelHelper::isNotificationListenerEnabled,
        )

        RuntimePermissionOption(
            context = context,
            text = "Location access",
            subtext = "Used for weather",
            permission = Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        RuntimePermissionOption(
            context = context,
            text = "Calendar access",
            subtext = "Used for today's events",
            permission = Manifest.permission.READ_CALENDAR,
        )
    }
}

// Seeded eagerly so the switch doesn't animate from off when the screen opens.
@Composable
private fun SpecialAccessOption(
    context: Context,
    text: String,
    subtext: String,
    settingsAction: String,
    isEnabled: (Context) -> Boolean,
) {
    var enabled by remember { mutableStateOf(isEnabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabled = isEnabled(context)
    }
    val openSettings = remember(context, settingsAction) {
        { _: Boolean -> context.startActivity(Intent(settingsAction)) }
    }
    ToggleMenuOption(
        text = text,
        subtext = subtext,
        checked = enabled,
        onCheckedChange = openSettings,
    )
}

@Composable
private fun RuntimePermissionOption(
    context: Context,
    text: String,
    subtext: String,
    permission: String,
) {
    var granted by remember { mutableStateOf(context.isGranted(permission)) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        permanentlyDenied = !isGranted && isPermissionPermanentlyDenied(context, permission)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = context.isGranted(permission)
        if (granted) permanentlyDenied = false
    }
    val onClick = remember(context, permission, permissionLauncher) {
        { _: Boolean ->
            if (granted || permanentlyDenied) {
                openAppDetailsSettings(context)
            } else {
                permissionLauncher.launch(permission)
            }
        }
    }
    ToggleMenuOption(
        text = text,
        subtext = subtext,
        checked = granted,
        onCheckedChange = onClick,
    )
}

private fun Context.isGranted(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = ComponentName(context, LauncherAccessibilityService::class.java)
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.split(':').any { ComponentName.unflattenFromString(it) == service }
}
