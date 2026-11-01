package net.wshmkr.launcher.datastore

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import net.wshmkr.launcher.model.HomeWidgetSettings
import net.wshmkr.launcher.model.PaletteStyle
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Singleton
class UserSettingsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.userSettingsDataStore

    private val preferences: Flow<Preferences> = dataStore.data
        .retryWhen { cause, _ ->
            if (cause !is IOException) return@retryWhen false
            emit(emptyPreferences())
            delay(READ_RETRY_DELAY_MS)
            true
        }

    val defaultUse24Hour: Boolean get() = DateFormat.is24HourFormat(context)
    val defaultUseFahrenheit: Boolean get() = Locale.getDefault().country == "US"

    companion object {
        private const val READ_RETRY_DELAY_MS = 5_000L
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
        private val KEY_PALETTE_STYLE = stringPreferencesKey("palette_style")

        private const val DEFAULT_SHOW_WIDGET = true
    }

    val showClock: Flow<Boolean> = perField(KEY_SHOW_CLOCK) { DEFAULT_SHOW_WIDGET }
    val showCalendar: Flow<Boolean> = perField(KEY_SHOW_CALENDAR) { DEFAULT_SHOW_WIDGET }
    val showCalendarEvents: Flow<Boolean> = perField(KEY_SHOW_CALENDAR_EVENTS) { DEFAULT_SHOW_WIDGET }
    val showWeather: Flow<Boolean> = perField(KEY_SHOW_WEATHER) { DEFAULT_SHOW_WIDGET }
    val showMediaControls: Flow<Boolean> = perField(KEY_SHOW_MEDIA) { DEFAULT_SHOW_WIDGET }
    val use24Hour: Flow<Boolean> = perField(KEY_USE_24_HOUR) { defaultUse24Hour }
    val useFahrenheit: Flow<Boolean> = perField(KEY_USE_FAHRENHEIT) { defaultUseFahrenheit }

    val weatherLocation: Flow<Pair<Double, Double>?> = preferences
        .map { it.weatherLocation() }
        .distinctUntilChanged()

    // Null when no fixed location is set, since a name without coordinates is not usable.
    val weatherLocationName: Flow<String?> = preferences
        .map { prefs -> prefs[KEY_WEATHER_LOCATION_NAME]?.takeIf { prefs.weatherLocation() != null } }
        .distinctUntilChanged()

    val homeWidgetSettings: Flow<HomeWidgetSettings> = preferences
        .map { prefs ->
            HomeWidgetSettings(
                showClock = prefs[KEY_SHOW_CLOCK] ?: DEFAULT_SHOW_WIDGET,
                showCalendar = prefs[KEY_SHOW_CALENDAR] ?: DEFAULT_SHOW_WIDGET,
                showCalendarEvents = prefs[KEY_SHOW_CALENDAR_EVENTS] ?: DEFAULT_SHOW_WIDGET,
                showWeather = prefs[KEY_SHOW_WEATHER] ?: DEFAULT_SHOW_WIDGET,
                showMediaControls = prefs[KEY_SHOW_MEDIA] ?: DEFAULT_SHOW_WIDGET,
                use24Hour = prefs[KEY_USE_24_HOUR] ?: defaultUse24Hour,
                useFahrenheit = prefs[KEY_USE_FAHRENHEIT] ?: defaultUseFahrenheit,
                weatherLocationName = prefs[KEY_WEATHER_LOCATION_NAME],
                weatherLocationLatitude = prefs[KEY_WEATHER_LOCATION_LAT],
                weatherLocationLongitude = prefs[KEY_WEATHER_LOCATION_LON],
            )
        }
        .distinctUntilChanged()

    val paletteStyle: Flow<PaletteStyle> = preferences
        .map { PaletteStyle.fromName(it[KEY_PALETTE_STYLE]) }
        .distinctUntilChanged()

    private fun Preferences.weatherLocation(): Pair<Double, Double>? {
        val latitude = this[KEY_WEATHER_LOCATION_LAT] ?: return null
        val longitude = this[KEY_WEATHER_LOCATION_LON] ?: return null
        return latitude to longitude
    }

    private fun <T> perField(key: Preferences.Key<T>, default: () -> T): Flow<T> =
        preferences.map { it[key] ?: default() }.distinctUntilChanged()

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    suspend fun setShowClock(enabled: Boolean) = put(KEY_SHOW_CLOCK, enabled)

    suspend fun setShowCalendar(enabled: Boolean) = put(KEY_SHOW_CALENDAR, enabled)

    suspend fun setShowCalendarEvents(enabled: Boolean) = put(KEY_SHOW_CALENDAR_EVENTS, enabled)

    suspend fun setShowWeather(enabled: Boolean) = put(KEY_SHOW_WEATHER, enabled)

    suspend fun setShowMedia(enabled: Boolean) = put(KEY_SHOW_MEDIA, enabled)

    suspend fun setUse24Hour(enabled: Boolean) = put(KEY_USE_24_HOUR, enabled)

    suspend fun setUseFahrenheit(enabled: Boolean) = put(KEY_USE_FAHRENHEIT, enabled)

    suspend fun setPaletteStyle(style: PaletteStyle) = put(KEY_PALETTE_STYLE, style.name)

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
