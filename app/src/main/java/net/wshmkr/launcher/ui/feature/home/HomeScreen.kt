package net.wshmkr.launcher.ui.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import net.wshmkr.launcher.ui.common.components.AlphabetSlider
import net.wshmkr.launcher.ui.common.components.AppLauncher
import net.wshmkr.launcher.ui.feature.search.SearchOverlay
import net.wshmkr.launcher.viewmodel.HomeViewModel


@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel,
) {
    AppLauncher(launchAppIntent = viewModel.launchAppIntent)

    val onSearchDismiss = remember(viewModel) {
        {
            viewModel.navigateToFavorites()
            viewModel.showSearchOverlay = false
        }
    }
    val onLetterSelected = remember(viewModel) { viewModel::scrollToLetter }
    val onSelectionCleared = remember(viewModel) { { viewModel.deselectLetter() } }

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.showSearchOverlay) {
            SearchOverlay(onDismiss = onSearchDismiss)
        } else {
            if (viewModel.showingFavorites) {
                FavoritesView(
                    navController = navController,
                    viewModel = viewModel,
                )
            } else {
                AllAppsView(
                    viewModel = viewModel,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                AlphabetSlider(
                    letters = viewModel.alphabetLetters,
                    onLetterSelected = onLetterSelected,
                    onSelectionCleared = onSelectionCleared,
                )
            }
        }
    }
}
