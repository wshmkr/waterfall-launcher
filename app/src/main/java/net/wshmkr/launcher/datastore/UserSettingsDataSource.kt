package net.wshmkr.launcher.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
}
