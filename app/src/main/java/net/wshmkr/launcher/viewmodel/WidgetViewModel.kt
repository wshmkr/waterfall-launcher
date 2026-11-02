package net.wshmkr.launcher.viewmodel

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.wshmkr.launcher.datastore.WidgetDataSource
import net.wshmkr.launcher.model.WidgetProviderAppInfo
import net.wshmkr.launcher.model.sectionLetter
import net.wshmkr.launcher.repository.WidgetRepository
import net.wshmkr.launcher.util.movedItem
import javax.inject.Inject

private const val TAG = "WidgetViewModel"

@HiltViewModel
class WidgetViewModel @Inject constructor(
    private val widgetRepository: WidgetRepository,
) : ViewModel() {

    var widgetIds by mutableStateOf<ImmutableList<Int>>(persistentListOf())
        private set

    var widgetAppListItems by mutableStateOf<ImmutableList<WidgetAppListItem>>(persistentListOf())
        private set

    var managedWidgets by mutableStateOf<ImmutableList<ManagedWidget>>(persistentListOf())
        private set

    val alphabetLetters: ImmutableList<String> by derivedStateOf {
        widgetAppListItems
            .filterIsInstance<WidgetAppListItem.SectionHeader>()
            .map { it.letter }
            .toImmutableList()
    }

    var activeLetter by mutableStateOf<String?>(null)
        private set

    // Read once as the pager seed; restored before the first widgetIds emission.
    var initialPageIndex = 0
        private set

    private var initialPageRestored = false

    var stackHeightDp by mutableIntStateOf(WidgetDataSource.DEFAULT_STACK_HEIGHT_DP)
        private set

    private var persistedStackHeightDp = WidgetDataSource.DEFAULT_STACK_HEIGHT_DP

    private val _bindWidgetEvent = Channel<Pair<Int, AppWidgetProviderInfo>>(Channel.BUFFERED)
    val bindWidgetEvent = _bindWidgetEvent.receiveAsFlow()

    private var widgetProvidersJob: Job? = null

    init {
        viewModelScope.launch {
            // Before the first widget renders, so the stack never draws at the default and jumps.
            stackHeightDp = widgetRepository.getStackHeightDp()
            persistedStackHeightDp = stackHeightDp
            val lastWidgetId = widgetRepository.getLastPageWidgetId()
            widgetRepository.loadWidgets()
            widgetRepository.widgetIds.collect {
                if (!initialPageRestored) {
                    initialPageIndex = it.indexOf(lastWidgetId).coerceAtLeast(0)
                    initialPageRestored = true
                }
                val ids = it.toImmutableList()
                if (ids == widgetIds) return@collect
                widgetIds = ids
                refreshManagedWidgets(it)
            }
        }

        refreshWidgetProviders()
    }

    fun refreshWidgetProviders() {
        widgetProvidersJob?.cancel()
        widgetProvidersJob = viewModelScope.launch {
            widgetAppListItems = withContext(Dispatchers.IO) {
                buildWidgetAppListItems(widgetRepository.getWidgetProviderApps())
            }
        }
    }

    fun updateCurrentPage(widgetId: Int) {
        viewModelScope.launch {
            widgetRepository.setLastPageWidgetId(widgetId)
        }
    }

    fun previewStackHeight(dp: Int) {
        stackHeightDp = dp.coerceIn(
            WidgetDataSource.MIN_STACK_HEIGHT_DP,
            WidgetDataSource.MAX_STACK_HEIGHT_DP,
        )
    }

    fun commitStackHeight() {
        val current = stackHeightDp
        if (current == persistedStackHeightDp) return
        persistedStackHeightDp = current
        viewModelScope.launch {
            widgetRepository.setStackHeightDp(current)
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

    fun removeWidget(widgetId: Int) {
        viewModelScope.launch {
            widgetRepository.removeWidget(widgetId)
        }
    }

    // The drag needs the move applied within the same frame; widgetIds intentionally waits for commit.
    fun moveWidget(fromWidgetId: Int, toWidgetId: Int) {
        val from = managedWidgets.firstOrNull { it.widgetId == fromWidgetId } ?: return
        val to = managedWidgets.firstOrNull { it.widgetId == toWidgetId } ?: return
        managedWidgets = managedWidgets.movedItem(from, to)?.toImmutableList() ?: return
    }

    fun commitWidgetOrder() {
        val orderedIds = managedWidgets.map { it.widgetId }
        // managedWidgets lags widgetIds while rows for newly added ids load.
        val merged = (orderedIds + widgetIds.filterNot { it in orderedIds }).toImmutableList()
        if (merged == widgetRepository.widgetIds.value) return
        widgetIds = merged
        viewModelScope.launch {
            widgetRepository.commitWidgetOrder(merged)
        }
    }

    fun createWidgetView(context: Context, widgetId: Int): AppWidgetHostView? {
        return try {
            widgetRepository.createWidgetView(context, widgetId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create widget view for id=$widgetId", e)
            null
        }
    }

    private suspend fun refreshManagedWidgets(ids: List<Int>) {
        val loaded = managedWidgets
            .filter { it.providerInfo != null }
            .associateBy { it.widgetId }
        managedWidgets = if (ids.all { it in loaded }) {
            ids.mapNotNull { loaded[it] }.toImmutableList()
        } else {
            withContext(Dispatchers.IO) {
                ids.map { widgetId ->
                    loaded[widgetId] ?: buildManagedWidget(widgetId)
                }.toImmutableList()
            }
        }
    }

    private fun buildManagedWidget(widgetId: Int): ManagedWidget {
        val manager = widgetRepository.appWidgetManager
        val pm = widgetRepository.packageManager
        val info = runCatching { manager.getAppWidgetInfo(widgetId) }.getOrNull()
            ?: return ManagedWidget(
                widgetId = widgetId,
                widgetName = "Unavailable widget",
                appName = "Provider not found",
                providerInfo = null,
            )
        val appName = runCatching { pm.getApplicationInfo(info.provider.packageName, 0) }
            .getOrNull()?.loadLabel(pm)?.toString() ?: info.provider.packageName
        val widgetName = runCatching { info.loadLabel(pm)?.toString() }
            .getOrNull() ?: info.provider.className
        return ManagedWidget(
            widgetId = widgetId,
            widgetName = widgetName,
            appName = appName,
            providerInfo = info
        )
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
                Log.w(TAG, "Failed to allocate widget id", e)
            }
        }
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
    val providerInfo: AppWidgetProviderInfo?
)
