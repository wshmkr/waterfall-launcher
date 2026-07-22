package net.wshmkr.launcher.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import android.os.UserHandle
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.model.HomeWidgetSettings
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.AppListItem
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.model.sectionLetter
import net.wshmkr.launcher.repository.AppsRepository
import net.wshmkr.launcher.repository.NotificationRepository
import net.wshmkr.launcher.ui.common.components.STAR_SYMBOL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

const val HOME_SCREEN_APPS = 6

@HiltViewModel
class HomeViewModel @Inject constructor(
    appsRepository: AppsRepository,
    private val notificationRepository: NotificationRepository,
    private val userSettingsDataSource: UserSettingsDataSource
) : LauncherViewModel(appsRepository) {

    private var notifications by mutableStateOf<Map<String, Map<UserHandle, List<NotificationInfo>>>>(emptyMap())

    var backgroundUri by mutableStateOf<String?>(null)
        private set

    var homeWidgetSettings by mutableStateOf(HomeWidgetSettings())
        private set

    val allAppsListItems by derivedStateOf {
        buildListItems(appsRepository.allApps.filter { !it.isHidden }, notifications)
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

    val favoriteApps by derivedStateOf { buildFavoriteAppsList(notifications) }

    var activeLetter by mutableStateOf<String?>(null)
        private set

    var showingFavorites by mutableStateOf(true)
        private set

    var showSearchOverlay by mutableStateOf(false)

    private var observedStop = false

    init {
        viewModelScope.launch {
            appsRepository.loadInstalledApps()
            appsRepository.updateMostUsedApps()
        }

        viewModelScope.launch {
            notificationRepository.notifications.collect { newNotifications ->
                notifications = newNotifications
            }
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
    }

    fun onLauncherResumed() {
        if (observedStop) {
            navigateToFavorites()
        }
    }

    fun getAlpha(letter: String): Float {
        return if (activeLetter == null || letter == activeLetter) 1f else 0.2f
    }

    private fun buildListItems(apps: List<AppInfo>, notifications: Map<String, Map<UserHandle, List<NotificationInfo>>>): List<AppListItem> {
        val items = mutableListOf<AppListItem>()
        var currentLetter = ""

        for (app in apps) {
            val firstChar = app.label.sectionLetter

            if (firstChar != currentLetter) {
                currentLetter = firstChar
                items.add(AppListItem.SectionHeader(currentLetter, items.size))
            }

            val appNotifications = notifications[app.packageName]?.get(app.userHandle) ?: emptyList()
            val appWithNotifications = app.copy(notifications = appNotifications)

            items.add(AppListItem.AppItem(appWithNotifications))
        }

        return items
    }

    private fun buildFavoriteAppsList(
        notifications: Map<String, Map<UserHandle, List<NotificationInfo>>>
    ): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()

        val favorites = appsRepository.allApps.filter { it.isFavorite }
        favorites.forEach { app ->
            val appNotifications = notifications[app.packageName]?.get(app.userHandle) ?: emptyList()
            val appWithNotifications = app.copy(notifications = appNotifications)
            apps.add(appWithNotifications)
        }

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
                val appNotifications = notifications[app.packageName]?.get(app.userHandle) ?: emptyList()
                val appWithNotifications = app.copy(notifications = appNotifications, isSuggested = true)
                apps.add(appWithNotifications)
            }
        }

        return apps
    }
}
