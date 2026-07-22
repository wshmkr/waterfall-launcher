package net.wshmkr.launcher.ui.feature.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.core.app.ActivityCompat
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
    var isCalendarEnabled by remember { mutableStateOf(false) }

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

        isCalendarEnabled = context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isLocationEnabled = isGranted
    }

    var isCalendarPermanentlyDenied by remember { mutableStateOf(false) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isCalendarEnabled = isGranted
        isCalendarPermanentlyDenied = !isGranted && context.findActivity()?.let { activity ->
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.READ_CALENDAR,
            )
        } == true
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        checkPermissions()
    }

    val openAccessibilitySettings = remember(context) {
        { _: Boolean -> context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }
    val openNotificationListenerSettings = remember(context) {
        { _: Boolean -> context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }
    val handleLocationClick = remember(context, locationPermissionLauncher) {
        { _: Boolean ->
            if (isLocationEnabled) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    val handleCalendarClick = remember(context, calendarPermissionLauncher, isCalendarPermanentlyDenied) {
        { _: Boolean ->
            if (isCalendarEnabled || isCalendarPermanentlyDenied) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } else {
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        }
    }

    Column {
        SettingsSectionHeader("Permissions")

        ToggleMenuOption(
            text = "Accessibility service",
            subtext = "Used for notification drawer control",
            color = Color.White,
            checked = isAccessibilityEnabled,
            onCheckedChange = openAccessibilitySettings,
        )

        ToggleMenuOption(
            text = "Notification access",
            subtext = "Used for media controls",
            color = Color.White,
            checked = isNotificationAccessEnabled,
            onCheckedChange = openNotificationListenerSettings,
        )

        ToggleMenuOption(
            text = "Location access",
            subtext = "Used for weather",
            color = Color.White,
            checked = isLocationEnabled,
            onCheckedChange = handleLocationClick,
        )

        ToggleMenuOption(
            text = "Calendar access",
            subtext = "Used for today's events",
            color = Color.White,
            checked = isCalendarEnabled,
            onCheckedChange = handleCalendarClick,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
