package net.wshmkr.launcher.viewmodel

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.model.WidgetProviderAppInfo
import net.wshmkr.launcher.model.sectionLetter
import net.wshmkr.launcher.repository.WidgetRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class WidgetViewModel @Inject constructor(
    private val widgetRepository: WidgetRepository,
    private val userSettingsDataSource: UserSettingsDataSource,
) : ViewModel() {

    var widgetIds by mutableStateOf<ImmutableList<Int>>(persistentListOf())
        private set

    var widgetAppListItems by mutableStateOf<ImmutableList<WidgetAppListItem>>(persistentListOf())
        private set

    var managedWidgets by mutableStateOf<ImmutableList<ManagedWidget>>(persistentListOf())
        private set

    var backgroundUri by mutableStateOf<String?>(null)
        private set

    val alphabetLetters: ImmutableList<String> by derivedStateOf {
        widgetAppListItems
            .filterIsInstance<WidgetAppListItem.SectionHeader>()
            .map { it.letter }
            .toImmutableList()
    }

    var activeLetter by mutableStateOf<String?>(null)
        private set

    private val _bindWidgetEvent = Channel<Pair<Int, AppWidgetProviderInfo>>(Channel.BUFFERED)
    val bindWidgetEvent = _bindWidgetEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            widgetRepository.loadWidgets()
            widgetRepository.widgetIds.collect {
                widgetIds = it.toImmutableList()
                refreshManagedWidgets(it)
            }
        }

        viewModelScope.launch {
            loadWidgetProviders()
        }

        viewModelScope.launch {
            userSettingsDataSource.backgroundUri.collect {
                backgroundUri = it
            }
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

    fun createWidgetView(context: Context, widgetId: Int): AppWidgetHostView? {
        return try {
            val info = widgetRepository.appWidgetManager.getAppWidgetInfo(widgetId) ?: return null
            widgetRepository.appWidgetHost.createView(context, widgetId, info)
        } catch (e: Exception) {
            Log.w("WidgetViewModel", "Failed to create widget view for id=$widgetId", e)
            null
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
                val info = runCatching { manager.getAppWidgetInfo(widgetId) }
                    .getOrNull() ?: return@mapNotNull null
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
            }.toImmutableList()
        }

        managedWidgets = items
    }

    private fun buildWidgetAppListItems(providers: List<WidgetProviderAppInfo>): ImmutableList<WidgetAppListItem> {
        if (providers.isEmpty()) return persistentListOf()

        val items = mutableListOf<WidgetAppListItem>()
        var currentLetter: String? = null

        providers.forEach { provider ->
            val letter = provider.label.sectionLetter
            if (letter != currentLetter) {
                currentLetter = letter
                items.add(WidgetAppListItem.SectionHeader(letter))
            }
            val widgetOptions = provider.widgets.map { widgetInfo ->
                val label = runCatching {
                    widgetInfo.loadLabel(widgetRepository.packageManager)?.toString()
                }.getOrNull() ?: widgetInfo.provider.className
                WidgetOption(info = widgetInfo, label = label)
            }.toImmutableList()
            items.add(
                WidgetAppListItem.Provider(
                    packageName = provider.packageName,
                    label = provider.label,
                    letter = letter,
                    icon = provider.icon,
                    widgetCount = provider.widgets.size,
                    widgets = widgetOptions
                )
            )
        }

        return items.toImmutableList()
    }

    fun onWidgetOptionSelected(option: WidgetOption) {
        viewModelScope.launch {
            try {
                val widgetId = widgetRepository.allocateWidgetId()
                _bindWidgetEvent.send(widgetId to option.info)
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

@Immutable
sealed class WidgetAppListItem {
    data class SectionHeader(val letter: String) : WidgetAppListItem()
    data class Provider(
        val packageName: String,
        val label: String,
        val letter: String,
        val icon: Drawable?,
        val widgetCount: Int,
        val widgets: ImmutableList<WidgetOption>
    ) : WidgetAppListItem()
}

@Immutable
data class WidgetOption(
    val info: AppWidgetProviderInfo,
    val label: String
)

@Immutable
data class ManagedWidget(
    val widgetId: Int,
    val widgetName: String,
    val appName: String,
    val appIcon: Drawable?
)
