package net.wshmkr.launcher.ui.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.util.WeatherHelper
import net.wshmkr.launcher.viewmodel.SettingsViewModel

@Composable
fun WeatherLocationSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings = viewModel.homeWidgetSettings
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherHelper.GeocodingResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            results = emptyList()
            isLoading = false
            return@LaunchedEffect
        }
        delay(400)
        isLoading = true
        results = WeatherHelper.fetchGeocodingResults(trimmedQuery)
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Text(
                text = "Weather location",
                color = Color.White,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Search for a city or region",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Start typing...") },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            MenuOption(
                text = "Use device location",
                subtext = if (settings.weatherLocationName == null) "Currently selected" else null,
                color = Color.White,
                onClick = {
                    viewModel.clearWeatherLocation()
                    navController.popBackStack()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else if (query.isNotBlank() && results.isEmpty()) {
            item {
                Text(
                    text = "No results",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(results) { result ->
            MenuOption(
                text = result.displayName,
                subtext = "Lat ${"%.4f".format(result.latitude)}, Lon ${"%.4f".format(result.longitude)}",
                color = Color.White,
                onClick = {
                    viewModel.setWeatherLocation(
                        name = result.displayName,
                        latitude = result.latitude,
                        longitude = result.longitude
                    )
                    navController.popBackStack()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
