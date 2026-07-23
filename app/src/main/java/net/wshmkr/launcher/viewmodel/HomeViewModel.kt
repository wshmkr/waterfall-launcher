package net.wshmkr.launcher.viewmodel

import android.os.UserHandle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.AppListItem
import net.wshmkr.launcher.model.HomeWidgetSettings
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.model.TodayEvents
import net.wshmkr.launcher.model.keyFor
import net.wshmkr.launcher.model.sectionLetter
import net.wshmkr.launcher.repository.AppsRepository
import net.wshmkr.launcher.repository.CalendarRepository
import net.wshmkr.launcher.repository.NotificationRepository
import net.wshmkr.launcher.ui.common.components.STAR_SYMBOL
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

const val HOME_SCREEN_APPS = 6

@HiltViewModel
class HomeViewModel @Inject constructor(
    appsRepository: AppsRepository,
    private val notificationRepository: NotificationRepository,
    private val calendarRepository: CalendarRepository,
    private val userSettingsDataSource: UserSettingsDataSource
) : LauncherViewModel(appsRepository) {

    var backgroundUri by mutableStateOf<String?>(null)
        private set

    var homeWidgetSettings by mutableStateOf(HomeWidgetSettings())
        private set

    val allAppsListItems by derivedStateOf {
        buildListItems(appsRepository.allApps.filter { !it.isHidden })
    }

    val alphabetLetters by derivedStateOf {
        buildList {
            add(STAR_SYMBOL)
            val letters = appsRepository.allApps
                .filter { !it.isHidden }
                .map { it.label.sectionLetter }
                .distinct()
                .sorted()
            addAll(letters)
        }
    }

    val favoriteApps by derivedStateOf { buildFavoriteAppsList() }

    val favoritesVisible: StateFlow<Boolean> = snapshotFlow { favoriteApps.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayEvents: StateFlow<TodayEvents> =
        userSettingsDataSource.showCalendarEvents
            .flatMapLatest { enabled ->
                if (enabled) calendarRepository.observeTodayEvents() else flowOf(TodayEvents())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayEvents())

    fun refreshCalendarEvents() {
        calendarRepository.requestRefresh()
    }

    var activeLetter by mutableStateOf<String?>(null)
        private set

    var showingFavorites by mutableStateOf(true)
        private set

    var showSearchOverlay by mutableStateOf(false)

    private var observedStop = false

    private val notificationCountCache = ConcurrentHashMap<String, StateFlow<Int>>()
    private val notificationListCache = ConcurrentHashMap<String, StateFlow<ImmutableList<NotificationInfo>>>()

    init {
        viewModelScope.launch {
            appsRepository.loadInstalledApps()
            appsRepository.updateMostUsedApps()
        }

        viewModelScope.launch {
            var previousProfiles = appsRepository.activeProfiles.value
            appsRepository.activeProfiles
                .drop(1)
                .collectLatest { newProfiles ->
                    val changedProfiles = (newProfiles - previousProfiles) + (previousProfiles - newProfiles)
                    if (changedProfiles.isNotEmpty()) {
                        appsRepository.refreshAppIcons(changedProfiles)
                    }
                    previousProfiles = newProfiles
                }
        }

        viewModelScope.launch {
            userSettingsDataSource.backgroundUri.collect { uri ->
                backgroundUri = uri
            }
        }

        viewModelScope.launch {
            userSettingsDataSource.homeWidgetSettings.collectLatest { settings ->
                homeWidgetSettings = settings
            }
        }

        // Prune per-package notification caches when apps leave the installed set so long-lived
        // sessions with many install/uninstall churns don't grow the cache unboundedly.
        viewModelScope.launch {
            snapshotFlow { appsRepository.allApps.mapTo(HashSet(appsRepository.allApps.size)) { it.key } }
                .collect { liveKeys ->
                    notificationCountCache.keys.retainAll(liveKeys)
                    notificationListCache.keys.retainAll(liveKeys)
                }
        }
    }

    fun scrollToLetter(letter: String) {
        activeLetter = letter
        showingFavorites = letter == STAR_SYMBOL
        observedStop = false
    }

    fun getScrollPosition(letter: String): Int? {
        if (letter == STAR_SYMBOL) return null

        val header = allAppsListItems.find {
            it is AppListItem.SectionHeader && it.letter == letter
        } as? AppListItem.SectionHeader

        return header?.position
    }

    fun deselectLetter() {
        activeLetter = null
    }

    fun navigateToFavorites() {
        activeLetter = null
        showingFavorites = true
        showSearchOverlay = false
        observedStop = false
    }

    fun onLauncherStopped() {
        observedStop = true
        appsRepository.releaseMostUsedPublish()
        viewModelScope.launch {
            appsRepository.flushUsage()
        }
    }

    fun onLauncherResumed() {
        if (observedStop) {
            navigateToFavorites()
        }
        appsRepository.updateMostUsedApps()
    }

    fun getAlpha(letter: String): Float {
        return if (activeLetter == null || letter == activeLetter) 1f else 0.2f
    }

    // Per-app notification count flow, cached so repeated lookups share the same StateFlow.
    fun notificationCountFor(packageName: String, user: UserHandle): StateFlow<Int> {
        val key = keyFor(packageName, user)
        return notificationCountCache.computeIfAbsent(key) {
            notificationRepository.countFor(packageName, user)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
        }
    }

    fun notificationsFor(packageName: String, user: UserHandle): StateFlow<ImmutableList<NotificationInfo>> {
        val key = keyFor(packageName, user)
        return notificationListCache.computeIfAbsent(key) {
            notificationRepository.notificationsFor(packageName, user)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())
        }
    }

    private fun buildListItems(apps: List<AppInfo>): List<AppListItem> {
        val items = mutableListOf<AppListItem>()
        var currentLetter = ""

        for (app in apps) {
            val firstChar = app.label.sectionLetter

            if (firstChar != currentLetter) {
                currentLetter = firstChar
                items.add(AppListItem.SectionHeader(currentLetter, items.size))
            }

            items.add(AppListItem.AppItem(app, firstChar))
        }

        return items
    }

    private fun buildFavoriteAppsList(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()

        apps.addAll(appsRepository.allApps.filter { it.isFavorite })

        if (apps.size < HOME_SCREEN_APPS) {
            val remainingSlots = HOME_SCREEN_APPS - apps.size
            val mostUsedApps = appsRepository.mostUsedApps.mapNotNull { usageKey ->
                appsRepository.allApps.find { it.key == usageKey }
            }
            val suggestions =
                mostUsedApps
                    .filter { !it.isFavorite && !it.isHidden && !it.doNotSuggest }
                    .take(remainingSlots)
                    .toMutableList()

            if (suggestions.size < remainingSlots) {
                val suggestedKeys = suggestions.mapTo(mutableSetOf()) { it.key }
                suggestions.addAll(
                    appsRepository.allApps
                        .filter { !it.isFavorite && !it.isHidden && !it.doNotSuggest && it.key !in suggestedKeys }
                        .take(remainingSlots - suggestions.size)
                )
            }

            suggestions.forEach { app ->
                apps.add(app.copy(isSuggested = true))
            }
        }

        return apps
    }
}
