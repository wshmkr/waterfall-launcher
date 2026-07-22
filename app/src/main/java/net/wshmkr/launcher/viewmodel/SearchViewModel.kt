package net.wshmkr.launcher.viewmodel

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.AppListItem
import net.wshmkr.launcher.model.sectionLetter
import net.wshmkr.launcher.repository.AppsRepository
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    appsRepository: AppsRepository
) : LauncherViewModel(appsRepository) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val allAppsFlow = snapshotFlow { appsRepository.allApps.toList() }

    val searchListItems: StateFlow<ImmutableList<AppListItem.AppItem>> =
        combine(_searchQuery.debounce(80), allAppsFlow) { query, apps ->
            filterApps(query, apps)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun launchTopResult() {
        val top = searchListItems.value.firstOrNull() ?: return
        val appInfo = top.appInfo
        launchApp(appInfo.packageName, appInfo.userHandle)
        clearSearch()
    }

    private fun filterApps(query: String, apps: List<AppInfo>): ImmutableList<AppListItem.AppItem> {
        if (query.isEmpty()) return persistentListOf()
        val queryLower = query.lowercase()
        return apps
            .asSequence()
            .filter { app -> app.searchTokens.any { it.startsWith(queryLower) } }
            .map { AppListItem.AppItem(it, it.label.sectionLetter) }
            .toPersistentList()
    }
}
