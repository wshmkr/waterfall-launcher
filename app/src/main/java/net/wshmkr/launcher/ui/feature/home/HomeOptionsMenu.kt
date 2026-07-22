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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import net.wshmkr.launcher.ui.Screen
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.ui.common.components.ToggleMenuOption
import net.wshmkr.launcher.ui.common.icons.CalendarTodayIcon
import net.wshmkr.launcher.ui.common.icons.MusicVideoIcon
import net.wshmkr.launcher.ui.common.icons.PartlyCloudyDayIcon
import net.wshmkr.launcher.ui.common.icons.ScheduleIcon
import net.wshmkr.launcher.ui.common.icons.SettingsIcon
import net.wshmkr.launcher.ui.common.icons.WidgetsIcon
import net.wshmkr.launcher.viewmodel.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeOptionsMenu(
    navController: NavController,
    onDismiss: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState()
    val settings = settingsViewModel.homeWidgetSettings

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
            ToggleMenuOption(
                icon = ScheduleIcon(),
                text = "Clock",
                checked = settings.showClock,
                onCheckedChange = { settingsViewModel.setShowClock(it) },
            )

            ToggleMenuOption(
                icon = CalendarTodayIcon(),
                text = "Calendar",
                checked = settings.showCalendar,
                onCheckedChange = { settingsViewModel.setShowCalendar(it) },
            )

            ToggleMenuOption(
                icon = PartlyCloudyDayIcon(),
                text = "Weather",
                checked = settings.showWeather,
                onCheckedChange = { settingsViewModel.setShowWeather(it) },
            )

            ToggleMenuOption(
                icon = MusicVideoIcon(),
                text = "Media controls",
                checked = settings.showMediaControls,
                onCheckedChange = { settingsViewModel.setShowMedia(it) },
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
