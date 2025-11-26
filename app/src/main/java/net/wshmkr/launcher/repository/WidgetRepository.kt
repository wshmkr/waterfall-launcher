package net.wshmkr.launcher.repository

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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

    suspend fun loadWidgets() = updateIdsOnIo { }

    suspend fun addWidget(widgetId: Int) = updateIdsOnIo {
        widgetDataSource.addWidget(widgetId)
    }

    suspend fun removeWidget(widgetId: Int) = updateIdsOnIo {
        appWidgetHost.deleteAppWidgetId(widgetId)
        widgetDataSource.removeWidget(widgetId)
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

    private suspend fun updateIdsOnIo(block: suspend () -> Unit) {
        val updated = withContext(Dispatchers.IO) {
            block()
            widgetDataSource.getWidgetIds()
        }
        _widgetIds.value = updated
    }
}
