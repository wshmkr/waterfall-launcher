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
import kotlinx.coroutines.flow.map
import net.wshmkr.launcher.model.WidgetInfo
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_settings")

@Singleton
class WidgetDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.widgetDataStore

    companion object {
        private fun widgetIdKey(index: Int) = intPreferencesKey("widget_${index}_id")
        private fun widgetProviderKey(index: Int) = stringPreferencesKey("widget_${index}_provider")
        private fun widgetLabelKey(index: Int) = stringPreferencesKey("widget_${index}_label")
        private fun widgetMinWidthKey(index: Int) = intPreferencesKey("widget_${index}_min_width")
        private fun widgetMinHeightKey(index: Int) = intPreferencesKey("widget_${index}_min_height")
        private val WIDGET_COUNT_KEY = intPreferencesKey("widget_count")
        
        const val MAX_WIDGETS = 10
    }

    suspend fun getWidgets(): List<WidgetInfo> {
        return dataStore.data.map { preferences ->
            val count = preferences[WIDGET_COUNT_KEY] ?: 0
            (0 until count).mapNotNull { index ->
                val widgetId = preferences[widgetIdKey(index)] ?: return@mapNotNull null
                val provider = preferences[widgetProviderKey(index)] ?: return@mapNotNull null
                val label = preferences[widgetLabelKey(index)] ?: ""
                val minWidth = preferences[widgetMinWidthKey(index)] ?: 0
                val minHeight = preferences[widgetMinHeightKey(index)] ?: 0
                
                WidgetInfo(
                    widgetId = widgetId,
                    providerName = provider,
                    minWidth = minWidth,
                    minHeight = minHeight,
                    label = label
                )
            }
        }.first()
    }

    suspend fun addWidget(widget: WidgetInfo) {
        dataStore.edit { preferences ->
            val count = preferences[WIDGET_COUNT_KEY] ?: 0
            if (count < MAX_WIDGETS) {
                preferences[widgetIdKey(count)] = widget.widgetId
                preferences[widgetProviderKey(count)] = widget.providerName
                preferences[widgetLabelKey(count)] = widget.label
                preferences[widgetMinWidthKey(count)] = widget.minWidth
                preferences[widgetMinHeightKey(count)] = widget.minHeight
                preferences[WIDGET_COUNT_KEY] = count + 1
            }
        }
    }

    suspend fun removeWidget(widgetId: Int) {
        dataStore.edit { preferences ->
            val count = preferences[WIDGET_COUNT_KEY] ?: 0
            val widgets = (0 until count).mapNotNull { index ->
                val id = preferences[widgetIdKey(index)] ?: return@mapNotNull null
                if (id == widgetId) return@mapNotNull null
                
                WidgetInfo(
                    widgetId = id,
                    providerName = preferences[widgetProviderKey(index)] ?: "",
                    minWidth = preferences[widgetMinWidthKey(index)] ?: 0,
                    minHeight = preferences[widgetMinHeightKey(index)] ?: 0,
                    label = preferences[widgetLabelKey(index)] ?: ""
                )
            }
            
            // Clear all widget data
            (0 until count).forEach { index ->
                preferences.remove(widgetIdKey(index))
                preferences.remove(widgetProviderKey(index))
                preferences.remove(widgetLabelKey(index))
                preferences.remove(widgetMinWidthKey(index))
                preferences.remove(widgetMinHeightKey(index))
            }
            
            // Re-add remaining widgets
            widgets.forEachIndexed { index, widget ->
                preferences[widgetIdKey(index)] = widget.widgetId
                preferences[widgetProviderKey(index)] = widget.providerName
                preferences[widgetLabelKey(index)] = widget.label
                preferences[widgetMinWidthKey(index)] = widget.minWidth
                preferences[widgetMinHeightKey(index)] = widget.minHeight
            }
            
            preferences[WIDGET_COUNT_KEY] = widgets.size
        }
    }

    suspend fun clearAllWidgets() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

