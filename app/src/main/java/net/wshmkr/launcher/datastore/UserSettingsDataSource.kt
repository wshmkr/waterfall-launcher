package net.wshmkr.launcher.datastore

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private val defaultUse24Hour: Boolean get() = DateFormat.is24HourFormat(context)
    private val defaultUseFahrenheit: Boolean get() = Locale.getDefault().country == "US"

    companion object {
        private val KEY_BACKGROUND_URI = stringPreferencesKey("background_uri")
        private val KEY_SHOW_CLOCK = booleanPreferencesKey("show_clock")
        private val KEY_SHOW_CALENDAR = booleanPreferencesKey("show_calendar")
        private val KEY_SHOW_CALENDAR_EVENTS = booleanPreferencesKey("show_calendar_events")
        private val KEY_SHOW_WEATHER = booleanPreferencesKey("show_weather")
        private val KEY_SHOW_MEDIA = booleanPreferencesKey("show_media_controls")
        private val KEY_USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        private val KEY_USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        private val KEY_WEATHER_LOCATION_NAME = stringPreferencesKey("weather_location_name")
        private val KEY_WEATHER_LOCATION_LAT = doublePreferencesKey("weather_location_latitude")
        private val KEY_WEATHER_LOCATION_LON = doublePreferencesKey("weather_location_longitude")
    }

    val showClock: Flow<Boolean> = perField(KEY_SHOW_CLOCK) { true }
    val showCalendar: Flow<Boolean> = perField(KEY_SHOW_CALENDAR) { true }
    val showCalendarEvents: Flow<Boolean> = perField(KEY_SHOW_CALENDAR_EVENTS) { true }
    val showWeather: Flow<Boolean> = perField(KEY_SHOW_WEATHER) { true }
    val showMediaControls: Flow<Boolean> = perField(KEY_SHOW_MEDIA) { true }
    val use24Hour: Flow<Boolean> = perField(KEY_USE_24_HOUR) { defaultUse24Hour }
    val useFahrenheit: Flow<Boolean> = perField(KEY_USE_FAHRENHEIT) { defaultUseFahrenheit }
    val weatherLocationName: Flow<String?> = optionalField(KEY_WEATHER_LOCATION_NAME)
    val weatherLat: Flow<Double?> = optionalField(KEY_WEATHER_LOCATION_LAT)
    val weatherLon: Flow<Double?> = optionalField(KEY_WEATHER_LOCATION_LON)

    val homeWidgetSettings: Flow<HomeWidgetSettings> = dataStore.data
        .map { preferences ->
            HomeWidgetSettings(
                showClock = preferences[KEY_SHOW_CLOCK] ?: true,
                showCalendar = preferences[KEY_SHOW_CALENDAR] ?: true,
                showCalendarEvents = preferences[KEY_SHOW_CALENDAR_EVENTS] ?: true,
                showWeather = preferences[KEY_SHOW_WEATHER] ?: true,
                showMediaControls = preferences[KEY_SHOW_MEDIA] ?: true,
                use24Hour = preferences[KEY_USE_24_HOUR] ?: defaultUse24Hour,
                useFahrenheit = preferences[KEY_USE_FAHRENHEIT] ?: defaultUseFahrenheit,
                weatherLocationName = preferences[KEY_WEATHER_LOCATION_NAME],
                weatherLocationLatitude = preferences[KEY_WEATHER_LOCATION_LAT],
                weatherLocationLongitude = preferences[KEY_WEATHER_LOCATION_LON],
            )
        }
        .distinctUntilChanged()

    val backgroundUri: Flow<String?> = optionalField(KEY_BACKGROUND_URI)

    private fun <T> perField(key: Preferences.Key<T>, default: () -> T): Flow<T> =
        dataStore.data.map { it[key] ?: default() }.distinctUntilChanged()

    private fun <T> optionalField(key: Preferences.Key<T>): Flow<T?> =
        dataStore.data.map { it[key] }.distinctUntilChanged()

    suspend fun setBackgroundUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) {
                preferences[KEY_BACKGROUND_URI] = uri
            } else {
                preferences.remove(KEY_BACKGROUND_URI)
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

    suspend fun setShowCalendarEvents(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_CALENDAR_EVENTS] = enabled
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
            preferences[KEY_WEATHER_LOCATION_LAT] = latitude
            preferences[KEY_WEATHER_LOCATION_LON] = longitude
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
