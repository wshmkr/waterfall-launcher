package net.wshmkr.launcher.ui.feature.search

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import net.wshmkr.launcher.ui.common.components.AppLauncher
import net.wshmkr.launcher.ui.common.components.AppListItem
import net.wshmkr.launcher.ui.common.components.SearchOverlayScaffold
import net.wshmkr.launcher.viewmodel.SearchViewModel

@Composable
fun SearchOverlay(
    onDismiss: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val activeProfiles by viewModel.activeProfiles.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSearch()
        }
    }

    AppLauncher(launchAppIntent = viewModel.launchAppIntent)

    SearchOverlayScaffold(
        query = viewModel.searchQuery,
        onQueryChange = viewModel::updateSearchQuery,
        placeholder = "Search apps",
        onDismiss = onDismiss,
        onSearch = { viewModel.launchTopResult() },
    ) {
        items(
            items = viewModel.searchListItems,
            key = { item -> item.appInfo.key },
        ) { item ->
            AppListItem(
                appInfo = item.appInfo,
                activeProfiles = activeProfiles,
                viewModel = viewModel,
            )
        }
    }
}
