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

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_settings")

@Singleton
class WidgetDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.widgetDataStore

    companion object {
        private val WIDGETS_KEY = stringPreferencesKey("widgets")
        const val MAX_WIDGETS = 10
    }

    suspend fun getWidgetIds(): List<Int> {
        val jsonString = dataStore.data.map { it[WIDGETS_KEY] }.first() ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).map { jsonArray.getInt(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addWidget(widgetId: Int) = updateWidgets { ids ->
        if (ids.size < MAX_WIDGETS) ids + widgetId else ids
    }

    suspend fun removeWidget(widgetId: Int) = updateWidgets { ids ->
        ids.filter { it != widgetId }
    }

    suspend fun clearAllWidgets() {
        dataStore.edit { it.clear() }
    }

    private suspend fun updateWidgets(transform: (List<Int>) -> List<Int>) {
        val current = getWidgetIds()
        val updated = transform(current)
        val jsonArray = JSONArray().apply { updated.forEach { put(it) } }
        dataStore.edit { it[WIDGETS_KEY] = jsonArray.toString() }
    }
}
