package net.wshmkr.launcher.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Singleton
class WidgetDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore = context.widgetDataStore

    companion object {
        private val WIDGETS_KEY = stringPreferencesKey("widgets")
        private val KEY_STACK_LAST_WIDGET = intPreferencesKey("widget_stack_last_widget")
        private val KEY_STACK_HEIGHT_DP = intPreferencesKey("widget_stack_height_dp")
        const val MAX_WIDGETS = 10
        const val MIN_STACK_HEIGHT_DP = 64
        const val DEFAULT_STACK_HEIGHT_DP = 144
        const val MAX_STACK_HEIGHT_DP = 400
    }

    suspend fun getLastPageWidgetId(): Int? {
        return readPreferences()[KEY_STACK_LAST_WIDGET]
    }

    suspend fun getWidgetIds(): List<Int> {
        return decode(readPreferences()[WIDGETS_KEY])
    }

    suspend fun getStackHeightDp(): Int {
        return readPreferences()[KEY_STACK_HEIGHT_DP] ?: DEFAULT_STACK_HEIGHT_DP
    }

    private suspend fun readPreferences(): Preferences =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .first()

    suspend fun addWidget(widgetId: Int) = updateWidgets { ids ->
        if (ids.size < MAX_WIDGETS) ids + widgetId else ids
    }

    suspend fun removeWidget(widgetId: Int) = updateWidgets { ids ->
        ids.filter { it != widgetId }
    }

    suspend fun setLastPageWidgetId(widgetId: Int) {
        dataStore.edit { it[KEY_STACK_LAST_WIDGET] = widgetId }
    }

    suspend fun setStackHeightDp(dp: Int) {
        dataStore.edit { it[KEY_STACK_HEIGHT_DP] = dp }
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
