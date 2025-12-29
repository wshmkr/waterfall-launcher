package net.wshmkr.launcher.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import android.os.UserHandle
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.ListItem
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.repository.AppsRepository
import net.wshmkr.launcher.repository.NotificationRepository
import net.wshmkr.launcher.ui.feature.home.STAR_SYMBOL
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

    val allAppsListItems by derivedStateOf {
        buildListItems(appsRepository.allApps.filter { !it.isHidden }, notifications)
    }
    
    val alphabetLetters by derivedStateOf {
        buildList {
            add(STAR_SYMBOL)
            val letters = appsRepository.allApps
                .filter { !it.isHidden }
                .map { it.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
                .distinct()
                .sorted()
            addAll(letters)
        }
    }
    
    val favoriteListItems by derivedStateOf {
        buildFavoriteListItems(notifications)
    }

    var activeLetter by mutableStateOf<String?>(null)
        private set

    var showingFavorites by mutableStateOf(true)
        private set

    var showSearchOverlay by mutableStateOf(false)

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
            appsRepository.activeProfiles
                .drop(1)
                .collectLatest {
                    appsRepository.refreshAppIcons()
                }
        }
        
        viewModelScope.launch {
            backgroundUri = userSettingsDataSource.getBackgroundUri()
        }
    }
    
    fun refreshBackground() {
        viewModelScope.launch {
            backgroundUri = userSettingsDataSource.getBackgroundUri()
        }
    }

    fun scrollToLetter(letter: String) {
        activeLetter = letter
        showingFavorites = letter == STAR_SYMBOL
    }
    
    fun getScrollPosition(letter: String): Int? {
        if (letter == STAR_SYMBOL) return null
        
        val header = allAppsListItems.find { 
            it is ListItem.SectionHeader && it.letter == letter 
        } as? ListItem.SectionHeader
        
        return header?.position
    }

    fun deselectLetter() {
        activeLetter = null
    }

    fun navigateToFavorites() {
        activeLetter = null
        showingFavorites = true
    }

    fun getAlpha(letter: String): Float {
        return if (activeLetter == null || letter == activeLetter) 1f else 0.2f
    }

    private fun buildListItems(apps: List<AppInfo>, notifications: Map<String, Map<UserHandle, List<NotificationInfo>>>): List<ListItem> {
        val items = mutableListOf<ListItem>()
        var currentLetter = ""
        
        for (app in apps) {
            val firstChar = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            
            if (firstChar != currentLetter) {
                currentLetter = firstChar
                items.add(ListItem.SectionHeader(currentLetter, items.size))
            }

            val appNotifications = notifications[app.packageName]?.get(app.userHandle) ?: emptyList()
            val appWithNotifications = app.copy(notifications = appNotifications)
            
            items.add(ListItem.AppItem(appWithNotifications))
        }
        
        return items
    }
    
    private fun buildFavoriteListItems(notifications: Map<String, Map<UserHandle, List<NotificationInfo>>>): List<ListItem> {
        val items = mutableListOf<ListItem>()
        
        items.add(ListItem.ClockWidget)
        items.add(ListItem.WidgetHost)
        items.add(ListItem.MediaWidget)
        
        val favorites = appsRepository.allApps.filter { it.isFavorite }
        favorites.forEach { app ->
            val appNotifications = notifications[app.packageName]?.get(app.userHandle) ?: emptyList()
            val appWithNotifications = app.copy(notifications = appNotifications)
            items.add(ListItem.AppItem(appWithNotifications))
        }
        
        if (items.size < HOME_SCREEN_APPS + 2) {
            val remainingSlots = HOME_SCREEN_APPS + 2 - items.size
            val mostUsedApps = appsRepository.mostUsedApps.mapNotNull { packageName ->
                appsRepository.allApps.find { it.packageName == packageName }
            }
            val suggestions =
                mostUsedApps
                    .filter { !it.isFavorite && !it.isHidden && !it.doNotSuggest }
                    .take(remainingSlots)
                    .toMutableList()

            if (suggestions.size < remainingSlots) {
                suggestions.addAll(
                    appsRepository.allApps.filter { !it.isFavorite && !it.isHidden && !it.doNotSuggest }
                        .take(remainingSlots - suggestions.size)
                )
            }

            suggestions.forEach { app ->
                val appNotifications = notifications[app.packageName]?.get(app.userHandle) ?: emptyList()
                val appWithNotifications = app.copy(notifications = appNotifications, isSuggested = true)
                items.add(ListItem.AppItem(appWithNotifications))
            }
        }

        return items
    }
}
