package net.wshmkr.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.model.PaletteStyle
import net.wshmkr.launcher.repository.WidgetRepository
import net.wshmkr.launcher.ui.AppNavigation
import net.wshmkr.launcher.ui.theme.WaterfallLauncherTheme
import net.wshmkr.launcher.util.WidgetPickerHelper
import net.wshmkr.launcher.viewmodel.HomeViewModel
import net.wshmkr.launcher.viewmodel.WidgetViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var widgetRepository: WidgetRepository

    @Inject
    lateinit var userSettingsDataSource: UserSettingsDataSource

    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var widgetPickerHelper: WidgetPickerHelper
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                homeViewModel.navigateToFavorites()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initWidgetPickerHelper()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        setContent {
            // Drawing nothing until the stored palette loads avoids a frame in the default palette.
            val paletteStyle by userSettingsDataSource.paletteStyle
                .collectAsStateWithLifecycle<PaletteStyle?>(initialValue = null)
            val loadedPaletteStyle = paletteStyle ?: return@setContent
            WaterfallLauncherTheme(paletteStyle = loadedPaletteStyle) {
                val navController = rememberNavController()
                val widgetViewModel: WidgetViewModel = hiltViewModel()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeViewModel.navigateToFavorites()
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
        unregisterReceiver(screenOffReceiver)
    }

    private fun initWidgetPickerHelper() {
        widgetPickerHelper = WidgetPickerHelper(
            activity = this,
            widgetRepository = widgetRepository,
            lifecycleScope = lifecycleScope,
        )
        widgetPickerHelper.registerLaunchers()
    }
}
