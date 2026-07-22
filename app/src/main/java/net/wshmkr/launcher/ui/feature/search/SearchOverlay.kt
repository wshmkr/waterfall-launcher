package net.wshmkr.launcher.ui.feature.search

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.ui.common.components.AppLauncher
import net.wshmkr.launcher.ui.common.components.AppListItem
import net.wshmkr.launcher.ui.common.components.SearchOverlayScaffold
import net.wshmkr.launcher.viewmodel.SearchViewModel

@Composable
fun SearchOverlay(
    onDismiss: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    // Single collector at outer scope; reads inside items() scope recompositions to rows.
    val activeProfiles by viewModel.activeProfiles.collectAsState()
    val queryState = viewModel.searchQuery.collectAsState()
    val listItemsState = viewModel.searchListItems.collectAsState()

    val onQueryChange = remember(viewModel) { viewModel::updateSearchQuery }
    val onSearch = remember(viewModel) { { _: String -> viewModel.launchTopResult() } }
    val onClickApp = remember(viewModel) {
        { app: AppInfo -> viewModel.launchApp(app.packageName, app.userHandle) }
    }
    val onToggleFavorite = remember(viewModel) { viewModel::toggleFavorite }
    val onToggleHidden = remember(viewModel) { viewModel::toggleHidden }
    val onToggleSuggest = remember(viewModel) { viewModel::toggleSuggest }

    DisposableEffect(viewModel) {
        onDispose { viewModel.clearSearch() }
    }

    AppLauncher(launchAppIntent = viewModel.launchAppIntent)

    SearchOverlayScaffold(
        query = { queryState.value },
        onQueryChange = onQueryChange,
        placeholder = "Search apps",
        onDismiss = onDismiss,
        onSearch = onSearch,
    ) {
        items(
            items = listItemsState.value,
            key = { it.appInfo.key },
            contentType = { it::class },
        ) { item ->
            val app = item.appInfo
            val isActiveUser = remember(app.userHandle, activeProfiles) {
                app.userHandle in activeProfiles
            }
            AppListItem(
                appInfo = app,
                isActiveUser = isActiveUser,
                onClick = onClickApp,
                onToggleFavorite = onToggleFavorite,
                onToggleHidden = onToggleHidden,
                onToggleSuggest = onToggleSuggest,
            )
        }
    }
}
