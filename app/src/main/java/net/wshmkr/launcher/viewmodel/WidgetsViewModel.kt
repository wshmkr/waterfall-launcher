package net.wshmkr.launcher.viewmodel

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.model.WidgetProviderAppInfo
import net.wshmkr.launcher.repository.WidgetRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class WidgetViewModel @Inject constructor(
    private val widgetRepository: WidgetRepository,
    private val userSettingsDataSource: UserSettingsDataSource,
) : ViewModel() {

    var widgetIds by mutableStateOf<List<Int>>(emptyList())
        private set

    var widgetAppListItems by mutableStateOf<List<WidgetAppListItem>>(emptyList())
        private set

    var managedWidgets by mutableStateOf<List<ManagedWidget>>(emptyList())
        private set

    var backgroundUri by mutableStateOf<String?>(null)
        private set

    val alphabetLetters by derivedStateOf {
        widgetAppListItems
            .filterIsInstance<WidgetAppListItem.SectionHeader>()
            .map { it.letter }
    }

    var activeLetter by mutableStateOf<String?>(null)
        private set

    private val _bindWidgetEvent = MutableSharedFlow<Pair<Int, AppWidgetProviderInfo>>(replay = 1, extraBufferCapacity = 1)
    val bindWidgetEvent = _bindWidgetEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            widgetRepository.loadWidgets()
            widgetRepository.widgetIds.collect {
                widgetIds = it
                refreshManagedWidgets(it)
            }
        }

        viewModelScope.launch {
            loadWidgetProviders()
        }

        viewModelScope.launch {
            backgroundUri = userSettingsDataSource.getBackgroundUri()
        }
    }

    fun scrollToLetter(letter: String) {
        activeLetter = letter
    }

    fun getScrollPosition(letter: String): Int? {
        val index = widgetAppListItems.indexOfFirst {
            it is WidgetAppListItem.SectionHeader && it.letter == letter
        }
        return if (index >= 0) index else null
    }

    fun deselectLetter() {
        activeLetter = null
    }

    fun getAlpha(letter: String): Float {
        return if (activeLetter == null || activeLetter == letter) 1f else 0.2f
    }

    fun removeWidget(widgetId: Int) {
        viewModelScope.launch {
            widgetRepository.removeWidget(widgetId)
        }
    }

    fun getWidgetRepository(): WidgetRepository = widgetRepository

    fun refreshBackground() {
        viewModelScope.launch {
            backgroundUri = userSettingsDataSource.getBackgroundUri()
        }
    }

    private suspend fun loadWidgetProviders() {
        val items = withContext(Dispatchers.IO) {
            val providers = widgetRepository.getWidgetProviderApps()
            buildWidgetAppListItems(providers)
        }
        widgetAppListItems = items
    }

    private suspend fun refreshManagedWidgets(ids: List<Int>) {
        val manager = widgetRepository.appWidgetManager
        val pm = widgetRepository.packageManager

        val items = withContext(Dispatchers.IO) {
            ids.mapNotNull { widgetId ->
                val info = manager.getAppWidgetInfo(widgetId) ?: return@mapNotNull null
                val appInfo = runCatching { pm.getApplicationInfo(info.provider.packageName, 0) }
                    .getOrNull()
                val appName = appInfo?.loadLabel(pm)?.toString() ?: info.provider.packageName
                val widgetName = runCatching { info.loadLabel(pm)?.toString() }
                    .getOrNull() ?: info.provider.className
                val icon = appInfo?.loadIcon(pm)
                ManagedWidget(
                    widgetId = widgetId,
                    widgetName = widgetName,
                    appName = appName,
                    appIcon = icon
                )
            }
        }

        managedWidgets = items
    }

    private fun buildWidgetAppListItems(providers: List<WidgetProviderAppInfo>): List<WidgetAppListItem> {
        if (providers.isEmpty()) return emptyList()

        val items = mutableListOf<WidgetAppListItem>()
        var currentLetter: String? = null

        providers.forEach { provider ->
            val letter = provider.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            if (letter != currentLetter) {
                currentLetter = letter
                items.add(WidgetAppListItem.SectionHeader(letter))
            }
            val widgetOptions = provider.widgets.map { widgetInfo ->
                val label = runCatching {
                    widgetInfo.loadLabel(widgetRepository.packageManager)?.toString()
                }.getOrNull() ?: widgetInfo.provider.className
                WidgetOption(info = widgetInfo, label = label)
            }
            items.add(
                WidgetAppListItem.Provider(
                    packageName = provider.packageName,
                    label = provider.label,
                    icon = provider.icon,
                    widgetCount = provider.widgets.size,
                    widgets = widgetOptions
                )
            )
        }

        return items
    }

    fun onWidgetOptionSelected(option: WidgetOption) {
        viewModelScope.launch {
            try {
                val widgetId = widgetRepository.allocateWidgetId()
                _bindWidgetEvent.emit(widgetId to option.info)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        widgetRepository.stopListening()
    }
}

sealed class WidgetAppListItem {
    data class SectionHeader(val letter: String) : WidgetAppListItem()
    data class Provider(
        val packageName: String,
        val label: String,
        val icon: Drawable?,
        val widgetCount: Int,
        val widgets: List<WidgetOption>
    ) : WidgetAppListItem()
}

data class WidgetOption(
    val info: AppWidgetProviderInfo,
    val label: String
)

data class ManagedWidget(
    val widgetId: Int,
    val widgetName: String,
    val appName: String,
    val appIcon: Drawable?
)
