package net.wshmkr.launcher.ui.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import net.wshmkr.launcher.ui.Screen
import net.wshmkr.launcher.ui.common.calculateCenteredContentTopPadding
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.ui.common.components.MenuOptionSwitch
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
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            val settings = viewModel.homeWidgetSettings

            Spacer(modifier = Modifier.height(calculateCenteredContentTopPadding()))

            Text("Permissions", color = Color.White)

            HorizontalDivider()

            Text("Home Screen", color = Color.White)

            MenuOption(
                icon = WallpaperIcon(),
                text = "Change wallpaper",
                color = Color.White,
                onClick = {
                    imagePickerLauncher.launch(arrayOf("image/*"))
                }
            )

            MenuOption(
                icon = ScheduleIcon(),
                text = "Clock",
                color = Color.White,
                onClick = { viewModel.setShowClock(!settings.showClock) },
                endContent = {
                    Switch(
                        modifier = Modifier.scale(0.8f),
                        checked = true,
                        onCheckedChange = {},
                    )
                }
            )

            MenuOption(
                text = "24-Hour",
                indent = 1,
                color = Color.White,
                onClick = { /* TODO hook up 24h setting */ },
                endContent = {
                    MenuOptionSwitch(
                        checked = settings.showClock,
                        onCheckedChange = { viewModel.setShowClock(it) }
                    )
                }
            )

            MenuOption(
                text = "Time Zone",
                indent = 1,
                color = Color.White,
                onClick = { /* TODO hook up time zone setting */ },
            )

            HorizontalDivider()

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

            HorizontalDivider()

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
                text = "Temperature degrees?",
                color = Color.White,
                onClick = { viewModel.setShowWeather(!settings.showWeather) },
            )

            MenuOption(
                text = "Location",
                color = Color.White,
                onClick = { viewModel.setShowWeather(!settings.showWeather) },
            )

            HorizontalDivider()

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

            HorizontalDivider()

            MenuOption(
                icon = WidgetsIcon(),
                text = "Manage widgets",
                color = Color.White,
                onClick = {
                    navController.navigate(Screen.WidgetList.route)
                }
            )
        }
    }
}
