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
import org.json.JSONArray
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
        val jsonString = dataStore.data.map { preferences ->
            preferences[KEY_USAGE_DATA]
        }.first() ?: return emptyList()

        return try {
            val jsonArray = JSONArray(jsonString)
            val usageList = mutableListOf<String>()

            for (i in 0 until jsonArray.length()) {
                usageList.add(jsonArray.getString(i))
            }

            usageList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun recordAppLaunch(packageName: String) {
        val usageList = getUsageList()
        val deque = ArrayDeque(usageList)

        // Add to front (most recent)
        deque.addFirst(packageName)

        // Remove old records if over limit
        while (deque.size > MAX_LAUNCHES_TO_TRACK) {
            deque.removeLast()
        }

        saveUsageList(deque.toList())
    }
    
    private suspend fun saveUsageList(usageList: List<String>) {
        val jsonArray = JSONArray()

        for (packageName in usageList) {
            jsonArray.put(packageName)
        }

        dataStore.edit { preferences ->
            preferences[KEY_USAGE_DATA] = jsonArray.toString()
        }
    }
}
