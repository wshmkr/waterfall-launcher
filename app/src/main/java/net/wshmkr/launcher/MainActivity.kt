package net.wshmkr.launcher

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.wshmkr.launcher.repository.WidgetRepository
import net.wshmkr.launcher.ui.AppNavigation
import net.wshmkr.launcher.ui.Screen
import net.wshmkr.launcher.viewmodel.WidgetViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    
    @Inject
    lateinit var widgetRepository: WidgetRepository
    
    private lateinit var widgetViewModel: WidgetViewModel
    
    private var pendingWidgetId: Int? = null
    
    private lateinit var pickWidgetLauncher: ActivityResultLauncher<Intent>
    private lateinit var bindWidgetLauncher: ActivityResultLauncher<Intent>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize widget picker launcher
        pickWidgetLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            android.util.Log.d("MainActivity", "pickWidgetLauncher result: ${result.resultCode}")
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                android.util.Log.d("MainActivity", "Widget selected with ID: $widgetId")
                
                if (widgetId != -1) {
                    val appWidgetManager = widgetRepository.getAppWidgetManager()
                    val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)
                    android.util.Log.d("MainActivity", "Widget info: ${appWidgetInfo?.provider}")
                    
                    if (appWidgetInfo != null) {
                        // Check if we need to configure the widget
                        if (appWidgetInfo.configure != null) {
                            android.util.Log.d("MainActivity", "Widget needs configuration")
                            // Widget needs configuration
                            val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                                component = appWidgetInfo.configure
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            }
                            pendingWidgetId = widgetId
                            bindWidgetLauncher.launch(configIntent)
                        } else {
                            android.util.Log.d("MainActivity", "Widget doesn't need configuration, adding directly")
                            // Widget doesn't need configuration, add it directly
                            saveWidget(widgetId, appWidgetInfo)
                        }
                    } else {
                        android.util.Log.e("MainActivity", "appWidgetInfo is null!")
                    }
                } else {
                    android.util.Log.e("MainActivity", "Invalid widget ID")
                }
            } else {
                android.util.Log.d("MainActivity", "Widget picker cancelled")
                // Widget picker was cancelled, clean up the allocated widget ID
                pendingWidgetId?.let { id ->
                    widgetRepository.getAppWidgetHost().deleteAppWidgetId(id)
                    pendingWidgetId = null
                }
            }
        }
        
        // Initialize widget bind/configure launcher
        bindWidgetLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            android.util.Log.d("MainActivity", "bindWidgetLauncher result: ${result.resultCode}")
            pendingWidgetId?.let { widgetId ->
                if (result.resultCode == RESULT_OK) {
                    val appWidgetManager = widgetRepository.getAppWidgetManager()
                    val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)
                    
                    if (appWidgetInfo != null) {
                        android.util.Log.d("MainActivity", "Widget bound/configured, saving directly")
                        // Widget is already bound after permission granted, save directly
                        saveWidgetDirectly(widgetId, appWidgetInfo)
                    }
                } else {
                    android.util.Log.d("MainActivity", "Binding/Configuration cancelled, cleaning up")
                    // Configuration was cancelled, clean up
                    widgetRepository.getAppWidgetHost().deleteAppWidgetId(widgetId)
                }
                pendingWidgetId = null
            }
        }
        
        setContent {
            android.util.Log.d("MainActivity", "setContent composing")
            MaterialTheme {
                navController = rememberNavController()
                widgetViewModel = hiltViewModel<WidgetViewModel>()
                android.util.Log.d("MainActivity", "MainActivity using ViewModel: ${widgetViewModel.hashCode()}")
                
                // Set callback for widget picker
                widgetViewModel.onPickWidget = { widgetId ->
                    android.util.Log.d("MainActivity", "onPickWidget callback invoked with widgetId: $widgetId")
                    launchWidgetPicker(widgetId)
                }
                
                // Listen for widget picker events from the ViewModel
                LaunchedEffect("pickWidget") {
                    android.util.Log.d("MainActivity", "Started listening for pickWidgetEvent")
                    widgetViewModel.pickWidgetEvent.collect { widgetId ->
                        android.util.Log.d("MainActivity", "Received pickWidgetEvent with widgetId: $widgetId")
                        launchWidgetPicker(widgetId)
                    }
                }
                
                // Listen for widget bind events from the ViewModel
                LaunchedEffect("bindWidget") {
                    android.util.Log.d("MainActivity", "Started listening for bindWidgetEvent")
                    widgetViewModel.bindWidgetEvent.collect { (widgetId, appWidgetInfo) ->
                        android.util.Log.d("MainActivity", "Received bindWidgetEvent")
                        requestBindWidget(widgetId, appWidgetInfo)
                    }
                }
                
                AppNavigation(navController = navController)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Restart widget host listening
        widgetRepository.startListening()
        
        if (::navController.isInitialized) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != Screen.Home.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Stop widget host listening to save resources
        widgetRepository.stopListening()
    }
    
    /**
     * Public method to request widget picker from composables
     */
    fun requestWidgetPicker() {
        android.util.Log.d("MainActivity", "requestWidgetPicker called")
        val widgetId = widgetRepository.getAppWidgetHost().allocateAppWidgetId()
        android.util.Log.d("MainActivity", "Allocated widgetId: $widgetId")
        launchWidgetPicker(widgetId)
    }
    
    /**
     * Save widget to repository - first binds it if needed
     */
    private fun saveWidget(widgetId: Int, appWidgetInfo: android.appwidget.AppWidgetProviderInfo) {
        android.util.Log.d("MainActivity", "saveWidget called for ID: $widgetId")
        
        // First, try to bind the widget if needed
        val hasPermission = widgetRepository.bindAppWidgetIdIfAllowed(
            widgetId,
            appWidgetInfo.provider
        )
        
        android.util.Log.d("MainActivity", "Widget bind permission: $hasPermission")
        
        if (!hasPermission) {
            // Need to request permission
            android.util.Log.d("MainActivity", "Requesting bind permission")
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetInfo.provider)
            }
            pendingWidgetId = widgetId
            bindWidgetLauncher.launch(bindIntent)
            return
        }
        
        // Widget is bound, save it
        saveWidgetDirectly(widgetId, appWidgetInfo)
    }
    
    /**
     * Save widget directly without permission checks (widget is already bound)
     */
    private fun saveWidgetDirectly(widgetId: Int, appWidgetInfo: android.appwidget.AppWidgetProviderInfo) {
        android.util.Log.d("MainActivity", "saveWidgetDirectly called for ID: $widgetId")
        lifecycleScope.launch {
            try {
                val packageManager = widgetRepository.getPackageManager()
                val label = try {
                    appWidgetInfo.loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    appWidgetInfo.provider.className
                }
                
                val widgetInfo = net.wshmkr.launcher.model.WidgetInfo(
                    widgetId = widgetId,
                    providerName = appWidgetInfo.provider.flattenToString(),
                    minWidth = appWidgetInfo.minWidth,
                    minHeight = appWidgetInfo.minHeight,
                    label = label
                )
                
                android.util.Log.d("MainActivity", "Saving widget info: $widgetInfo")
                widgetRepository.addWidget(widgetInfo)
                android.util.Log.d("MainActivity", "Widget saved successfully")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error saving widget", e)
            }
        }
    }
    
    private fun launchWidgetPicker(widgetId: Int) {
        pendingWidgetId = widgetId
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        pickWidgetLauncher.launch(pickIntent)
    }
    
    private fun requestBindWidget(widgetId: Int, appWidgetInfo: android.appwidget.AppWidgetProviderInfo) {
        val appWidgetManager = widgetRepository.getAppWidgetManager()
        
        // Try to bind the widget
        val hasPermission = widgetRepository.bindAppWidgetIdIfAllowed(
            widgetId,
            appWidgetInfo.provider
        )
        
        if (hasPermission) {
            // Permission granted, widget is bound
            widgetViewModel.onWidgetSelected(widgetId, appWidgetInfo)
        } else {
            // Need to request permission
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetInfo.provider)
            }
            pendingWidgetId = widgetId
            bindWidgetLauncher.launch(bindIntent)
        }
    }
}
