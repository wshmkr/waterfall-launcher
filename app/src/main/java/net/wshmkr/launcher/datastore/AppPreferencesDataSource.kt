package net.wshmkr.launcher.datastore

import android.content.Context
import android.os.UserHandle
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Singleton
class AppPreferencesDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = context.appPreferencesDataStore

    val doNotSuggest = PackageNameSetStore("do_not_suggest")
    val hidden = PackageNameSetStore("hidden")

    suspend fun getFavorites(): PersistentList<String> {
        val raw = dataStore.data.first()[FAVORITES_KEY]
        if (raw.isNullOrEmpty()) return persistentListOf()
        return raw.split(FAVORITES_SEPARATOR).toPersistentList()
    }

    suspend fun setFavorites(appKeys: List<String>) {
        dataStore.edit { it[FAVORITES_KEY] = appKeys.joinToString(FAVORITES_SEPARATOR) }
    }

    inner class PackageNameSetStore internal constructor(private val baseName: String) {
        suspend fun get(userHandle: UserHandle): Set<String> {
            return dataStore.data.first()[keyForUser(userHandle)] ?: emptySet()
        }

        suspend fun add(packageName: String, userHandle: UserHandle) {
            update(userHandle) { it + packageName }
        }

        suspend fun remove(packageName: String, userHandle: UserHandle) {
            update(userHandle) { it - packageName }
        }

        private suspend fun update(userHandle: UserHandle, transform: (Set<String>) -> Set<String>) {
            dataStore.edit { preferences ->
                val key = keyForUser(userHandle)
                preferences[key] = transform(preferences[key] ?: emptySet())
            }
        }

        private fun keyForUser(userHandle: UserHandle): Preferences.Key<Set<String>> {
            return stringSetPreferencesKey("${baseName}_${userHandle.hashCode()}")
        }
    }

    private companion object {
        val FAVORITES_KEY = stringPreferencesKey("favorites")
        const val FAVORITES_SEPARATOR = ","
    }
}
