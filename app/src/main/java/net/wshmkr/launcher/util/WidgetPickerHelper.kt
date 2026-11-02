package net.wshmkr.launcher.util

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.launch
import net.wshmkr.launcher.repository.WidgetRepository

private const val TAG = "WidgetPickerHelper"
private const val SAVED_STATE_KEY = "widget_picker_pending"
private const val KEY_WIDGET_ID = "widget_id"
private const val KEY_WIDGET_INFO = "widget_info"
private const val KEY_REQUEST = "request"
private const val REQUEST_CONFIGURE_WIDGET = 1

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
            ActivityResultContracts.StartActivityForResult()
        ) { result -> handleBindOrConfigureResult(result.resultCode) }
        // The bind/configure activity can outlive this activity instance across a config change.
        restorePendingWidget()
        activity.savedStateRegistry.registerSavedStateProvider(SAVED_STATE_KEY) { savePendingWidget() }
    }

    fun bindOrConfigure(widgetId: Int, info: AppWidgetProviderInfo) {
        // A flow still pending here never delivered its result; release its id.
        clearPendingWidget(deleteFromHost = true)
        pendingWidgetId = widgetId
        pendingWidgetInfo = info
        runOrAbandon {
            if (widgetRepository.bindAppWidgetIdIfAllowed(widgetId, info.provider)) {
                configureOrSave(widgetId, info)
            } else {
                launchBindPermissionRequest(widgetId, info)
            }
        }
    }

    // AppWidgetHost delivers the configure result only through Activity.onActivityResult.
    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == REQUEST_CONFIGURE_WIDGET) handleBindOrConfigureResult(resultCode)
    }

    private fun handleBindOrConfigureResult(resultCode: Int) {
        val widgetId = pendingWidgetId ?: return
        val info = pendingWidgetInfo
        if (resultCode != Activity.RESULT_OK || info == null) {
            clearPendingWidget(deleteFromHost = true)
            return
        }
        runOrAbandon {
            when (pendingRequest) {
                PendingRequest.BIND -> configureOrSave(widgetId, info)
                PendingRequest.CONFIG -> saveWidget(widgetId)
                PendingRequest.NONE -> clearPendingWidget(deleteFromHost = true)
            }
        }
    }

    private fun configureOrSave(widgetId: Int, info: AppWidgetProviderInfo) {
        if (info.configure != null) {
            launchConfiguration(widgetId)
        } else {
            saveWidget(widgetId)
        }
    }

    private fun launchBindPermissionRequest(widgetId: Int, info: AppWidgetProviderInfo) {
        pendingRequest = PendingRequest.BIND
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
        }
        widgetResultLauncher.launch(intent)
    }

    private fun launchConfiguration(widgetId: Int) {
        pendingRequest = PendingRequest.CONFIG
        widgetRepository.startConfigureActivityForResult(activity, widgetId, REQUEST_CONFIGURE_WIDGET)
    }

    private fun saveWidget(widgetId: Int) {
        clearPendingWidget()
        lifecycleScope.launch {
            widgetRepository.addWidget(widgetId)
        }
    }

    private inline fun runOrAbandon(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to bind or configure widget id=$pendingWidgetId", e)
            clearPendingWidget(deleteFromHost = true)
        }
    }

    private fun clearPendingWidget(deleteFromHost: Boolean = false) {
        val widgetId = pendingWidgetId
        pendingWidgetId = null
        pendingWidgetInfo = null
        pendingRequest = PendingRequest.NONE
        if (deleteFromHost && widgetId != null) {
            widgetRepository.deleteWidgetId(widgetId)
        }
    }

    private fun savePendingWidget(): Bundle = Bundle().apply {
        val widgetId = pendingWidgetId ?: return@apply
        putInt(KEY_WIDGET_ID, widgetId)
        putParcelable(KEY_WIDGET_INFO, pendingWidgetInfo)
        putString(KEY_REQUEST, pendingRequest.name)
    }

    private fun restorePendingWidget() {
        val state = activity.savedStateRegistry.consumeRestoredStateForKey(SAVED_STATE_KEY) ?: return
        if (!state.containsKey(KEY_WIDGET_ID)) return
        pendingWidgetId = state.getInt(KEY_WIDGET_ID)
        pendingWidgetInfo = state.getParcelable(KEY_WIDGET_INFO, AppWidgetProviderInfo::class.java)
        pendingRequest = state.getString(KEY_REQUEST)
            ?.let { runCatching { PendingRequest.valueOf(it) }.getOrNull() }
            ?: PendingRequest.NONE
    }
}
