package net.wshmkr.launcher.ui.feature.home.widgets

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.wshmkr.launcher.ui.common.icons.CloudOffIcon
import net.wshmkr.launcher.ui.common.icons.HelpIcon
import net.wshmkr.launcher.ui.common.icons.LocationOnIcon
import net.wshmkr.launcher.util.WeatherHelper
import net.wshmkr.launcher.util.WeatherHelper.WeatherState

@SuppressLint("MissingPermission")
@Composable
fun WeatherWidget(
    modifier: Modifier = Modifier,
    useFahrenheit: Boolean = false,
    weatherLocationLatitude: Double? = null,
    weatherLocationLongitude: Double? = null,
) {
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val hasStaticLocation = weatherLocationLatitude != null && weatherLocationLongitude != null
    var hasPermission by remember(hasStaticLocation) {
        mutableStateOf(if (hasStaticLocation) true else WeatherHelper.isLocationGranted(context))
    }
    var weatherState by remember(useFahrenheit) { mutableStateOf<WeatherState>(WeatherState.Idle) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(hasPermission, hasStaticLocation, useFahrenheit, weatherLocationLatitude, weatherLocationLongitude) {
        if (!hasPermission) {
            weatherState = WeatherState.Idle
            return@LaunchedEffect
        }

        while (isActive) {
            val currentKey = if (hasStaticLocation) {
                weatherLocationLatitude to weatherLocationLongitude
            } else {
                val loc = runCatching { WeatherHelper.getBestAvailableLocation(fusedClient) }.getOrNull()
                loc?.let { it.latitude to it.longitude }
            }

            if (currentKey == null) {
                weatherState = WeatherState.Error("No location")
                delay(WeatherHelper.REFRESH_INTERVAL_MS)
                continue
            }

            val cacheMatchesCurrent = WeatherHelper.isCacheValid(currentKey.first, currentKey.second, useFahrenheit)
            val isStale = System.currentTimeMillis() - WeatherHelper.getLastFetchTime() >= WeatherHelper.REFRESH_INTERVAL_MS
            val shouldFetch = !cacheMatchesCurrent || isStale || weatherState is WeatherState.Error

            if (shouldFetch) {
                if (!cacheMatchesCurrent) {
                    weatherState = WeatherState.Loading
                }
                val result = WeatherHelper.fetchWeather(currentKey.first, currentKey.second, useFahrenheit)
                if (result is WeatherState.Ready) {
                    with(WeatherHelper) {
                        setCachedWeather(result.toCached(currentKey.first, currentKey.second))
                    }
                }
                weatherState = result
            }

            delay(WeatherHelper.REFRESH_INTERVAL_MS)
        }
    }

    WeatherContent(
        state = weatherState,
        hasPermission = hasPermission,
        modifier = modifier,
        onRequestPermission = {
            if (!hasStaticLocation) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    )
}

@Composable
private fun WeatherContent(
    state: WeatherState,
    hasPermission: Boolean,
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = Color.White,
        fontSize = 16.sp
    )

    when {
        !hasPermission -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.clickable { onRequestPermission() }
            ) {
                Icon(
                    painter = LocationOnIcon(),
                    contentDescription = "Enable location",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Enable location", style = textStyle)
            }
        }

        state is WeatherState.Loading -> {
            CircularProgressIndicator(
                modifier = modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }

        state is WeatherState.Ready -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
            ) {
                Icon(
                    painter = if (state.isStale) CloudOffIcon()
                    else WeatherHelper.getWeatherIcon(
                        state.weatherCode,
                        WeatherHelper.isNightTime(state.sunriseTime, state.sunsetTime)
                    ),
                    contentDescription = "Weather",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${state.temperature.toInt()}°${if (state.isFahrenheit) "F" else "C"}",
                    style = textStyle
                )
                if (state.isStale) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "stale", style = textStyle.copy(color = Color.Gray))
                }
            }
        }

        state is WeatherState.Error -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
            ) {
                Icon(
                    painter = HelpIcon(),
                    contentDescription = "Weather unavailable",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Weather unavailable", style = textStyle)
            }
        }

        else -> {
            CircularProgressIndicator(
                modifier = modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
    }
}
