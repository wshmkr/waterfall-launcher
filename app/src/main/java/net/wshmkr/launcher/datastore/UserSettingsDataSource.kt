package net.wshmkr.launcher.datastore

import android.content.Context
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
    }

    val homeWidgetSettings = dataStore.data.map { preferences ->
        HomeWidgetSettings(
            showClock = preferences[KEY_SHOW_CLOCK] ?: true,
            showCalendar = preferences[KEY_SHOW_CALENDAR] ?: true,
            showWeather = preferences[KEY_SHOW_WEATHER] ?: true,
            showMediaControls = preferences[KEY_SHOW_MEDIA] ?: true,
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
}
