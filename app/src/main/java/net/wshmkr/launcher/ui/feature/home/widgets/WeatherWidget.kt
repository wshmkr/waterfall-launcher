package net.wshmkr.launcher.ui.feature.home.widgets

import android.Manifest
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import net.wshmkr.launcher.model.WeatherReading
import net.wshmkr.launcher.model.WeatherUiState
import net.wshmkr.launcher.ui.common.icons.CloudOffIcon
import net.wshmkr.launcher.ui.common.icons.HelpIcon
import net.wshmkr.launcher.ui.common.icons.LocationOnIcon
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.rememberAmbientBodyStyle
import net.wshmkr.launcher.util.WeatherHelper
import net.wshmkr.launcher.util.rememberCurrentLocalTime

@Composable
fun WeatherWidget(
    state: WeatherUiState,
    useFahrenheit: Boolean,
    hasStaticLocation: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Raw permission state is remembered across static/dynamic toggles so flipping the source
    // doesn't wipe an in-flight grant.
    var rawHasPermission by remember { mutableStateOf(WeatherHelper.isLocationGranted(context)) }
    val hasPermission = hasStaticLocation || rawHasPermission

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            rawHasPermission = granted
            // Grants via system Settings are covered by the resume-triggered refresh instead.
            if (granted) onRefresh()
        }
    )

    // Re-check on resume so grants made via system Settings are picked up.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        rawHasPermission = WeatherHelper.isLocationGranted(context)
    }

    WeatherContent(
        state = state,
        hasPermission = hasPermission,
        useFahrenheit = useFahrenheit,
        modifier = modifier,
        onRequestPermission = {
            if (!hasStaticLocation) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        },
        onRetry = onRefresh
    )
}

@Composable
private fun WeatherContent(
    state: WeatherUiState,
    hasPermission: Boolean,
    useFahrenheit: Boolean,
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = rememberAmbientBodyStyle(LocalDimensions.current.fontMedium)

    when {
        !hasPermission -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.clickable { onRequestPermission() }
            ) {
                Icon(
                    painter = LocationOnIcon(),
                    contentDescription = "Enable location",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Enable location", style = textStyle)
            }
        }

        state is WeatherUiState.Ready -> {
            WeatherReadyRow(
                reading = state.reading,
                isStale = state.isStale,
                useFahrenheit = useFahrenheit,
                modifier = modifier,
                textStyle = textStyle
            )
        }

        state is WeatherUiState.Error -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.clickable { onRetry() }
            ) {
                Icon(
                    painter = HelpIcon(),
                    contentDescription = "Weather unavailable, tap to retry",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Weather unavailable", style = textStyle)
            }
        }

        else -> {
            CircularProgressIndicator(
                modifier = modifier.size(18.dp),
                color = colors.primary,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun WeatherReadyRow(
    reading: WeatherReading,
    isStale: Boolean,
    useFahrenheit: Boolean,
    modifier: Modifier,
    textStyle: androidx.compose.ui.text.TextStyle,
) {
    val colors = MaterialTheme.colorScheme
    val now by rememberCurrentLocalTime()
    val isNight = remember(now, reading.sunriseTime, reading.sunsetTime) {
        WeatherHelper.isNightAt(now, reading.sunriseTime, reading.sunsetTime)
    }
    val iconRes = remember(isStale, reading.weatherCode, isNight) {
        if (isStale) null else WeatherHelper.weatherIconRes(reading.weatherCode, isNight)
    }
    val displayTemperature = if (useFahrenheit) {
        WeatherHelper.celsiusToFahrenheit(reading.temperatureCelsius)
    } else {
        reading.temperatureCelsius
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = if (iconRes == null) CloudOffIcon() else painterResource(iconRes),
            contentDescription = "Weather",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${displayTemperature.toInt()}°${if (useFahrenheit) "F" else "C"}",
            style = textStyle
        )
        if (isStale) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "stale", style = textStyle.copy(color = colors.onSurfaceVariant))
        }
    }
}
