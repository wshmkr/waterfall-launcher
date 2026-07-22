package net.wshmkr.launcher.ui.feature.settings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import net.wshmkr.launcher.ui.Screen
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.ui.common.components.MenuOptionTextSize
import net.wshmkr.launcher.ui.common.components.ToggleMenuOption
import net.wshmkr.launcher.ui.common.icons.CalendarTodayIcon
import net.wshmkr.launcher.ui.common.icons.MusicVideoIcon
import net.wshmkr.launcher.ui.common.icons.PartlyCloudyDayIcon
import net.wshmkr.launcher.ui.common.icons.ScheduleIcon
import net.wshmkr.launcher.ui.common.icons.WallpaperIcon
import net.wshmkr.launcher.ui.common.icons.WidgetsIcon
import net.wshmkr.launcher.viewmodel.SettingsViewModel

@Composable
fun HomeScreenSettings(
    context: Context,
    navController: NavController,
    viewModel: SettingsViewModel
) {
    SettingsSectionHeader("Home Screen")

    ChangeWallpaperRow(context = context, viewModel = viewModel)

    Spacer(modifier = Modifier.height(8.dp))
    ClockRow(viewModel = viewModel)
    Use24HourRow(viewModel = viewModel)

    Spacer(modifier = Modifier.height(8.dp))
    CalendarRow(viewModel = viewModel)
    TodaysEventsRow(viewModel = viewModel)

    Spacer(modifier = Modifier.height(8.dp))
    WeatherRow(viewModel = viewModel)
    UseFahrenheitRow(viewModel = viewModel)
    WeatherLocationRow(navController = navController, viewModel = viewModel)

    Spacer(modifier = Modifier.height(8.dp))
    MediaControlsRow(viewModel = viewModel)

    Spacer(modifier = Modifier.height(8.dp))
    ManageWidgetsRow(navController = navController)
}

@Composable
private fun ChangeWallpaperRow(
    context: Context,
    viewModel: SettingsViewModel,
) {
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
    val onPick = remember(imagePickerLauncher) {
        { imagePickerLauncher.launch(arrayOf("image/*")) }
    }

    MenuOption(
        icon = WallpaperIcon(),
        text = "Change wallpaper",
        color = Color.White,
        onClick = onPick,
    )
}

@Composable
private fun ClockRow(viewModel: SettingsViewModel) {
    val checked by viewModel.showClock.collectAsStateWithLifecycle()
    val onChange = remember(viewModel) { viewModel::setShowClock }
    ToggleMenuOption(
        icon = ScheduleIcon(),
        text = "Clock",
        color = Color.White,
        checked = checked,
        onCheckedChange = onChange,
    )
}

@Composable
private fun Use24HourRow(viewModel: SettingsViewModel) {
    val checked by viewModel.use24Hour.collectAsStateWithLifecycle()
    val onChange = remember(viewModel) { viewModel::setUse24Hour }
    ToggleMenuOption(
        text = "Time format",
        textSize = MenuOptionTextSize.Small,
        indent = 1,
        color = Color.White,
        checked = checked,
        onCheckedChange = onChange,
        offText = "12",
        onText = "24",
    )
}

@Composable
private fun CalendarRow(viewModel: SettingsViewModel) {
    val checked by viewModel.showCalendar.collectAsStateWithLifecycle()
    val onChange = remember(viewModel) { viewModel::setShowCalendar }
    ToggleMenuOption(
        icon = CalendarTodayIcon(),
        text = "Calendar",
        color = Color.White,
        checked = checked,
        onCheckedChange = onChange,
    )
}

@Composable
private fun TodaysEventsRow(viewModel: SettingsViewModel) {
    val checked by viewModel.showCalendarEvents.collectAsStateWithLifecycle()
    val onChange = remember(viewModel) { viewModel::setShowCalendarEvents }
    ToggleMenuOption(
        text = "Today's events",
        textSize = MenuOptionTextSize.Small,
        indent = 1,
        color = Color.White,
        checked = checked,
        onCheckedChange = onChange,
    )
}

@Composable
private fun WeatherRow(viewModel: SettingsViewModel) {
    val checked by viewModel.showWeather.collectAsStateWithLifecycle()
    val onChange = remember(viewModel) { viewModel::setShowWeather }
    ToggleMenuOption(
        icon = PartlyCloudyDayIcon(),
        text = "Weather",
        color = Color.White,
        checked = checked,
        onCheckedChange = onChange,
    )
}

@Composable
private fun UseFahrenheitRow(viewModel: SettingsViewModel) {
    val checked by viewModel.useFahrenheit.collectAsStateWithLifecycle()
    val onChange = remember(viewModel) { viewModel::setUseFahrenheit }
    ToggleMenuOption(
        text = "Temperature unit",
        textSize = MenuOptionTextSize.Small,
        indent = 1,
        color = Color.White,
        checked = checked,
        onCheckedChange = onChange,
        offText = "°C",
        onText = "°F",
    )
}

@Composable
private fun WeatherLocationRow(
    navController: NavController,
    viewModel: SettingsViewModel,
) {
    val name by viewModel.weatherLocationName.collectAsStateWithLifecycle()
    val lat by viewModel.weatherLat.collectAsStateWithLifecycle()
    val lon by viewModel.weatherLon.collectAsStateWithLifecycle()
    val label = if (lat != null && lon != null && !name.isNullOrBlank()) name else "Device location"
    val onClick = remember(navController) {
        { navController.navigate(Screen.WeatherLocation.route); Unit }
    }
    MenuOption(
        text = "Weather location",
        subtext = label,
        textSize = MenuOptionTextSize.Small,
        indent = 1,
        color = Color.White,
        onClick = onClick,
    )
}

@Composable
private fun MediaControlsRow(viewModel: SettingsViewModel) {
    val checked by viewModel.showMediaControls.collectAsStateWithLifecycle()
    val onChange = remember(viewModel) { viewModel::setShowMedia }
    ToggleMenuOption(
        icon = MusicVideoIcon(),
        text = "Media controls",
        color = Color.White,
        checked = checked,
        onCheckedChange = onChange,
    )
}

@Composable
private fun ManageWidgetsRow(navController: NavController) {
    val onClick = remember(navController) {
        { navController.navigate(Screen.WidgetList.route); Unit }
    }
    MenuOption(
        icon = WidgetsIcon(),
        text = "Manage widgets",
        color = Color.White,
        onClick = onClick,
    )
}
