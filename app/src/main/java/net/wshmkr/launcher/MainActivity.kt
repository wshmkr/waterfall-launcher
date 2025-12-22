package net.wshmkr.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.wshmkr.launcher.repository.WidgetRepository
import net.wshmkr.launcher.ui.AppNavigation
import net.wshmkr.launcher.ui.Screen
import net.wshmkr.launcher.util.WidgetPickerHelper
import net.wshmkr.launcher.viewmodel.WidgetViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var widgetRepository: WidgetRepository

    private lateinit var navController: NavHostController
    private lateinit var widgetViewModel: WidgetViewModel
    private lateinit var widgetPickerHelper: WidgetPickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initWidgetPickerHelper()
        setContent {
            MaterialTheme {
                navController = rememberNavController()
                widgetViewModel = hiltViewModel()
                LaunchedEffect(widgetViewModel) {
                    widgetViewModel.bindWidgetEvent.collect { (widgetId, info) ->
                        widgetPickerHelper.bindOrConfigure(widgetId, info)
                    }
                }

                AppNavigation(
                    navController = navController,
                    widgetViewModel = widgetViewModel
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        widgetRepository.stopListening()
    }

    override fun onResume() {
        super.onResume()
        widgetRepository.startListening()
        returnHome()
    }

    private fun initWidgetPickerHelper() {
        widgetPickerHelper = WidgetPickerHelper(
            activity = this,
            widgetRepository = widgetRepository,
            lifecycleScope = lifecycleScope,
        )
        widgetPickerHelper.registerLaunchers()
    }

    private fun returnHome() {
        if (::navController.isInitialized) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != Screen.Home.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
}
