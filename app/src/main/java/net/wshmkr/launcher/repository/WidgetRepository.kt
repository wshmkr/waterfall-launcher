package net.wshmkr.launcher.repository

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.wshmkr.launcher.datastore.WidgetDataSource
import net.wshmkr.launcher.model.WidgetInfo
import net.wshmkr.launcher.model.WidgetProviderApp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val widgetDataSource: WidgetDataSource
) {
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val appWidgetHost: AppWidgetHost = AppWidgetHost(context, 1024)

    private val _widgets = MutableStateFlow<List<WidgetInfo>>(emptyList())
    val widgets: StateFlow<List<WidgetInfo>> = _widgets.asStateFlow()

    suspend fun loadWidgets() {
        val loadedWidgets = widgetDataSource.getWidgets()
        _widgets.value = loadedWidgets
    }

    suspend fun addWidget(widgetInfo: WidgetInfo) {
        widgetDataSource.addWidget(widgetInfo)
        loadWidgets()
    }

    suspend fun removeWidget(widgetId: Int) {
        appWidgetHost.deleteAppWidgetId(widgetId)
        widgetDataSource.removeWidget(widgetId)
        loadWidgets()
    }

    suspend fun removeAllWidgets() {
        _widgets.value.forEach { widget ->
            appWidgetHost.deleteAppWidgetId(widget.widgetId)
        }
        widgetDataSource.clearAllWidgets()
        _widgets.value = emptyList()
    }

    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun getAppWidgetHost(): AppWidgetHost {
        return appWidgetHost
    }

    fun getAppWidgetManager(): AppWidgetManager {
        return appWidgetManager
    }

    fun getAppWidgetInfo(widgetId: Int): AppWidgetProviderInfo? {
        return appWidgetManager.getAppWidgetInfo(widgetId)
    }

    fun getPackageManager(): android.content.pm.PackageManager {
        return context.packageManager
    }

    fun bindAppWidgetIdIfAllowed(appWidgetId: Int, provider: android.content.ComponentName): Boolean {
        return appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)
    }

    fun getWidgetProviderApps(): List<WidgetProviderApp> {
        val packageManager = context.packageManager
        val providers = try {
            appWidgetManager.installedProviders ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return providers
            .groupBy { it.provider.packageName }
            .mapNotNull { (packageName, infos) ->
                val applicationInfo = try {
                    packageManager.getApplicationInfo(packageName, 0)
                } catch (e: Exception) {
                    null
                }

                val label = when {
                    applicationInfo != null -> applicationInfo.loadLabel(packageManager).toString()
                    else -> null
                } ?: packageName

                val icon = applicationInfo?.loadIcon(packageManager)

                WidgetProviderApp(
                    packageName = packageName,
                    label = label,
                    icon = icon,
                    widgets = infos
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun stopListening() {
        appWidgetHost.stopListening()
    }

    fun startListening() {
        appWidgetHost.startListening()
    }
}

