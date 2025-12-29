package net.wshmkr.launcher.datastore

import android.content.Context
import android.os.UserHandle
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

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.appPreferencesDataStore
    
    companion object {
        private const val KEY_NAME_FAVORITES = "favorites"
        private const val KEY_NAME_DO_NOT_SUGGEST = "do_not_suggest"
        private const val KEY_NAME_HIDDEN = "hidden"
    }

    suspend fun getFavorites(userHandle: UserHandle): Set<String> {
        return getPackageNameSet(keyForUser(KEY_NAME_FAVORITES, userHandle))
    }
    
    suspend fun addToFavorites(packageName: String, userHandle: UserHandle) {
        addToPackageNameSet(keyForUser(KEY_NAME_FAVORITES, userHandle), packageName)
    }
    
    suspend fun removeFromFavorites(packageName: String, userHandle: UserHandle) {
        removeFromPackageNameSet(keyForUser(KEY_NAME_FAVORITES, userHandle), packageName)
    }
    
    suspend fun isFavorite(packageName: String, userHandle: UserHandle): Boolean {
        return getFavorites(userHandle).contains(packageName)
    }
    
    suspend fun getDoNotSuggest(userHandle: UserHandle): Set<String> {
        return getPackageNameSet(keyForUser(KEY_NAME_DO_NOT_SUGGEST, userHandle))
    }
    
    suspend fun addToDoNotSuggest(packageName: String, userHandle: UserHandle) {
        addToPackageNameSet(keyForUser(KEY_NAME_DO_NOT_SUGGEST, userHandle), packageName)
    }
    
    suspend fun removeFromDoNotSuggest(packageName: String, userHandle: UserHandle) {
        removeFromPackageNameSet(keyForUser(KEY_NAME_DO_NOT_SUGGEST, userHandle), packageName)
    }
    
    suspend fun isDoNotSuggest(packageName: String, userHandle: UserHandle): Boolean {
        return getDoNotSuggest(userHandle).contains(packageName)
    }
    
    suspend fun getHidden(userHandle: UserHandle): Set<String> {
        return getPackageNameSet(keyForUser(KEY_NAME_HIDDEN, userHandle))
    }
    
    suspend fun addToHidden(packageName: String, userHandle: UserHandle) {
        addToPackageNameSet(keyForUser(KEY_NAME_HIDDEN, userHandle), packageName)
    }
    
    suspend fun removeFromHidden(packageName: String, userHandle: UserHandle) {
        removeFromPackageNameSet(keyForUser(KEY_NAME_HIDDEN, userHandle), packageName)
    }
    
    suspend fun isHidden(packageName: String, userHandle: UserHandle): Boolean {
        return getHidden(userHandle).contains(packageName)
    }

    private fun keyForUser(baseName: String, userHandle: UserHandle): Preferences.Key<String> {
        return stringPreferencesKey("${baseName}_${userHandle.hashCode()}")
    }

    private suspend fun getPackageNameSet(key: Preferences.Key<String>): Set<String> {
        val jsonString = dataStore.data.map { preferences ->
            preferences[key]
        }.first() ?: return emptySet()
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val packageNames = mutableSetOf<String>()
            
            for (i in 0 until jsonArray.length()) {
                packageNames.add(jsonArray.getString(i))
            }
            
            packageNames
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }
    
    private suspend fun savePackageNameSet(key: Preferences.Key<String>, packageNames: Set<String>) {
        val jsonArray = JSONArray()
        
        for (packageName in packageNames) {
            jsonArray.put(packageName)
        }
        
        dataStore.edit { preferences ->
            preferences[key] = jsonArray.toString()
        }
    }
    
    private suspend fun addToPackageNameSet(key: Preferences.Key<String>, packageName: String) {
        val currentSet = getPackageNameSet(key).toMutableSet()
        currentSet.add(packageName)
        savePackageNameSet(key, currentSet)
    }
    
    private suspend fun removeFromPackageNameSet(key: Preferences.Key<String>, packageName: String) {
        val currentSet = getPackageNameSet(key).toMutableSet()
        currentSet.remove(packageName)
        savePackageNameSet(key, currentSet)
    }
}
