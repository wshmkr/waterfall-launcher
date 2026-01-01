package net.wshmkr.launcher.ui.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import net.wshmkr.launcher.ui.Screen
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.ui.common.icons.CalendarTodayIcon
import net.wshmkr.launcher.ui.common.icons.MusicVideoIcon
import net.wshmkr.launcher.ui.common.icons.PartlyCloudyDayIcon
import net.wshmkr.launcher.ui.common.icons.ScheduleIcon
import net.wshmkr.launcher.ui.common.icons.SettingsIcon
import net.wshmkr.launcher.ui.common.icons.WidgetsIcon


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeOptionsMenu(
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    var clockSwitch by remember { mutableStateOf(false) }
    var calendarSwitch by remember { mutableStateOf(false) }
    var mediaSwitch by remember { mutableStateOf(false) }

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
                onClick = { },
                switch = clockSwitch,
                onToggle = { clockSwitch = !clockSwitch },
            )

            MenuOption(
                icon = CalendarTodayIcon(),
                text = "Calendar",
                onClick = { },
                switch = calendarSwitch,
                onToggle = { calendarSwitch = !calendarSwitch },
            )

            MenuOption(
                icon = PartlyCloudyDayIcon(),
                text = "Weather",
                onClick = { },
                switch = calendarSwitch,
                onToggle = { calendarSwitch = !calendarSwitch },
            )

            MenuOption(
                icon = MusicVideoIcon(),
                text = "Media controls",
                onClick = { },
                switch = mediaSwitch,
                onToggle = { mediaSwitch = !mediaSwitch },
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