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

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.appPreferencesDataStore
    
    companion object {
        private val KEY_FAVORITES = stringPreferencesKey("favorites")
        private val KEY_DO_NOT_SUGGEST = stringPreferencesKey("do_not_suggest")
        private val KEY_HIDDEN = stringPreferencesKey("hidden")
    }
    
    suspend fun getFavorites(): Set<String> {
        return getPackageNameSet(KEY_FAVORITES)
    }
    
    suspend fun addToFavorites(packageName: String) {
        addToPackageNameSet(KEY_FAVORITES, packageName)
    }
    
    suspend fun removeFromFavorites(packageName: String) {
        removeFromPackageNameSet(KEY_FAVORITES, packageName)
    }
    
    suspend fun isFavorite(packageName: String): Boolean {
        return getFavorites().contains(packageName)
    }
    
    suspend fun getDoNotSuggest(): Set<String> {
        return getPackageNameSet(KEY_DO_NOT_SUGGEST)
    }
    
    suspend fun addToDoNotSuggest(packageName: String) {
        addToPackageNameSet(KEY_DO_NOT_SUGGEST, packageName)
    }
    
    suspend fun removeFromDoNotSuggest(packageName: String) {
        removeFromPackageNameSet(KEY_DO_NOT_SUGGEST, packageName)
    }
    
    suspend fun isDoNotSuggest(packageName: String): Boolean {
        return getDoNotSuggest().contains(packageName)
    }
    
    suspend fun getHidden(): Set<String> {
        return getPackageNameSet(KEY_HIDDEN)
    }
    
    suspend fun addToHidden(packageName: String) {
        addToPackageNameSet(KEY_HIDDEN, packageName)
    }
    
    suspend fun removeFromHidden(packageName: String) {
        removeFromPackageNameSet(KEY_HIDDEN, packageName)
    }
    
    suspend fun isHidden(packageName: String): Boolean {
        return getHidden().contains(packageName)
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
