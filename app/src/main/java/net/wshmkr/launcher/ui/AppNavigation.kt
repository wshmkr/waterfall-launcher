package net.wshmkr.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import net.wshmkr.launcher.ui.feature.home.HomeScreen
import net.wshmkr.launcher.ui.feature.settings.SettingsScreen
import net.wshmkr.launcher.ui.feature.widgets.WidgetAppList
import net.wshmkr.launcher.viewmodel.WidgetViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object WidgetList : Screen("widgets")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    widgetViewModel: WidgetViewModel = hiltViewModel(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    navController = navController
                )
            }
            composable(Screen.WidgetList.route) {
                WidgetAppList(
                    viewModel = widgetViewModel,
                    onDismiss = { navController.popBackStack() },
                    onWidgetSelected = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
