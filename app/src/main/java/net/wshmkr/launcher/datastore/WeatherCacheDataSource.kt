package net.wshmkr.launcher.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import net.wshmkr.launcher.model.WeatherReading
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.weatherCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "weather_cache",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Singleton
class WeatherCacheDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = context.weatherCacheDataStore

    companion object {
        private val KEY_TEMPERATURE_CELSIUS = doublePreferencesKey("temperature_celsius")
        private val KEY_WEATHER_CODE = intPreferencesKey("weather_code")
        private val KEY_SUNRISE = longPreferencesKey("sunrise_epoch_millis")
        private val KEY_SUNSET = longPreferencesKey("sunset_epoch_millis")
        private val KEY_LATITUDE = doublePreferencesKey("latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("longitude")
        private val KEY_FETCHED_AT = longPreferencesKey("fetched_at")
        private val KEY_FROM_DEVICE_LOCATION = booleanPreferencesKey("from_device_location")
    }

    suspend fun load(): WeatherReading? {
        val preferences = dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .first()
        return runCatching {
            WeatherReading(
                temperatureCelsius = requireNotNull(preferences[KEY_TEMPERATURE_CELSIUS]),
                weatherCode = requireNotNull(preferences[KEY_WEATHER_CODE]),
                sunriseEpochMillis = preferences[KEY_SUNRISE],
                sunsetEpochMillis = preferences[KEY_SUNSET],
                latitude = requireNotNull(preferences[KEY_LATITUDE]),
                longitude = requireNotNull(preferences[KEY_LONGITUDE]),
                fetchedAtMillis = requireNotNull(preferences[KEY_FETCHED_AT]),
                fromDeviceLocation = requireNotNull(preferences[KEY_FROM_DEVICE_LOCATION]),
            )
        }.getOrNull()
    }

    // A failed write must not kill the caller's refresh loop.
    suspend fun save(reading: WeatherReading) {
        runCatching {
            dataStore.edit { preferences ->
                preferences[KEY_TEMPERATURE_CELSIUS] = reading.temperatureCelsius
                preferences[KEY_WEATHER_CODE] = reading.weatherCode
                preferences.setOrRemove(KEY_SUNRISE, reading.sunriseEpochMillis)
                preferences.setOrRemove(KEY_SUNSET, reading.sunsetEpochMillis)
                preferences[KEY_LATITUDE] = reading.latitude
                preferences[KEY_LONGITUDE] = reading.longitude
                preferences[KEY_FETCHED_AT] = reading.fetchedAtMillis
                preferences[KEY_FROM_DEVICE_LOCATION] = reading.fromDeviceLocation
            }
        }
    }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else set(key, value)
    }
}
