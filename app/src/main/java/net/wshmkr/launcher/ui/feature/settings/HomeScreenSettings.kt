package net.wshmkr.launcher.ui.feature.settings

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun HomeScreenSettings(
    context: Context,
    navController: NavController,
    viewModel: SettingsViewModel
) {
    val settings = viewModel.homeWidgetSettings
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
        onClick = { viewModel.setUse24Hour(!settings.use24Hour) },
        endContent = {
            MenuOptionSwitch(
                checked = settings.use24Hour,
                onCheckedChange = { viewModel.setUse24Hour(it) },
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
        onClick = { viewModel.setUseFahrenheit(!settings.useFahrenheit) },
        endContent = {
            MenuOptionSwitch(
                checked = settings.useFahrenheit,
                onCheckedChange = { viewModel.setUseFahrenheit(it) },
                offText = "°C",
                onText = "°F"
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
}
