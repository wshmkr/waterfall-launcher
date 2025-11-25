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
    private val onWidgetSaved: ((Int, AppWidgetProviderInfo) -> Unit)? = null
) {
    private lateinit var pickWidgetLauncher: ActivityResultLauncher<Intent>
    private lateinit var bindWidgetLauncher: ActivityResultLauncher<Intent>

    private var pendingWidgetId: Int? = null

    private val appWidgetManager: AppWidgetManager
        get() = widgetRepository.appWidgetManager


    fun registerLaunchers() {
        pickWidgetLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handlePickerResult
        )
        bindWidgetLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleBindOrConfigureResult
        )
    }

    fun launchPicker(widgetId: Int) {
        pendingWidgetId = widgetId
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        pickWidgetLauncher.launch(intent)
    }

    fun bindOrConfigure(widgetId: Int, info: AppWidgetProviderInfo) {
        if (!tryBind(widgetId, info.provider)) {
            launchBindPermissionRequest(widgetId, info.provider)
            return
        }

        if (info.configure != null) {
            launchConfiguration(widgetId, info)
        } else {
            onWidgetSaved?.invoke(widgetId, info)
        }
    }

    private fun handlePickerResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) {
            clearPendingWidget(deleteFromHost = true)
            return
        }

        val widgetId = result.extractWidgetId() ?: return
        val info = appWidgetManager.getAppWidgetInfo(widgetId) ?: return

        if (info.configure != null) {
            launchConfiguration(widgetId, info)
        } else {
            bindOrSave(widgetId, info)
        }
    }

    private fun handleBindOrConfigureResult(result: ActivityResult) {
        val widgetId = pendingWidgetId ?: return

        if (result.resultCode == Activity.RESULT_OK) {
            appWidgetManager.getAppWidgetInfo(widgetId)?.let { info ->
                saveWidget(widgetId, info)
            }
            clearPendingWidget()
        } else {
            clearPendingWidget(deleteFromHost = true)
        }
    }

    private fun bindOrSave(widgetId: Int, info: AppWidgetProviderInfo) {
        if (tryBind(widgetId, info.provider)) {
            saveWidget(widgetId, info)
        } else {
            launchBindPermissionRequest(widgetId, info.provider)
        }
    }

    private fun tryBind(widgetId: Int, provider: ComponentName): Boolean {
        return widgetRepository.bindAppWidgetIdIfAllowed(widgetId, provider)
    }

    private fun launchBindPermissionRequest(widgetId: Int, provider: ComponentName) {
        pendingWidgetId = widgetId
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        }
        bindWidgetLauncher.launch(intent)
    }

    private fun launchConfiguration(widgetId: Int, info: AppWidgetProviderInfo) {
        pendingWidgetId = widgetId
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = info.configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        bindWidgetLauncher.launch(intent)
    }

    private fun saveWidget(widgetId: Int, info: AppWidgetProviderInfo) {
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
    }

    private fun ActivityResult.extractWidgetId(): Int? {
        val id = data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        return id.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }
    }
}
