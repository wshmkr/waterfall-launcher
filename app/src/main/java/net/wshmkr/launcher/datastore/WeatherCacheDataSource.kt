package net.wshmkr.launcher.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import net.wshmkr.launcher.model.WeatherReading
import javax.inject.Inject
import javax.inject.Singleton

private val Context.weatherCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_cache")

@Singleton
class WeatherCacheDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_TEMPERATURE_CELSIUS = doublePreferencesKey("temperature_celsius")
        private val KEY_WEATHER_CODE = intPreferencesKey("weather_code")
        private val KEY_SUNRISE = stringPreferencesKey("sunrise")
        private val KEY_SUNSET = stringPreferencesKey("sunset")
        private val KEY_LATITUDE = doublePreferencesKey("latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("longitude")
        private val KEY_FETCHED_AT = longPreferencesKey("fetched_at")
    }

    suspend fun load(): WeatherReading? = runCatching {
        val preferences = context.weatherCacheDataStore.data.first()
        WeatherReading(
            temperatureCelsius = preferences[KEY_TEMPERATURE_CELSIUS] ?: return null,
            weatherCode = preferences[KEY_WEATHER_CODE] ?: return null,
            sunriseTime = preferences[KEY_SUNRISE],
            sunsetTime = preferences[KEY_SUNSET],
            latitude = preferences[KEY_LATITUDE] ?: return null,
            longitude = preferences[KEY_LONGITUDE] ?: return null,
            fetchedAtMillis = preferences[KEY_FETCHED_AT] ?: return null,
        )
    }.getOrNull()

    suspend fun save(reading: WeatherReading) {
        runCatching {
            context.weatherCacheDataStore.edit { preferences ->
                preferences[KEY_TEMPERATURE_CELSIUS] = reading.temperatureCelsius
                preferences[KEY_WEATHER_CODE] = reading.weatherCode
                reading.sunriseTime?.let { preferences[KEY_SUNRISE] = it } ?: preferences.remove(KEY_SUNRISE)
                reading.sunsetTime?.let { preferences[KEY_SUNSET] = it } ?: preferences.remove(KEY_SUNSET)
                preferences[KEY_LATITUDE] = reading.latitude
                preferences[KEY_LONGITUDE] = reading.longitude
                preferences[KEY_FETCHED_AT] = reading.fetchedAtMillis
            }
        }
    }
}
