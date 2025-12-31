package net.wshmkr.launcher.ui.feature.home.widgets

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
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
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import net.wshmkr.launcher.ui.common.icons.BedtimeIcon
import net.wshmkr.launcher.ui.common.icons.ClearDayIcon
import net.wshmkr.launcher.ui.common.icons.CloudIcon
import net.wshmkr.launcher.ui.common.icons.FoggyIcon
import net.wshmkr.launcher.ui.common.icons.HelpIcon
import net.wshmkr.launcher.ui.common.icons.LocationOnIcon
import net.wshmkr.launcher.ui.common.icons.PartlyCloudyDayIcon
import net.wshmkr.launcher.ui.common.icons.PartlyCloudyNightIcon
import net.wshmkr.launcher.ui.common.icons.RainyIcon
import net.wshmkr.launcher.ui.common.icons.ThunderstormIcon
import net.wshmkr.launcher.ui.common.icons.WeatherMixIcon
import net.wshmkr.launcher.ui.common.icons.WeatherSnowyIcon

private const val THIRTY_MINUTES_MS = 30 * 60 * 1000L
private const val TAG = "WeatherWidget"

private sealed interface WeatherState {
    data object Idle : WeatherState
    data object Loading : WeatherState
    data class Ready(
        val temperatureF: Double,
        val weatherCode: Int,
        val sunriseTime: String? = null,
        val sunsetTime: String? = null
    ) : WeatherState
    data class Error(val reason: String) : WeatherState
}

@Composable
fun WeatherWidget(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasPermission by remember { mutableStateOf(isLocationGranted(context)) }
    var location by remember { mutableStateOf<Location?>(null) }
    var weatherState by remember { mutableStateOf<WeatherState>(WeatherState.Idle) }
    var lastFetchTimestamp by remember { mutableStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission = granted
        }
    )

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            runCatching { fusedClient.getBestAvailableLocation() }
                .onSuccess { location = it }
                .onFailure {
                    Log.w(TAG, "Failed to get location", it)
                    weatherState = WeatherState.Error("No location")
                }
        } else {
            weatherState = WeatherState.Idle
        }
    }

    val locationKey = location?.let { Pair(it.latitude, it.longitude) }

    LaunchedEffect(locationKey, hasPermission) {
        if (!hasPermission || locationKey == null) return@LaunchedEffect
        while (isActive) {
            val now = System.currentTimeMillis()
            val shouldFetch = now - lastFetchTimestamp >= THIRTY_MINUTES_MS ||
                    weatherState is WeatherState.Error ||
                    weatherState is WeatherState.Idle
            if (shouldFetch) {
                weatherState = WeatherState.Loading
                weatherState = fetchWeather(locationKey.first, locationKey.second)
                lastFetchTimestamp = System.currentTimeMillis()
            }
            delay(THIRTY_MINUTES_MS)
        }
    }

    WeatherContent(
        state = weatherState,
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
            Text(
                text = "Weather…",
                style = textStyle,
                modifier = modifier
            )
        }

        state is WeatherState.Ready -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.then(
                    if (onClick != null) Modifier.clickable { onClick() } else Modifier
                )
            ) {
                Icon(
                    painter = weatherIcon(
                        state.weatherCode,
                        isNightTime(state.sunriseTime, state.sunsetTime)
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

private suspend fun fetchWeather(
    latitude: Double,
    longitude: Double
): WeatherState = withContext(Dispatchers.IO) {
    val url =
        "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code&daily=sunrise,sunset&temperature_unit=fahrenheit"
    val connection = URL(url).openConnection() as HttpURLConnection
    return@withContext try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.useCaches = false

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        val current = json.getJSONObject("current")
        val temperature = current.getDouble("temperature_2m")
        val weatherCode = current.getInt("weather_code")
        
        val daily = json.optJSONObject("daily")
        val sunriseArray = daily?.optJSONArray("sunrise")
        val sunsetArray = daily?.optJSONArray("sunset")
        val sunriseTime = sunriseArray?.optString(0)
        val sunsetTime = sunsetArray?.optString(0)

        WeatherState.Ready(
            temperatureF = temperature,
            weatherCode = weatherCode,
            sunriseTime = sunriseTime,
            sunsetTime = sunsetTime
        )
    } catch (e: Exception) {
        Log.w(TAG, "Weather fetch failed for $latitude,$longitude", e)
        WeatherState.Error(e.message ?: "Unable to load weather")
    } finally {
        connection.disconnect()
    }
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
private suspend fun FusedLocationProviderClient.getBestAvailableLocation(): Location? {
    return lastLocation.suspendForTask()
        ?: getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).suspendForTask()
}

private fun isLocationGranted(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.suspendForTask(): T? =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.resume(null)
        }
    }

@Composable
private fun weatherIcon(code: Int, isNight: Boolean) = when (code) {
    0, 1 -> if (isNight) BedtimeIcon() else ClearDayIcon()
    2 -> if (isNight) PartlyCloudyNightIcon() else PartlyCloudyDayIcon()
    3 -> CloudIcon()
    45, 48 -> FoggyIcon()
    51, 53, 55 -> RainyIcon()
    61, 63, 65, 80, 81, 82 -> RainyIcon()
    56, 57, 66, 67, 77 -> WeatherMixIcon()
    71, 73, 75, 85, 86 -> WeatherSnowyIcon()
    95, 96, 99 -> ThunderstormIcon()
    else -> HelpIcon()
}

private fun isNightTime(sunriseTime: String?, sunsetTime: String?): Boolean {
    if (sunriseTime == null || sunsetTime == null) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour >= 18 || hour < 6
    }
    
    try {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        )
        
        var sunrise: Date? = null
        var sunset: Date? = null
        
        for (format in formats) {
            try {
                sunrise = format.parse(sunriseTime)
                sunset = format.parse(sunsetTime)
                if (sunrise != null && sunset != null) break
            } catch (e: Exception) {
            }
        }
        
        if (sunrise != null && sunset != null) {
            val now = Date()
            return if (sunset.after(sunrise)) {
                now.after(sunset) || now.before(sunrise)
            } else {
                now.after(sunset) && now.before(sunrise)
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to parse sunrise/sunset times, using fallback", e)
    }
    
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    return hour >= 18 || hour < 6
}
