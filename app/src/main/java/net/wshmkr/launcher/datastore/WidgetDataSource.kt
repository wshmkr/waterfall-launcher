package net.wshmkr.launcher.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_settings")

@Singleton
class WidgetDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.widgetDataStore

    companion object {
        private val WIDGETS_KEY = stringPreferencesKey("widgets")
        private val KEY_STACK_LAST_WIDGET = intPreferencesKey("widget_stack_last_widget")
        const val MAX_WIDGETS = 10
    }

    suspend fun getLastPageWidgetId(): Int? {
        return dataStore.data.first()[KEY_STACK_LAST_WIDGET]
    }

    suspend fun getWidgetIds(): List<Int> {
        return decode(dataStore.data.first()[WIDGETS_KEY])
    }

    suspend fun addWidget(widgetId: Int) = updateWidgets { ids ->
        if (ids.size < MAX_WIDGETS) ids + widgetId else ids
    }

    suspend fun removeWidget(widgetId: Int) = updateWidgets { ids ->
        ids.filter { it != widgetId }
    }

    suspend fun setLastPageWidgetId(widgetId: Int) {
        dataStore.edit { it[KEY_STACK_LAST_WIDGET] = widgetId }
    }

    private suspend fun updateWidgets(transform: (List<Int>) -> List<Int>) {
        dataStore.edit { preferences ->
            val current = decode(preferences[WIDGETS_KEY])
            preferences[WIDGETS_KEY] = transform(current).joinToString(",")
        }
    }

    private fun decode(raw: String?): List<Int> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(',').mapNotNull { it.toIntOrNull() }
    }
}
