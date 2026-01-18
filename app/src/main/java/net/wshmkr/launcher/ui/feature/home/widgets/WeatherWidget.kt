package net.wshmkr.launcher.ui.feature.home.widgets

import android.Manifest
import android.location.Location
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
import net.wshmkr.launcher.util.WeatherHelper.CachedWeather
import net.wshmkr.launcher.util.WeatherHelper.WeatherState

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
        mutableStateOf(
            if (hasStaticLocation) true else WeatherHelper.isLocationGranted(context)
        )
    }
    var location by remember { mutableStateOf<Location?>(null) }
    val locationKey = remember(location, weatherLocationLatitude, weatherLocationLongitude) {
        if (hasStaticLocation) {
            weatherLocationLatitude!! to weatherLocationLongitude!!
        } else {
            location?.let { it.latitude to it.longitude }
        }
    }
    val cachedSnapshot = WeatherHelper.getCachedWeather()
    val cachedMatchesUnit = cachedSnapshot?.isFahrenheit == useFahrenheit
    val cachedMatchesLocation = cachedSnapshot?.let { snapshot ->
        locationKey?.let { key ->
            snapshot.latitude == key.first && snapshot.longitude == key.second
        }
    } ?: false
    val usableCachedWeather = cachedSnapshot?.takeIf { cachedMatchesUnit && cachedMatchesLocation }
    var weatherState by remember(useFahrenheit) {
        mutableStateOf(
            usableCachedWeather?.let {
                WeatherState.Ready(
                    temperature = it.temperature,
                    weatherCode = it.weatherCode,
                    sunriseTime = it.sunriseTime,
                    sunsetTime = it.sunsetTime,
                    isFahrenheit = it.isFahrenheit
                )
            } ?: WeatherState.Idle
        )
    }
    val locationSettingsKey = remember {
        mutableStateOf(weatherLocationLatitude to weatherLocationLongitude)
    }
    val unitSettingsKey = remember { mutableStateOf(useFahrenheit) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(hasPermission, hasStaticLocation) {
        if (hasStaticLocation) {
            location = null
            return@LaunchedEffect
        }
        if (hasPermission) {
            runCatching { WeatherHelper.getBestAvailableLocation(fusedClient) }
                .onSuccess { loc ->
                    if (loc != null) {
                        location = loc
                    } else {
                        weatherState = WeatherState.Error("No location")
                    }
                }
                .onFailure { weatherState = WeatherState.Error("No location") }
        } else {
            weatherState = WeatherState.Idle
        }
    }

    LaunchedEffect(weatherLocationLatitude, weatherLocationLongitude) {
        val newKey = weatherLocationLatitude to weatherLocationLongitude
        if (locationSettingsKey.value != newKey) {
            WeatherHelper.clearCache()
            weatherState = WeatherState.Idle
            locationSettingsKey.value = newKey
        }
    }

    LaunchedEffect(useFahrenheit) {
        if (unitSettingsKey.value != useFahrenheit) {
            WeatherHelper.clearCache()
            weatherState = WeatherState.Idle
            unitSettingsKey.value = useFahrenheit
        }
    }

    LaunchedEffect(locationKey, hasPermission, useFahrenheit) {
        if (!hasPermission || locationKey == null) return@LaunchedEffect
        while (isActive) {
            val now = System.currentTimeMillis()
            val lastFetch = WeatherHelper.getLastFetchTime()
            val cachedRefresh = WeatherHelper.getCachedWeather()
            val cachedMatchesUnitNow = cachedRefresh?.isFahrenheit == useFahrenheit
            val cachedMatchesLocationNow = cachedRefresh?.let {
                it.latitude == locationKey.first && it.longitude == locationKey.second
            } ?: false
            val shouldFetch = now - lastFetch >= WeatherHelper.REFRESH_INTERVAL_MS ||
                    weatherState is WeatherState.Error ||
                    (weatherState is WeatherState.Idle && cachedRefresh == null) ||
                    !cachedMatchesUnitNow ||
                    !cachedMatchesLocationNow

            if (shouldFetch) {
                val activeLocationKey = if (!hasStaticLocation) {
                    val refreshedLocation = runCatching {
                        WeatherHelper.getBestAvailableLocation(fusedClient)
                    }.getOrNull()
                    refreshedLocation?.let { latest ->
                        location = latest
                        latest.latitude to latest.longitude
                    } ?: locationKey
                } else {
                    locationKey
                }
                val cachedForDisplay = if (cachedMatchesUnitNow && cachedMatchesLocationNow) cachedRefresh else null
                if (cachedForDisplay == null) {
                    weatherState = WeatherState.Loading
                }
                val result = WeatherHelper.fetchWeather(
                    activeLocationKey.first,
                    activeLocationKey.second,
                    useFahrenheit
                )
                if (result is WeatherState.Ready) {
                    val updatedCache = with(WeatherHelper) {
                        result.toCached(activeLocationKey.first, activeLocationKey.second)
                    }
                    WeatherHelper.setCachedWeather(updatedCache)
                }
                weatherState = result
            }
            delay(WeatherHelper.REFRESH_INTERVAL_MS)
        }
    }

    WeatherContent(
        state = weatherState,
        cachedWeather = usableCachedWeather,
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
    cachedWeather: CachedWeather?,
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
            if (cachedWeather != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                ) {
                    Icon(
                        painter = WeatherHelper.getWeatherIcon(
                            cachedWeather.weatherCode,
                            WeatherHelper.isNightTime(cachedWeather.sunriseTime, cachedWeather.sunsetTime)
                        ),
                        contentDescription = "Weather",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${cachedWeather.temperature.toInt()}°${if (cachedWeather.isFahrenheit) "F" else "C"}",
                        style = textStyle
                    )
                }
            } else {
                CircularProgressIndicator(
                    modifier = modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
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
