package net.wshmkr.launcher.ui.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.ui.common.components.SearchOverlayScaffold
import net.wshmkr.launcher.ui.common.icons.LocationOnIcon
import net.wshmkr.launcher.util.WeatherHelper
import net.wshmkr.launcher.viewmodel.SettingsViewModel

@Composable
fun WeatherLocationOverlay(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherHelper.GeocodingResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            results = emptyList()
            isLoading = false
            hasError = false
            return@LaunchedEffect
        }
        delay(400)
        isLoading = true
        val fetched = WeatherHelper.fetchGeocodingResults(trimmedQuery)
        hasError = fetched == null
        results = fetched ?: emptyList()
        isLoading = false
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissOverlay = remember(keyboardController, navController) {
        {
            keyboardController?.hide()
            navController.popBackStack()
            Unit
        }
    }
    val onQueryChange = remember { { value: String -> query = value } }
    val onDismissRequest = remember(navController) { { navController.popBackStack(); Unit } }
    val onUseDeviceLocation = remember(viewModel, dismissOverlay) {
        {
            viewModel.clearWeatherLocation()
            dismissOverlay()
        }
    }

    SearchOverlayScaffold(
        query = { query },
        onQueryChange = onQueryChange,
        placeholder = "Enter weather location",
        onDismiss = onDismissRequest,
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            MenuOption(
                icon = LocationOnIcon(),
                text = "Use device location",
                color = Color.White,
                onClick = onUseDeviceLocation
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        } else if (hasError) {
            item {
                Text(
                    text = "Couldn't reach search. Check your connection.",
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else if (query.isNotBlank() && results.isEmpty()) {
            item {
                Text(
                    text = "No results",
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(
            items = results,
            key = { result -> "${result.latitude},${result.longitude}:${result.displayName}" }
        ) { result ->
            val onSelect = remember(result, viewModel, dismissOverlay) {
                {
                    viewModel.setWeatherLocation(
                        name = result.displayName,
                        latitude = result.latitude,
                        longitude = result.longitude
                    )
                    dismissOverlay()
                }
            }
            MenuOption(
                text = result.name,
                subtext = result.regionLabel,
                color = Color.White,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
