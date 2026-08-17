package net.wshmkr.launcher.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.wshmkr.launcher.R
import net.wshmkr.launcher.model.WeatherReading
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalTime
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object WeatherHelper {
    private const val WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast"
    private const val GEOCODING_API_URL = "https://geocoding-api.open-meteo.com/v1/search"

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

    suspend fun fetchCurrentWeather(latitude: Double, longitude: Double): Result<WeatherReading> {
        val url = "$WEATHER_API_URL?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,weather_code&daily=sunrise,sunset&timezone=auto"

        return httpGetJson(url).mapCatching { json ->
            val current = json.getJSONObject("current")
            val daily = json.optJSONObject("daily")

            WeatherReading(
                temperatureCelsius = current.getDouble("temperature_2m"),
                weatherCode = current.getInt("weather_code"),
                sunriseTime = daily?.optJSONArray("sunrise")?.optString(0),
                sunsetTime = daily?.optJSONArray("sunset")?.optString(0),
                latitude = latitude,
                longitude = longitude,
                fetchedAtMillis = System.currentTimeMillis(),
            )
        }
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
                results.optJSONObject(index)?.let(::parseGeocodingResult)?.let(::add)
            }
        }
    }

    private fun parseGeocodingResult(item: JSONObject): GeocodingResult? {
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

    @DrawableRes
    fun weatherIconRes(code: Int, isNight: Boolean): Int = when (code) {
        0, 1 -> if (isNight) R.drawable.icon_bedtime else R.drawable.icon_clear_day
        2 -> if (isNight) R.drawable.icon_partly_cloudy_night else R.drawable.icon_partly_cloudy_day
        3 -> R.drawable.icon_cloud                                          // cloudy
        45, 48 -> R.drawable.icon_foggy                                     // fog
        51, 53, 55 -> R.drawable.icon_drizzle                               // light rain
        61, 63, 65, 80, 81, 82 -> R.drawable.icon_rainy                     // rain & showers
        56, 57, 66, 67, 77 -> R.drawable.icon_weather_mix                   // freezing rain & snow grains
        71, 73, 75, 85, 86 -> R.drawable.icon_weather_snowy                 // snow & snow showers
        95, 96, 99 -> R.drawable.icon_thunderstorm                          // thunderstorm, with or without hail
        else -> R.drawable.icon_help
    }

    // Sunrise/sunset ISO string form is `YYYY-MM-DDTHH:mm`; only the local-time part is compared.
    fun isNightAt(now: LocalTime, sunriseTime: String?, sunsetTime: String?): Boolean {
        val sunrise = parseLocalTime(sunriseTime)
        val sunset = parseLocalTime(sunsetTime)
        if (sunrise == null || sunset == null) return isNightHour(now.hour)
        return now.isBefore(sunrise) || !now.isBefore(sunset)
    }

    fun isNightHour(hour: Int): Boolean = hour >= 18 || hour < 6

    private fun parseLocalTime(iso: String?): LocalTime? =
        iso?.substringAfter('T')?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

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

    fun celsiusToFahrenheit(value: Double): Double = value * 9 / 5 + 32
}
