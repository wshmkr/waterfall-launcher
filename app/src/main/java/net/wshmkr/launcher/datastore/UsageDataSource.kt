package net.wshmkr.launcher.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import net.wshmkr.launcher.model.keyFor
import javax.inject.Inject
import javax.inject.Singleton

private val Context.usageDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_usage")

@Singleton
class UsageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.usageDataStore

    companion object {
        private val KEY_USAGE_DATA = stringPreferencesKey("usage_data")
        private const val MAX_LAUNCHES_TO_TRACK = 100
    }

    suspend fun getUsageList(): List<String> {
        return decodeStringList(dataStore.data.first()[KEY_USAGE_DATA])
    }

    suspend fun recordAppLaunch(packageName: String, userHandle: UserHandle): List<String> {
        var updated: List<String> = emptyList()
        dataStore.edit { preferences ->
            val current = decodeStringList(preferences[KEY_USAGE_DATA])
            updated = (listOf(keyFor(packageName, userHandle)) + current).take(MAX_LAUNCHES_TO_TRACK)
            preferences[KEY_USAGE_DATA] = encodeStringList(updated)
        }
        return updated
    }
}
