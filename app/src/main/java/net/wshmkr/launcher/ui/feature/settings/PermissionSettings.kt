package net.wshmkr.launcher.ui.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import net.wshmkr.launcher.ui.common.components.ToggleMenuOption

@Composable
fun PermissionSettings(
    context: Context
) {
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isNotificationAccessEnabled by remember { mutableStateOf(false) }
    var isLocationEnabled by remember { mutableStateOf(false) }

    fun checkPermissions() {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        isAccessibilityEnabled = enabledServices.contains(context.packageName)

        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        isNotificationAccessEnabled = enabledListeners.contains(context.packageName)

        isLocationEnabled = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isLocationEnabled = isGranted
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        checkPermissions()
    }

    val openAccessibilitySettings = {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    val openNotificationListenerSettings = {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
    val handleLocationClick = {
        if (isLocationEnabled) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    Column {
        SettingsSectionHeader("Permissions")

        ToggleMenuOption(
            text = "Accessibility service",
            subtext = "Used for notification drawer control",
            color = Color.White,
            checked = isAccessibilityEnabled,
            onCheckedChange = { openAccessibilitySettings() },
        )

        ToggleMenuOption(
            text = "Notification access",
            subtext = "Used for media controls",
            color = Color.White,
            checked = isNotificationAccessEnabled,
            onCheckedChange = { openNotificationListenerSettings() },
        )

        ToggleMenuOption(
            text = "Location access",
            subtext = "Used for weather",
            color = Color.White,
            checked = isLocationEnabled,
            onCheckedChange = { handleLocationClick() },
        )
    }
}
