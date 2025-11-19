package net.wshmkr.launcher.viewmodel

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.wshmkr.launcher.model.WidgetInfo
import net.wshmkr.launcher.repository.WidgetRepository
import javax.inject.Inject

@HiltViewModel
class WidgetViewModel @Inject constructor(
    private val widgetRepository: WidgetRepository
) : ViewModel() {

    var widgets by mutableStateOf<List<WidgetInfo>>(emptyList())
        private set

    private val _pickWidgetEvent = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 1)
    val pickWidgetEvent = _pickWidgetEvent.asSharedFlow()

    private val _bindWidgetEvent = MutableSharedFlow<Pair<Int, AppWidgetProviderInfo>>(replay = 1, extraBufferCapacity = 1)
    val bindWidgetEvent = _bindWidgetEvent.asSharedFlow()

    // Callback for widget picker - more direct approach
    var onPickWidget: ((Int) -> Unit)? = null

    init {
        android.util.Log.d("WidgetViewModel", "ViewModel initialized")
        viewModelScope.launch {
            android.util.Log.d("WidgetViewModel", "Loading widgets from repository")
            widgetRepository.loadWidgets()
            widgetRepository.widgets.collect { loadedWidgets ->
                android.util.Log.d("WidgetViewModel", "Received ${loadedWidgets.size} widgets from repository")
                widgets = loadedWidgets
            }
        }
    }

    fun requestAddWidget() {
        viewModelScope.launch {
            try {
                val widgetId = widgetRepository.allocateWidgetId()
                android.util.Log.d("WidgetViewModel", "Allocated widget ID: $widgetId, emitting event")
                _pickWidgetEvent.emit(widgetId)
                android.util.Log.d("WidgetViewModel", "Event emitted successfully")
                
                // Also call callback directly
                android.util.Log.d("WidgetViewModel", "Calling onPickWidget callback: ${onPickWidget != null}")
                onPickWidget?.invoke(widgetId)
            } catch (e: Exception) {
                android.util.Log.e("WidgetViewModel", "Error requesting widget", e)
            }
        }
    }

    fun onWidgetSelected(widgetId: Int, appWidgetInfo: AppWidgetProviderInfo) {
        viewModelScope.launch {
            val packageManager = widgetRepository.getPackageManager()
            val label = try {
                appWidgetInfo.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                appWidgetInfo.provider.className
            }
            
            val widgetInfo = WidgetInfo(
                widgetId = widgetId,
                providerName = appWidgetInfo.provider.flattenToString(),
                minWidth = appWidgetInfo.minWidth,
                minHeight = appWidgetInfo.minHeight,
                label = label
            )
            widgetRepository.addWidget(widgetInfo)
        }
    }

    fun onWidgetBindRequired(widgetId: Int, appWidgetInfo: AppWidgetProviderInfo) {
        viewModelScope.launch {
            _bindWidgetEvent.emit(Pair(widgetId, appWidgetInfo))
        }
    }

    fun removeWidget(widgetId: Int) {
        viewModelScope.launch {
            widgetRepository.removeWidget(widgetId)
        }
    }

    fun removeAllWidgets() {
        viewModelScope.launch {
            widgetRepository.removeAllWidgets()
        }
    }

    fun getAppWidgetInfo(widgetId: Int): AppWidgetProviderInfo? {
        return widgetRepository.getAppWidgetInfo(widgetId)
    }

    fun getWidgetRepository(): WidgetRepository {
        return widgetRepository
    }

    override fun onCleared() {
        super.onCleared()
        widgetRepository.stopListening()
    }
}

