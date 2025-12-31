package net.wshmkr.launcher.ui.feature.home.widgets

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
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
import net.wshmkr.launcher.ui.common.icons.HelpIcon
import net.wshmkr.launcher.ui.common.icons.LocationOnIcon
import net.wshmkr.launcher.util.WeatherHelper
import net.wshmkr.launcher.util.WeatherHelper.CachedWeather
import net.wshmkr.launcher.util.WeatherHelper.WeatherState

@Composable
fun WeatherWidget(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasPermission by remember { mutableStateOf(WeatherHelper.isLocationGranted(context)) }
    var location by remember { mutableStateOf<Location?>(null) }
    var weatherState by remember {
        mutableStateOf<WeatherState>(
            WeatherHelper.getCachedWeather()?.let {
                WeatherState.Ready(it.temperatureF, it.weatherCode, it.sunriseTime, it.sunsetTime)
            } ?: WeatherState.Idle
        )
    }
    var lastCachedWeather by remember { mutableStateOf(WeatherHelper.getCachedWeather()) }
    var lastFetchTimestamp by remember { mutableStateOf(WeatherHelper.getLastFetchTime()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            runCatching { WeatherHelper.getBestAvailableLocation(fusedClient) }
                .onSuccess { location = it }
                .onFailure { weatherState = WeatherState.Error("No location") }
        } else {
            weatherState = WeatherState.Idle
        }
    }

    val locationKey = location?.let { Pair(it.latitude, it.longitude) }

    LaunchedEffect(weatherState) {
        if (weatherState is WeatherState.Ready) {
            val cached = with(WeatherHelper) { (weatherState as WeatherState.Ready).toCached() }
            lastCachedWeather = cached
            WeatherHelper.setCachedWeather(cached)
        }
    }

    LaunchedEffect(locationKey, hasPermission) {
        if (!hasPermission || locationKey == null) return@LaunchedEffect
        while (isActive) {
            val now = System.currentTimeMillis()
            val shouldFetch = now - lastFetchTimestamp >= WeatherHelper.REFRESH_INTERVAL_MS ||
                    weatherState is WeatherState.Error ||
                    (weatherState is WeatherState.Idle && WeatherHelper.getCachedWeather() == null)
            if (shouldFetch) {
                if (lastCachedWeather == null) {
                    weatherState = WeatherState.Loading
                }
                weatherState = WeatherHelper.fetchWeather(locationKey.first, locationKey.second)
                lastFetchTimestamp = System.currentTimeMillis()
            }
            delay(WeatherHelper.REFRESH_INTERVAL_MS)
        }
    }

    WeatherContent(
        state = weatherState,
        cachedWeather = lastCachedWeather,
        hasPermission = hasPermission,
        modifier = modifier,
        onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        },
        onClick = onClick
    )
}

@Composable
private fun WeatherContent(
    state: WeatherState,
    cachedWeather: CachedWeather?,
    hasPermission: Boolean,
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onClick: (() -> Unit)? = null
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
                Spacer(modifier = Modifier.size(4.dp))
                Text(text = "Enable location", style = textStyle)
            }
        }

        state is WeatherState.Loading -> {
            if (cachedWeather != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier.then(
                        if (onClick != null) Modifier.clickable { onClick() } else Modifier
                    )
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
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "${cachedWeather.temperatureF.toInt()}°F",
                        style = textStyle
                    )
                }
            } else {
                Text(
                    text = "Weather…",
                    style = textStyle,
                    modifier = modifier
                )
            }
        }

        state is WeatherState.Ready -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.then(
                    if (onClick != null) Modifier.clickable { onClick() } else Modifier
                )
            ) {
                Icon(
                    painter = WeatherHelper.getWeatherIcon(
                        state.weatherCode,
                        WeatherHelper.isNightTime(state.sunriseTime, state.sunsetTime)
                    ),
                    contentDescription = "Weather",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "${state.temperatureF.toInt()}°F",
                    style = textStyle
                )
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
                Spacer(modifier = Modifier.size(4.dp))
                Text(text = "Weather unavailable", style = textStyle)
            }
        }

        else -> {
            Text(
                text = "Locating…",
                style = textStyle,
                modifier = modifier
            )
        }
    }
}
