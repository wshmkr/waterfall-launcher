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
import net.wshmkr.launcher.ui.common.icons.BedtimeIcon
import net.wshmkr.launcher.ui.common.icons.ClearDayIcon
import net.wshmkr.launcher.ui.common.icons.CloudIcon
import net.wshmkr.launcher.ui.common.icons.FoggyIcon
import net.wshmkr.launcher.ui.common.icons.HelpIcon
import net.wshmkr.launcher.ui.common.icons.PartlyCloudyDayIcon
import net.wshmkr.launcher.ui.common.icons.PartlyCloudyNightIcon
import net.wshmkr.launcher.ui.common.icons.RainyIcon
import net.wshmkr.launcher.ui.common.icons.ThunderstormIcon
import net.wshmkr.launcher.ui.common.icons.WeatherMixIcon
import net.wshmkr.launcher.ui.common.icons.WeatherSnowyIcon
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getBestAvailableLocation(client: FusedLocationProviderClient): Location? {
        return client.lastLocation.suspendForTask()
            ?: client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).suspendForTask()
    }

    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherState =
        withContext(Dispatchers.IO) {
            val url = "$WEATHER_API_URL?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,weather_code&daily=sunrise,sunset&temperature_unit=fahrenheit"
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
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
                WeatherState.Error(e.message ?: "Unable to load weather")
            } finally {
                connection.disconnect()
            }
        }

    @Composable
    fun getWeatherIcon(code: Int, isNight: Boolean): Painter = when (code) {
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

    fun isNightTime(sunriseTime: String?, sunsetTime: String?): Boolean {
        if (sunriseTime == null || sunsetTime == null) {
            return isNightByHour()
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
                } catch (_: Exception) {
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
        } catch (_: Exception) {
        }

        return isNightByHour()
    }

    private fun isNightByHour(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
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
            val temperatureF: Double,
            val weatherCode: Int,
            val sunriseTime: String? = null,
            val sunsetTime: String? = null
        ) : WeatherState
        data class Error(val reason: String) : WeatherState
    }

    data class CachedWeather(
        val temperatureF: Double,
        val weatherCode: Int,
        val sunriseTime: String?,
        val sunsetTime: String?
    )

    fun WeatherState.Ready.toCached(): CachedWeather = CachedWeather(
        temperatureF = temperatureF,
        weatherCode = weatherCode,
        sunriseTime = sunriseTime,
        sunsetTime = sunsetTime
    )
}

