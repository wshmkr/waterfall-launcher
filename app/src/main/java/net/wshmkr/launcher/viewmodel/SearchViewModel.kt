package net.wshmkr.launcher.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import net.wshmkr.launcher.model.AppListItem
import net.wshmkr.launcher.model.sectionLetter
import net.wshmkr.launcher.repository.AppsRepository
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
            val queryLower = searchQuery.lowercase()
            appsRepository.allApps
                .filter { app ->
                    app.searchTokens.any { it.startsWith(queryLower) }
                }
                .map { AppListItem.AppItem(it, it.label.sectionLetter) }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun clearSearch() {
        searchQuery = ""
    }

    fun launchTopResult() {
        if (searchListItems.isNotEmpty()) {
            val appInfo = searchListItems[0].appInfo
            launchApp(appInfo.packageName, appInfo.userHandle)
            clearSearch()
        }
    }
}
