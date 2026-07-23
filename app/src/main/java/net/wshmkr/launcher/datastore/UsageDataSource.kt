package net.wshmkr.launcher.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class UsageEntry(val count: Long, val lastUsed: Long)

private val Context.usageDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_usage")

private const val COUNT_PREFIX = "count_"
private const val LAST_PREFIX = "last_"

@Singleton
class UsageDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = context.usageDataStore

    suspend fun loadAll(): Map<String, UsageEntry> {
        val prefs = dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .first()
            .asMap()
        val counts = mutableMapOf<String, Long>()
        val lasts = mutableMapOf<String, Long>()
        for ((key, value) in prefs) {
            val longValue = value as? Long ?: continue
            val name = key.name
            when {
                name.startsWith(COUNT_PREFIX) -> counts[name.removePrefix(COUNT_PREFIX)] = longValue
                name.startsWith(LAST_PREFIX) -> lasts[name.removePrefix(LAST_PREFIX)] = longValue
            }
        }
        return counts.keys.associateWith { key ->
            UsageEntry(counts.getValue(key), lasts[key] ?: 0L)
        }
    }

    suspend fun flush(entries: Map<String, UsageEntry>) {
        dataStore.edit { prefs ->
            val staleKeys = prefs.asMap().keys.filter { pref ->
                val name = pref.name
                when {
                    name.startsWith(COUNT_PREFIX) -> name.removePrefix(COUNT_PREFIX) !in entries
                    name.startsWith(LAST_PREFIX) -> name.removePrefix(LAST_PREFIX) !in entries
                    else -> false
                }
            }
            for (stale in staleKeys) prefs.remove(stale)
            for ((key, entry) in entries) {
                prefs[longPreferencesKey(COUNT_PREFIX + key)] = entry.count
                prefs[longPreferencesKey(LAST_PREFIX + key)] = entry.lastUsed
            }
        }
    }
}
