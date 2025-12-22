package net.wshmkr.launcher.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import net.wshmkr.launcher.ui.feature.home.HomeScreen
import net.wshmkr.launcher.ui.feature.settings.SettingsScreen
import net.wshmkr.launcher.ui.feature.widgets.WidgetsScreen
import net.wshmkr.launcher.viewmodel.WidgetViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object WidgetList : Screen("widgets")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    widgetViewModel: WidgetViewModel
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
            composable(
                route = Screen.Home.route,
                enterTransition = { fadeIn(animationSpec = tween(500)) },
                exitTransition = { fadeOut(animationSpec = tween(500)) }
            ) {
                HomeScreen(
                    navController = navController
                )
            }
            composable(
                route = Screen.Settings.route,
                enterTransition = { fadeIn(animationSpec = tween(500)) },
                exitTransition = { fadeOut(animationSpec = tween(500)) }
            ) {
                SettingsScreen(
                    navController = navController
                )
            }
            composable(
                route = Screen.WidgetList.route,
                enterTransition = { fadeIn(animationSpec = tween(500)) },
                exitTransition = { fadeOut(animationSpec = tween(500)) }
            ) {
                WidgetsScreen(
                    navController = navController,
                    viewModel = widgetViewModel
                )
            }
        }
    }
}
