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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.wshmkr.launcher.ui.common.icons.CloudOffIcon
import net.wshmkr.launcher.ui.common.icons.HelpIcon
import net.wshmkr.launcher.ui.common.icons.LocationOnIcon
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.util.WeatherHelper
import net.wshmkr.launcher.util.WeatherHelper.WeatherState
import net.wshmkr.launcher.util.rememberCurrentLocalTime

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
    // Raw permission state is remembered across static/dynamic toggles so flipping the source
    // doesn't wipe an in-flight grant.
    var rawHasPermission by remember { mutableStateOf(WeatherHelper.isLocationGranted(context)) }
    val hasPermission = hasStaticLocation || rawHasPermission
    var weatherState by remember { mutableStateOf<WeatherState>(WeatherState.Idle) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> rawHasPermission = granted }
    )

    // Re-check on resume so grants made via system Settings are picked up.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        rawHasPermission = WeatherHelper.isLocationGranted(context)
    }

    // Convert the current reading locally for an instant update; the fetch loop refreshes it after.
    LaunchedEffect(useFahrenheit) {
        val current = weatherState
        if (current is WeatherState.Ready && current.isFahrenheit != useFahrenheit) {
            weatherState = current.copy(
                temperature = WeatherHelper.convertTemperature(
                    current.temperature,
                    fromFahrenheit = current.isFahrenheit,
                    toFahrenheit = useFahrenheit
                ),
                isFahrenheit = useFahrenheit
            )
        }
    }

    LaunchedEffect(hasPermission, useFahrenheit, weatherLocationLatitude, weatherLocationLongitude, retryTrigger) {
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

            weatherState = WeatherHelper.getWeather(currentKey.first, currentKey.second, useFahrenheit)

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
        },
        onRetry = { retryTrigger++ }
    )
}

@Composable
private fun WeatherContent(
    state: WeatherState,
    hasPermission: Boolean,
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit
) {
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = Color.White,
        fontSize = LocalDimensions.current.weatherFont
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

        state is WeatherState.Ready -> {
            WeatherReadyRow(state = state, modifier = modifier, textStyle = textStyle)
        }

        state is WeatherState.Error -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.clickable { onRetry() }
            ) {
                Icon(
                    painter = HelpIcon(),
                    contentDescription = "Weather unavailable, tap to retry",
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

@Composable
private fun WeatherReadyRow(
    state: WeatherState.Ready,
    modifier: Modifier,
    textStyle: androidx.compose.ui.text.TextStyle,
) {
    val now by rememberCurrentLocalTime()
    val isNight = remember(now, state.sunriseTime, state.sunsetTime) {
        WeatherHelper.isNightAt(now, state.sunriseTime, state.sunsetTime)
    }
    val iconRes = remember(state.isStale, state.weatherCode, isNight) {
        if (state.isStale) null else WeatherHelper.weatherIconRes(state.weatherCode, isNight)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = if (iconRes == null) CloudOffIcon() else painterResource(iconRes),
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
