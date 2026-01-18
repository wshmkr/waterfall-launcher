package net.wshmkr.launcher.ui.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import net.wshmkr.launcher.ui.Screen
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.ui.common.components.MenuOptionSwitch
import net.wshmkr.launcher.ui.common.icons.CalendarTodayIcon
import net.wshmkr.launcher.ui.common.icons.MusicVideoIcon
import net.wshmkr.launcher.ui.common.icons.PartlyCloudyDayIcon
import net.wshmkr.launcher.ui.common.icons.ScheduleIcon
import net.wshmkr.launcher.ui.common.icons.SettingsIcon
import net.wshmkr.launcher.ui.common.icons.WidgetsIcon
import net.wshmkr.launcher.viewmodel.HomeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeOptionsMenu(
    navController: NavController,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val settings = viewModel.homeWidgetSettings

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 18.dp)
        ) {
            MenuOption(
                icon = ScheduleIcon(),
                text = "Clock",
                onClick = { viewModel.setShowClock(!settings.showClock) },
                endContent = {
                    MenuOptionSwitch(
                        checked = settings.showClock,
                        onCheckedChange = { viewModel.setShowClock(it) }
                    )
                }
            )

            MenuOption(
                icon = CalendarTodayIcon(),
                text = "Calendar",
                onClick = { viewModel.setShowCalendar(!settings.showCalendar) },
                endContent = {
                    MenuOptionSwitch(
                        checked = settings.showCalendar,
                        onCheckedChange = { viewModel.setShowCalendar(it) }
                    )
                }
            )

            MenuOption(
                icon = PartlyCloudyDayIcon(),
                text = "Weather",
                onClick = { viewModel.setShowWeather(!settings.showWeather) },
                endContent = {
                    MenuOptionSwitch(
                        checked = settings.showWeather,
                        onCheckedChange = { viewModel.setShowWeather(it) }
                    )
                }
            )

            MenuOption(
                icon = MusicVideoIcon(),
                text = "Media controls",
                onClick = { viewModel.setShowMedia(!settings.showMediaControls) },
                endContent = {
                    MenuOptionSwitch(
                        checked = settings.showMediaControls,
                        onCheckedChange = { viewModel.setShowMedia(it) }
                    )
                }
            )

            MenuOption(
                icon = WidgetsIcon(),
                text = "Manage widgets",
                onClick = {
                    onDismiss()
                    navController.navigate(Screen.WidgetList.route)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
            )

            MenuOption(
                icon = SettingsIcon(),
                text = "Waterfall launcher settings",
                onClick = {
                    onDismiss()
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
    }
}