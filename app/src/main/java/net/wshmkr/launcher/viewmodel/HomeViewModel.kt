package net.wshmkr.launcher.viewmodel

import android.os.UserHandle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
import net.wshmkr.launcher.model.sectionLetter
import net.wshmkr.launcher.repository.AppsRepository
import net.wshmkr.launcher.repository.CalendarRepository
import net.wshmkr.launcher.repository.NotificationMap
import net.wshmkr.launcher.repository.NotificationRepository
import net.wshmkr.launcher.ui.common.components.STAR_SYMBOL
import net.wshmkr.launcher.util.NotificationPanelHelper
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

    var homeWidgetSettings by mutableStateOf(HomeWidgetSettings())
        private set

    val allAppsListItems by derivedStateOf {
        buildListItems(appsRepository.allApps.filter { !it.isHidden })
    }

    private val letterPositions by derivedStateOf {
        allAppsListItems.asSequence()
            .filterIsInstance<AppListItem.SectionHeader>()
            .associate { it.letter to it.position }
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

    private val _returnHomeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val returnHomeEvents: SharedFlow<Unit> = _returnHomeEvents.asSharedFlow()

    private val notificationsByApp =
        ConcurrentHashMap<Pair<String, UserHandle>, MutableState<ImmutableList<NotificationInfo>>>()

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
            userSettingsDataSource.homeWidgetSettings.collectLatest { settings ->
                homeWidgetSettings = settings
            }
        }

        // Per-app states so a notification invalidates only its own row.
        viewModelScope.launch {
            notificationRepository.notifications.collect { snapshot ->
                for ((app, state) in notificationsByApp) {
                    val (packageName, user) = app
                    state.value = snapshot.notificationsFor(packageName, user)
                }
            }
        }

        // Prune per-package notification states when apps leave the installed set so long-lived
        // sessions with many install/uninstall churns don't grow the cache unboundedly.
        viewModelScope.launch {
            snapshotFlow {
                appsRepository.allApps.mapTo(HashSet(appsRepository.allApps.size)) {
                    it.packageName to it.userHandle
                }
            }
                .collect { liveApps ->
                    notificationsByApp.keys.retainAll(liveApps)
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
        return letterPositions[letter]
    }

    fun deselectLetter() {
        activeLetter = null
    }

    fun navigateToFavorites() {
        activeLetter = null
        showingFavorites = true
        showSearchOverlay = false
        observedStop = false
        _returnHomeEvents.tryEmit(Unit)
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

    fun notificationsFor(packageName: String, user: UserHandle): State<ImmutableList<NotificationInfo>> =
        notificationsByApp.computeIfAbsent(packageName to user) {
            mutableStateOf(notificationRepository.notifications.value.notificationsFor(packageName, user))
        }

    // Reports whether the cancel was actually sent, so a caller can hold off on hiding what is
    // still live — a repost the guard below rejects never reaches the listener either.
    fun clearNotifications(dismissed: List<NotificationInfo>): Boolean {
        val current = notificationRepository.notifications.value
        // Dismissals land after their exit animation, by which point a repost can have taken over
        // the key; cancelling then would take content the user never saw.
        val keys = dismissed.filter { current.timestampOf(it) == it.timestamp }.map { it.key }
        return NotificationPanelHelper.dismissNotifications(keys + orphanedSummaryKeys(keys))
    }

    // A summary the panel hid behind its children resurfaces alone once they are all cleared.
    private fun orphanedSummaryKeys(keys: List<String>): List<String> {
        val cleared = keys.toHashSet()
        val groups = notificationRepository.notifications.value
            .values.flatMap { byUser -> byUser.values.flatten() }
            .filter { it.groupKey != null }
            .groupBy { it.groupKey }
        return groups.values.mapNotNull { group ->
            val (summaries, children) = group.partition { it.isGroupSummary }
            if (children.isEmpty() || children.any { it.key !in cleared }) return@mapNotNull null
            summaries.firstOrNull { it.isClearable && it.key !in cleared }?.key
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

private fun NotificationMap.timestampOf(notification: NotificationInfo): Long? =
    this[notification.packageName]?.get(notification.userHandle)
        ?.firstOrNull { it.key == notification.key }
        ?.timestamp

// Newest first, so the row preview and the panel both read the head of one ordering.
private fun NotificationMap.notificationsFor(
    packageName: String,
    user: UserHandle,
): ImmutableList<NotificationInfo> =
    this[packageName]?.get(user)
        ?.withoutRedundantSummaries()
        ?.sortedByDescending { it.timestamp }
        ?.toImmutableList()
        ?: persistentListOf()

// A summary restates the children posted alongside it, but alone it holds the only copy.
private fun List<NotificationInfo>.withoutRedundantSummaries(): List<NotificationInfo> {
    if (none { it.isGroupSummary }) return this
    val groupsWithChildren = filterNot { it.isGroupSummary }.mapNotNullTo(HashSet()) { it.groupKey }
    return filterNot { it.isGroupSummary && it.groupKey in groupsWithChildren }
}
