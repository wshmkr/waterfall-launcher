package net.wshmkr.launcher.ui.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import net.wshmkr.launcher.ui.common.components.AppLauncher
import net.wshmkr.launcher.ui.feature.search.SearchOverlay
import net.wshmkr.launcher.viewmodel.HomeViewModel


@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState = lifecycleOwner.lifecycle.currentStateAsState()
    
    LaunchedEffect(lifecycleState.value) {
        if (lifecycleState.value == Lifecycle.State.RESUMED) {
            if (!viewModel.showingFavorites) {
                viewModel.navigateToFavorites()
            }
            if (viewModel.showSearchOverlay) {
                viewModel.showSearchOverlay = false
            }
            viewModel.refreshBackground()
        }
    }
    
    AppLauncher(launchAppIntent = viewModel.launchAppIntent)
    Box(modifier = Modifier.fillMaxSize()) {
        viewModel.backgroundUri?.let { uriString ->
            Image(
                painter = rememberAsyncImagePainter(uriString.toUri()),
                contentDescription = "Home screen background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (viewModel.showSearchOverlay) {
            SearchOverlay( {
                viewModel.navigateToFavorites()
                viewModel.showSearchOverlay = false
            })
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
                    activeLetter = viewModel.activeLetter,
                    onLetterSelected = { letter -> viewModel.scrollToLetter(letter) },
                    onSelectionCleared = { viewModel.deselectLetter() },
                )
            }
        }
    }
}
