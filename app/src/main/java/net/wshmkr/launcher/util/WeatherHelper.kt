package net.wshmkr.launcher.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import net.wshmkr.launcher.ui.common.icons.BedtimeIcon
import net.wshmkr.launcher.ui.common.icons.ClearDayIcon
import net.wshmkr.launcher.ui.common.icons.CloudIcon
import net.wshmkr.launcher.ui.common.icons.DrizzleIcon
import net.wshmkr.launcher.ui.common.icons.FoggyIcon
import net.wshmkr.launcher.ui.common.icons.HelpIcon
import net.wshmkr.launcher.ui.common.icons.PartlyCloudyDayIcon
import net.wshmkr.launcher.ui.common.icons.PartlyCloudyNightIcon
import net.wshmkr.launcher.ui.common.icons.RainyIcon
import net.wshmkr.launcher.ui.common.icons.ThunderstormIcon
import net.wshmkr.launcher.ui.common.icons.WeatherMixIcon
import net.wshmkr.launcher.ui.common.icons.WeatherSnowyIcon

object WeatherHelper {
    const val REFRESH_INTERVAL_MS = 30 * 60 * 1000L
    private const val WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast"

    private var cachedWeather: CachedWeather? = null
    private var lastFetchTime: Long = 0L

    fun getCachedWeather(): CachedWeather? = cachedWeather
    fun getLastFetchTime(): Long = lastFetchTime

    fun setCachedWeather(weather: CachedWeather) {
        cachedWeather = weather
        lastFetchTime = System.currentTimeMillis()
    }

    fun isLocationGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    suspend fun getBestAvailableLocation(client: FusedLocationProviderClient): Location? {
        return client.lastLocation.suspendForTask()
            ?: client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).suspendForTask()
    }

    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        useFahrenheit: Boolean
    ): WeatherState =
        withContext(Dispatchers.IO) {
            val temperatureUnit = if (useFahrenheit) "fahrenheit" else "celsius"
            val url = "$WEATHER_API_URL?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,weather_code&daily=sunrise,sunset&temperature_unit=$temperatureUnit&timezone=auto"
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.useCaches = false

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext cachedOrError("HTTP $responseCode")
                }
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
                    temperature = temperature,
                    weatherCode = weatherCode,
                    sunriseTime = sunriseTime,
                    sunsetTime = sunsetTime,
                    isFahrenheit = useFahrenheit
                )
            } catch (e: Exception) {
                cachedOrError(e.message ?: "Unable to load weather")
            } finally {
                connection.disconnect()
            }
        }

    private fun cachedOrError(reason: String): WeatherState =
        getCachedWeather()?.let {
            WeatherState.Ready(
                temperature = it.temperature,
                weatherCode = it.weatherCode,
                sunriseTime = it.sunriseTime,
                sunsetTime = it.sunsetTime,
                isStale = true,
                isFahrenheit = it.isFahrenheit
            )
        } ?: WeatherState.Error(reason)

    @Composable
    fun getWeatherIcon(code: Int, isNight: Boolean): Painter = when (code) {
        0, 1 -> if (isNight) BedtimeIcon() else ClearDayIcon()                  // clear
        2 -> if (isNight) PartlyCloudyNightIcon() else PartlyCloudyDayIcon()    // partly cloudy
        3 -> CloudIcon()                    // cloudy
        45, 48 -> FoggyIcon()               // fog
        51, 53, 55 -> DrizzleIcon()         // light rain
        61, 63, 65 -> RainyIcon()           // rain
        80, 81, 82 -> RainyIcon()           // rain showers
        56, 57 -> WeatherMixIcon()          // freezing drizzle
        66, 67 -> WeatherMixIcon()          // freezing rain
        77 -> WeatherMixIcon()              // snow grains
        71, 73, 75 -> WeatherSnowyIcon()    // snow
        85, 86 -> WeatherSnowyIcon()        // snow showers
        95 -> ThunderstormIcon()            // thunderstorm
        96, 99 -> ThunderstormIcon()        // thunderstorm with hail
        else -> HelpIcon()
    }

    fun isNightTime(sunriseTime: String?, sunsetTime: String?): Boolean {
        val sunrise = sunriseTime?.substringAfter('T')?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val sunset = sunsetTime?.substringAfter('T')?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

        if (sunrise == null || sunset == null) return isNightByHour()

        val now = LocalTime.now()
        return now.isBefore(sunrise) || !now.isBefore(sunset)
    }

    private fun isNightByHour(): Boolean {
        val hour = LocalTime.now().hour
        return hour >= 18 || hour < 6
    }

    private suspend fun <T> Task<T>.suspendForTask(): T? =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { result -> cont.resume(result) }
            addOnFailureListener { exception ->
                if (cont.isActive) cont.resumeWithException(exception)
            }
            addOnCanceledListener {
                if (cont.isActive) cont.resume(null)
            }
        }

    sealed interface WeatherState {
        data object Idle : WeatherState
        data object Loading : WeatherState
        data class Ready(
            val temperature: Double,
            val weatherCode: Int,
            val sunriseTime: String? = null,
            val sunsetTime: String? = null,
            val isStale: Boolean = false,
            val isFahrenheit: Boolean = false,
        ) : WeatherState
        data class Error(val reason: String) : WeatherState
    }

    data class CachedWeather(
        val temperature: Double,
        val weatherCode: Int,
        val sunriseTime: String?,
        val sunsetTime: String?,
        val isFahrenheit: Boolean
    )

    fun WeatherState.Ready.toCached(): CachedWeather = CachedWeather(
        temperature = temperature,
        weatherCode = weatherCode,
        sunriseTime = sunriseTime,
        sunsetTime = sunsetTime,
        isFahrenheit = isFahrenheit
    )
}

