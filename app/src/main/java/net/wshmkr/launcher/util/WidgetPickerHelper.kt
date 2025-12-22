package net.wshmkr.launcher.util

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.launch
import net.wshmkr.launcher.repository.WidgetRepository


class WidgetPickerHelper(
    private val activity: ComponentActivity,
    private val widgetRepository: WidgetRepository,
    private val lifecycleScope: LifecycleCoroutineScope,
) {
    private lateinit var widgetResultLauncher: ActivityResultLauncher<Intent>

    private var pendingWidgetId: Int? = null
    private var pendingWidgetInfo: AppWidgetProviderInfo? = null
    private var pendingRequest: PendingRequest = PendingRequest.NONE

    private enum class PendingRequest { NONE, BIND, CONFIG }

    fun registerLaunchers() {
        widgetResultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleBindOrConfigureResult
        )
    }

    fun bindOrConfigure(widgetId: Int, info: AppWidgetProviderInfo) {
        pendingWidgetInfo = info
        if (!tryBind(widgetId, info.provider)) {
            launchBindPermissionRequest(widgetId, info.provider)
            return
        }

        if (info.configure != null) {
            launchConfiguration(widgetId, info)
        } else {
            saveWidget(widgetId)
            pendingWidgetInfo = null
        }
    }

    private fun handleBindOrConfigureResult(result: ActivityResult) {
        val widgetId = pendingWidgetId ?: return

        if (result.resultCode == Activity.RESULT_OK) {
            when (pendingRequest) {
                PendingRequest.BIND -> {
                    val info = pendingWidgetInfo
                    if (info == null) {
                        clearPendingWidget(deleteFromHost = true)
                        return
                    }
                    if (info.configure != null) {
                        launchConfiguration(widgetId, info)
                    } else {
                        saveWidget(widgetId)
                        clearPendingWidget()
                    }
                }
                PendingRequest.CONFIG -> {
                    saveWidget(widgetId)
                    clearPendingWidget()
                }
                PendingRequest.NONE -> clearPendingWidget()
            }
        } else {
            clearPendingWidget(deleteFromHost = true)
        }
    }

    private fun tryBind(widgetId: Int, provider: ComponentName): Boolean {
        return widgetRepository.bindAppWidgetIdIfAllowed(widgetId, provider)
    }

    private fun launchBindPermissionRequest(widgetId: Int, provider: ComponentName) {
        pendingWidgetId = widgetId
        pendingRequest = PendingRequest.BIND
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        }
        widgetResultLauncher.launch(intent)
    }

    private fun launchConfiguration(widgetId: Int, info: AppWidgetProviderInfo) {
        pendingWidgetId = widgetId
        pendingRequest = PendingRequest.CONFIG
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = info.configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        widgetResultLauncher.launch(intent)
    }

    private fun saveWidget(widgetId: Int) {
        lifecycleScope.launch {
            widgetRepository.addWidget(widgetId)
        }
    }

    private fun clearPendingWidget(deleteFromHost: Boolean = false) {
        pendingWidgetId?.let { id ->
            if (deleteFromHost) {
                widgetRepository.appWidgetHost.deleteAppWidgetId(id)
            }
        }
        pendingWidgetId = null
        pendingWidgetInfo = null
        pendingRequest = PendingRequest.NONE
    }
}
