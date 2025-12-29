package net.wshmkr.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.wshmkr.launcher.repository.WidgetRepository
import net.wshmkr.launcher.ui.AppNavigation
import net.wshmkr.launcher.util.WidgetPickerHelper
import net.wshmkr.launcher.viewmodel.HomeViewModel
import net.wshmkr.launcher.viewmodel.WidgetViewModel
import javax.inject.Inject
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var widgetRepository: WidgetRepository

    private lateinit var navController: NavHostController
    private lateinit var widgetViewModel: WidgetViewModel
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var widgetPickerHelper: WidgetPickerHelper
    private var screenOffReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initWidgetPickerHelper()
        registerScreenOffReceiver()
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
                    homeViewModel = homeViewModel,
                    widgetViewModel = widgetViewModel
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        widgetRepository.stopListening()
    }

    override fun onStop() {
        super.onStop()
        homeViewModel.onLauncherStopped()
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.onLauncherResumed()
        widgetRepository.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        screenOffReceiver?.let { unregisterReceiver(it) }
        screenOffReceiver = null
    }

    private fun initWidgetPickerHelper() {
        widgetPickerHelper = WidgetPickerHelper(
            activity = this,
            widgetRepository = widgetRepository,
            lifecycleScope = lifecycleScope,
        )
        widgetPickerHelper.registerLaunchers()
    }

    private fun registerScreenOffReceiver() {
        if (screenOffReceiver != null) return
        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    homeViewModel.navigateToFavorites()
                }
            }
        }
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }
}
