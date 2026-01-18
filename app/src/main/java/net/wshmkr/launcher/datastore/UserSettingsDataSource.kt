package net.wshmkr.launcher.datastore

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.wshmkr.launcher.model.HomeWidgetSettings
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserSettingsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.userSettingsDataStore
    
    companion object {
        private val KEY_BACKGROUND_URI = stringPreferencesKey("background_uri")
        private val KEY_SHOW_CLOCK = booleanPreferencesKey("show_clock")
        private val KEY_SHOW_CALENDAR = booleanPreferencesKey("show_calendar")
        private val KEY_SHOW_WEATHER = booleanPreferencesKey("show_weather")
        private val KEY_SHOW_MEDIA = booleanPreferencesKey("show_media_controls")
        private val KEY_USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        private val KEY_USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        private val KEY_WEATHER_LOCATION_NAME = stringPreferencesKey("weather_location_name")
        private val KEY_WEATHER_LOCATION_LAT = stringPreferencesKey("weather_location_latitude")
        private val KEY_WEATHER_LOCATION_LON = stringPreferencesKey("weather_location_longitude")
    }

    val homeWidgetSettings = dataStore.data.map { preferences ->
        val defaultUse24Hour = DateFormat.is24HourFormat(context)
        val defaultUseFahrenheit = Locale.getDefault().country == "US"
        val weatherLocationName = preferences[KEY_WEATHER_LOCATION_NAME]
        val weatherLocationLatitude = preferences[KEY_WEATHER_LOCATION_LAT]?.toDoubleOrNull()
        val weatherLocationLongitude = preferences[KEY_WEATHER_LOCATION_LON]?.toDoubleOrNull()
        HomeWidgetSettings(
            showClock = preferences[KEY_SHOW_CLOCK] ?: true,
            showCalendar = preferences[KEY_SHOW_CALENDAR] ?: true,
            showWeather = preferences[KEY_SHOW_WEATHER] ?: true,
            showMediaControls = preferences[KEY_SHOW_MEDIA] ?: true,
            use24Hour = preferences[KEY_USE_24_HOUR] ?: defaultUse24Hour,
            useFahrenheit = preferences[KEY_USE_FAHRENHEIT] ?: defaultUseFahrenheit,
            weatherLocationName = weatherLocationName,
            weatherLocationLatitude = weatherLocationLatitude,
            weatherLocationLongitude = weatherLocationLongitude,
        )
    }

    suspend fun getBackgroundUri(): String? {
        return dataStore.data.map { preferences ->
            preferences[KEY_BACKGROUND_URI]
        }.first()
    }

    suspend fun setBackgroundUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) {
                preferences[KEY_BACKGROUND_URI] = uri
            } else {
                preferences.remove(KEY_BACKGROUND_URI)
            }
        }
    }

    suspend fun setHomeWidgetSettings(settings: HomeWidgetSettings) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_CLOCK] = settings.showClock
            preferences[KEY_SHOW_CALENDAR] = settings.showCalendar
            preferences[KEY_SHOW_WEATHER] = settings.showWeather
            preferences[KEY_SHOW_MEDIA] = settings.showMediaControls
            preferences[KEY_USE_24_HOUR] = settings.use24Hour
            preferences[KEY_USE_FAHRENHEIT] = settings.useFahrenheit
            if (settings.weatherLocationName != null) {
                preferences[KEY_WEATHER_LOCATION_NAME] = settings.weatherLocationName
            } else {
                preferences.remove(KEY_WEATHER_LOCATION_NAME)
            }
            if (settings.weatherLocationLatitude != null) {
                preferences[KEY_WEATHER_LOCATION_LAT] = settings.weatherLocationLatitude.toString()
            } else {
                preferences.remove(KEY_WEATHER_LOCATION_LAT)
            }
            if (settings.weatherLocationLongitude != null) {
                preferences[KEY_WEATHER_LOCATION_LON] = settings.weatherLocationLongitude.toString()
            } else {
                preferences.remove(KEY_WEATHER_LOCATION_LON)
            }
        }
    }

    suspend fun setShowClock(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_CLOCK] = enabled
        }
    }

    suspend fun setShowCalendar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_CALENDAR] = enabled
        }
    }

    suspend fun setShowWeather(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_WEATHER] = enabled
        }
    }

    suspend fun setShowMedia(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_MEDIA] = enabled
        }
    }

    suspend fun setUse24Hour(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_USE_24_HOUR] = enabled
        }
    }

    suspend fun setUseFahrenheit(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_USE_FAHRENHEIT] = enabled
        }
    }

    suspend fun setWeatherLocation(name: String, latitude: Double, longitude: Double) {
        dataStore.edit { preferences ->
            preferences[KEY_WEATHER_LOCATION_NAME] = name
            preferences[KEY_WEATHER_LOCATION_LAT] = latitude.toString()
            preferences[KEY_WEATHER_LOCATION_LON] = longitude.toString()
        }
    }

    suspend fun clearWeatherLocation() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_WEATHER_LOCATION_NAME)
            preferences.remove(KEY_WEATHER_LOCATION_LAT)
            preferences.remove(KEY_WEATHER_LOCATION_LON)
        }
    }
}
