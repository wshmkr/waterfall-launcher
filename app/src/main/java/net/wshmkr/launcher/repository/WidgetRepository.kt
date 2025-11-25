package net.wshmkr.launcher.repository

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.wshmkr.launcher.datastore.WidgetDataSource
import net.wshmkr.launcher.model.WidgetProviderAppInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val widgetDataSource: WidgetDataSource
) {
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(context, 1024)
    val packageManager: PackageManager = context.packageManager

    private val _widgetIds = MutableStateFlow<List<Int>>(emptyList())
    val widgetIds = _widgetIds.asStateFlow()

    suspend fun loadWidgets() {
        _widgetIds.value = widgetDataSource.getWidgetIds()
    }

    suspend fun addWidget(widgetId: Int) {
        widgetDataSource.addWidget(widgetId)
        loadWidgets()
    }

    suspend fun removeWidget(widgetId: Int) {
        appWidgetHost.deleteAppWidgetId(widgetId)
        widgetDataSource.removeWidget(widgetId)
        loadWidgets()
    }

    suspend fun removeAllWidgets() {
        _widgetIds.value.forEach { appWidgetHost.deleteAppWidgetId(it) }
        widgetDataSource.clearAllWidgets()
        _widgetIds.value = emptyList()
    }

    fun allocateWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun bindAppWidgetIdIfAllowed(appWidgetId: Int, provider: ComponentName): Boolean =
        appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)

    fun getWidgetProviderApps(): List<WidgetProviderAppInfo> =
        runCatching { appWidgetManager.installedProviders }
            .getOrElse { emptyList() }
            .groupBy { it.provider.packageName }
            .mapNotNull { (packageName, infos) ->
                val appInfo = runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
                WidgetProviderAppInfo(
                    packageName = packageName,
                    label = appInfo?.loadLabel(packageManager)?.toString() ?: packageName,
                    icon = appInfo?.loadIcon(packageManager),
                    widgets = infos
                )
            }
            .sortedBy { it.label.lowercase() }

    fun startListening() = appWidgetHost.startListening()
    fun stopListening() = appWidgetHost.stopListening()
}
