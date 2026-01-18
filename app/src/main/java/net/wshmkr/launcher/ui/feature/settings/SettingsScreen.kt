package net.wshmkr.launcher.ui.feature.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import net.wshmkr.launcher.ui.Screen
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.ui.common.components.MenuOptionSwitch
import net.wshmkr.launcher.ui.common.components.MenuOptionTextSize
import net.wshmkr.launcher.ui.common.icons.CalendarTodayIcon
import net.wshmkr.launcher.ui.common.icons.MusicVideoIcon
import net.wshmkr.launcher.ui.common.icons.PartlyCloudyDayIcon
import net.wshmkr.launcher.ui.common.icons.ScheduleIcon
import net.wshmkr.launcher.ui.common.icons.WallpaperIcon
import net.wshmkr.launcher.ui.common.icons.WidgetsIcon
import net.wshmkr.launcher.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isLocationEnabled = isGranted
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setBackgroundUri(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .verticalScroll(rememberScrollState())
        ) {
            val settings = viewModel.homeWidgetSettings
            var useFahrenheit by remember { mutableStateOf(false) }
            var use24Hour by remember { mutableStateOf(false) }
            var useAutoLocation by remember { mutableStateOf(false) }

            Text(
                text = "Home Screen",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuOption(
                icon = WallpaperIcon(),
                text = "Change wallpaper",
                color = Color.White,
                onClick = {
                    imagePickerLauncher.launch(arrayOf("image/*"))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            MenuOption(
                icon = ScheduleIcon(),
                text = "Clock",
                color = Color.White,
                onClick = { viewModel.setShowClock(!settings.showClock) },
                endContent = {
                    MenuOptionSwitch(
                        checked = settings.showClock,
                        onCheckedChange = { viewModel.setShowClock(it) }
                    )
                }
            )

            MenuOption(
                text = "Time format",
                textSize = MenuOptionTextSize.Small,
                indent = 1,
                color = Color.White,
                onClick = { use24Hour = !use24Hour },
                endContent = {
                    MenuOptionSwitch(
                        checked = use24Hour,
                        onCheckedChange = { use24Hour = it },
                        offText = "12",
                        onText = "24"
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            MenuOption(
                icon = CalendarTodayIcon(),
                text = "Calendar",
                color = Color.White,
                onClick = { viewModel.setShowCalendar(!settings.showCalendar) },
                endContent = {
                    MenuOptionSwitch(
                        checked = settings.showCalendar,
                        onCheckedChange = { viewModel.setShowCalendar(it) }
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            MenuOption(
                icon = PartlyCloudyDayIcon(),
                text = "Weather",
                color = Color.White,
                onClick = { viewModel.setShowWeather(!settings.showWeather) },
                endContent = {
                    MenuOptionSwitch(
                        checked = settings.showWeather,
                        onCheckedChange = { viewModel.setShowWeather(it) }
                    )
                }
            )

            MenuOption(
                text = "Temperature unit",
                textSize = MenuOptionTextSize.Small,
                indent = 1,
                color = Color.White,
                onClick = { useFahrenheit = !useFahrenheit },
                endContent = {
                    MenuOptionSwitch(
                        checked = useFahrenheit,
                        onCheckedChange = { useFahrenheit = it },
                        offText = "°C",
                        onText = "°F"
                    )
                }
            )

            MenuOption(
                text = "Auto-determine location",
                textSize = MenuOptionTextSize.Small,
                indent = 1,
                color = Color.White,
                onClick = { useAutoLocation = !useAutoLocation },
                endContent = {
                    MenuOptionSwitch(
                        checked = useAutoLocation,
                        onCheckedChange = { useAutoLocation = it }
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            MenuOption(
                icon = MusicVideoIcon(),
                text = "Media controls",
                color = Color.White,
                onClick = { viewModel.setShowMedia(!settings.showMediaControls) },
                endContent = {
                    MenuOptionSwitch(
                        checked = settings.showMediaControls,
                        onCheckedChange = { viewModel.setShowMedia(it) }
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            MenuOption(
                icon = WidgetsIcon(),
                text = "Manage widgets",
                color = Color.White,
                onClick = {
                    navController.navigate(Screen.WidgetList.route)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Permissions",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuOption(
                text = "Accessibility service",
                subtext = "Used for notification drawer control",
                color = Color.White,
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                endContent = {
                    MenuOptionSwitch(
                        checked = isAccessibilityEnabled,
                        onCheckedChange = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }
            )

            MenuOption(
                text = "Notification access",
                subtext = "Used for media controls",
                color = Color.White,
                onClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                },
                endContent = {
                    MenuOptionSwitch(
                        checked = isNotificationAccessEnabled,
                        onCheckedChange = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }
            )

            MenuOption(
                text = "Location access",
                subtext = "Used for weather",
                color = Color.White,
                onClick = {
                    if (isLocationEnabled) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                },
                endContent = {
                    MenuOptionSwitch(
                        checked = isLocationEnabled,
                        onCheckedChange = {
                            if (isLocationEnabled) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
