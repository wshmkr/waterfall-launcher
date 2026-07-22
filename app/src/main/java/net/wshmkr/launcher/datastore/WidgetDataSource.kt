package net.wshmkr.launcher.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        private val KEY_STACK_LAST_PAGE = intPreferencesKey("widget_stack_last_page")
        const val MAX_WIDGETS = 10
    }

    val lastPageIndex: Flow<Int> = dataStore.data.map { it[KEY_STACK_LAST_PAGE] ?: 0 }

    suspend fun getWidgetIds(): List<Int> {
        return decodeWidgetIds(dataStore.data.first()[WIDGETS_KEY])
    }

    suspend fun addWidget(widgetId: Int) = updateWidgets { ids ->
        if (ids.size < MAX_WIDGETS) ids + widgetId else ids
    }

    suspend fun removeWidget(widgetId: Int) = updateWidgets { ids ->
        ids.filter { it != widgetId }
    }

    suspend fun setLastPageIndex(idx: Int) {
        dataStore.edit { it[KEY_STACK_LAST_PAGE] = idx }
    }

    private suspend fun updateWidgets(transform: (List<Int>) -> List<Int>) {
        dataStore.edit { preferences ->
            val current = decodeWidgetIds(preferences[WIDGETS_KEY])
            preferences[WIDGETS_KEY] = encodeStringList(transform(current).map { it.toString() })
        }
    }

    private fun decodeWidgetIds(json: String?): List<Int> {
        return decodeStringList(json).mapNotNull { it.toIntOrNull() }
    }
}
