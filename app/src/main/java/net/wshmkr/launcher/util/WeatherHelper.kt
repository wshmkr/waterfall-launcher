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
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalTime
import java.util.Locale
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
    private const val GEOCODING_API_URL = "https://geocoding-api.open-meteo.com/v1/search"

    private var cachedWeather: CachedWeather? = null
    private var lastFetchTime: Long = 0L

    private fun setCachedWeather(weather: CachedWeather) {
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
        return client.lastLocation.suspendForTask() ?: getCurrentLocation(client)
    }

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    private suspend fun getCurrentLocation(client: FusedLocationProviderClient): Location? {
        val cancellationTokenSource = CancellationTokenSource()
        return client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
            .suspendForTask(cancellationTokenSource)
    }

    suspend fun getWeather(
        latitude: Double,
        longitude: Double,
        useFahrenheit: Boolean
    ): WeatherState {
        val cached = cachedWeather
        val cacheMatches = cached?.let {
            it.latitude == latitude &&
                it.longitude == longitude &&
                it.isFahrenheit == useFahrenheit
        } == true
        val isFresh = cacheMatches && System.currentTimeMillis() - lastFetchTime < REFRESH_INTERVAL_MS
        if (isFresh) {
            return cached!!.toReady(isStale = false)
        }

        val result = fetchWeather(latitude, longitude, useFahrenheit)
        if (result is WeatherState.Ready) {
            setCachedWeather(result.toCached(latitude, longitude))
            return result
        }

        // On failure, fall back to a cached reading for this location instead of showing an error.
        val sameLocationCache = cached?.takeIf {
            it.latitude == latitude && it.longitude == longitude
        }
        return sameLocationCache?.toReady(isStale = true, targetFahrenheit = useFahrenheit)
            ?: result
    }

    private suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        useFahrenheit: Boolean
    ): WeatherState {
        val temperatureUnit = if (useFahrenheit) "fahrenheit" else "celsius"
        val url = "$WEATHER_API_URL?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,weather_code&daily=sunrise,sunset&temperature_unit=$temperatureUnit&timezone=auto"

        return httpGetJson(url).mapCatching { json ->
            val current = json.getJSONObject("current")
            val temperature = current.getDouble("temperature_2m")
            val weatherCode = current.getInt("weather_code")

            val daily = json.optJSONObject("daily")
            val sunriseTime = daily?.optJSONArray("sunrise")?.optString(0)
            val sunsetTime = daily?.optJSONArray("sunset")?.optString(0)

            WeatherState.Ready(
                temperature = temperature,
                weatherCode = weatherCode,
                sunriseTime = sunriseTime,
                sunsetTime = sunsetTime,
                isFahrenheit = useFahrenheit
            )
        }.getOrElse { WeatherState.Error(it.message ?: "Unable to load weather") }
    }

    // Null means the lookup failed; an empty list means it succeeded but matched nothing.
    suspend fun fetchGeocodingResults(
        query: String,
        language: String = Locale.getDefault().language
    ): List<GeocodingResult>? {
        if (query.isBlank()) return emptyList()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$GEOCODING_API_URL?name=$encodedQuery&count=10&language=$language&format=json"

        val json = httpGetJson(url).getOrElse { return null }
        val results = json.optJSONArray("results") ?: return emptyList()
        return buildList {
            for (index in 0 until results.length()) {
                parseGeocodingResult(results.optJSONObject(index))?.let(::add)
            }
        }
    }

    private fun parseGeocodingResult(item: JSONObject?): GeocodingResult? {
        if (item == null) return null
        val name = item.optString("name").takeIf { it.isNotBlank() } ?: return null
        val latitude = item.optDouble("latitude")
        val longitude = item.optDouble("longitude")
        if (latitude.isNaN() || longitude.isNaN()) return null
        return GeocodingResult(
            name = name,
            latitude = latitude,
            longitude = longitude,
            admin1 = item.optString("admin1").takeIf { it.isNotBlank() },
            admin2 = item.optString("admin2").takeIf { it.isNotBlank() },
            country = item.optString("country").takeIf { it.isNotBlank() }
        )
    }

    private suspend fun httpGetJson(url: String): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.useCaches = false

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(IOException("HTTP $responseCode"))
                }
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Result.success(JSONObject(response))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                connection.disconnect()
            }
        }

    @Composable
    fun getWeatherIcon(code: Int, isNight: Boolean): Painter = when (code) {
        0, 1 -> if (isNight) BedtimeIcon() else ClearDayIcon()                  // clear
        2 -> if (isNight) PartlyCloudyNightIcon() else PartlyCloudyDayIcon()    // partly cloudy
        3 -> CloudIcon()                            // cloudy
        45, 48 -> FoggyIcon()                       // fog
        51, 53, 55 -> DrizzleIcon()                 // light rain
        61, 63, 65, 80, 81, 82 -> RainyIcon()       // rain & showers
        56, 57, 66, 67, 77 -> WeatherMixIcon()      // freezing rain & snow grains
        71, 73, 75, 85, 86 -> WeatherSnowyIcon()    // snow & snow showers
        95, 96, 99 -> ThunderstormIcon()            // thunderstorm, with or without hail
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

    private suspend fun <T> Task<T>.suspendForTask(
        cancellationTokenSource: CancellationTokenSource? = null
    ): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.resume(null)
        }
        cont.invokeOnCancellation { cancellationTokenSource?.cancel() }
    }

    sealed interface WeatherState {
        data object Idle : WeatherState
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

    private data class CachedWeather(
        val temperature: Double,
        val weatherCode: Int,
        val sunriseTime: String?,
        val sunsetTime: String?,
        val isFahrenheit: Boolean,
        val latitude: Double,
        val longitude: Double
    )

    data class GeocodingResult(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val admin1: String?,
        val admin2: String?,
        val country: String?,
    ) {
        val displayName: String
            get() = listOfNotNull(name, admin1, country).joinToString(", ")

        val regionLabel: String?
            get() = listOfNotNull(admin2, admin1, country)
                .joinToString(", ")
                .takeIf { it.isNotBlank() }
    }

    fun convertTemperature(value: Double, fromFahrenheit: Boolean, toFahrenheit: Boolean): Double =
        when {
            fromFahrenheit == toFahrenheit -> value
            toFahrenheit -> value * 9 / 5 + 32
            else -> (value - 32) * 5 / 9
        }

    private fun WeatherState.Ready.toCached(latitude: Double, longitude: Double): CachedWeather = CachedWeather(
        temperature = temperature,
        weatherCode = weatherCode,
        sunriseTime = sunriseTime,
        sunsetTime = sunsetTime,
        isFahrenheit = isFahrenheit,
        latitude = latitude,
        longitude = longitude
    )

    private fun CachedWeather.toReady(
        isStale: Boolean,
        targetFahrenheit: Boolean = isFahrenheit
    ): WeatherState.Ready = WeatherState.Ready(
        temperature = convertTemperature(temperature, fromFahrenheit = isFahrenheit, toFahrenheit = targetFahrenheit),
        weatherCode = weatherCode,
        sunriseTime = sunriseTime,
        sunsetTime = sunsetTime,
        isStale = isStale,
        isFahrenheit = targetFahrenheit
    )
}
