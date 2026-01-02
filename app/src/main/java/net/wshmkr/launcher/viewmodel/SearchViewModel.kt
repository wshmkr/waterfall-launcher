package net.wshmkr.launcher.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.wshmkr.launcher.model.AppListItem
import net.wshmkr.launcher.repository.AppsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    appsRepository: AppsRepository
) : LauncherViewModel(appsRepository) {
    var searchQuery by mutableStateOf("")
        private set

    val searchListItems by derivedStateOf {
        if (searchQuery.isEmpty()) {
            emptyList()
        } else {
            appsRepository.allApps
                .filter { app ->
                    app.label.split(" ").any { it.startsWith(searchQuery, ignoreCase = true) }
                }
                .map { AppListItem.AppItem(it) }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun clearSearch() {
        searchQuery = ""
    }

    fun onSearch(query: String) {
        if (searchListItems.isNotEmpty()) {
            val appInfo = searchListItems[0].appInfo
            launchApp(appInfo.packageName, appInfo.userHandle)
            clearSearch()
        }
    }
}
