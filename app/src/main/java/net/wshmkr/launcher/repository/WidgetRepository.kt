package net.wshmkr.launcher.repository

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.wshmkr.launcher.datastore.WidgetDataSource
import net.wshmkr.launcher.model.WidgetProviderAppInfo
import javax.inject.Inject
import javax.inject.Singleton

private const val WIDGET_HOST_ID = 1024

@Singleton
class WidgetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val widgetDataSource: WidgetDataSource
) {
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val appWidgetHost: AppWidgetHost = AppWidgetHost(context, WIDGET_HOST_ID)
    val packageManager: PackageManager = context.packageManager

    private val _widgetIds = MutableStateFlow<List<Int>>(emptyList())
    val widgetIds = _widgetIds.asStateFlow()

    private val widgetMutations = Mutex()

    suspend fun getLastPageWidgetId(): Int? = widgetDataSource.getLastPageWidgetId()

    suspend fun setLastPageWidgetId(widgetId: Int) = widgetDataSource.setLastPageWidgetId(widgetId)

    suspend fun getStackHeightDp(): Int = widgetDataSource.getStackHeightDp()

    suspend fun setStackHeightDp(dp: Int) = widgetDataSource.setStackHeightDp(dp)

    suspend fun loadWidgets() = updateIdsOnIo { }

    suspend fun addWidget(widgetId: Int) = updateIdsOnIo {
        if (!widgetDataSource.addWidget(widgetId)) {
            appWidgetHost.deleteAppWidgetId(widgetId)
        }
    }

    suspend fun removeWidget(widgetId: Int) = updateIdsOnIo {
        widgetDataSource.removeWidget(widgetId)
        appWidgetHost.deleteAppWidgetId(widgetId)
    }

    suspend fun commitWidgetOrder(orderedIds: List<Int>) = updateIdsOnIo {
        widgetDataSource.setWidgetOrder(orderedIds)
    }

    fun allocateWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun deleteWidgetId(widgetId: Int) = appWidgetHost.deleteAppWidgetId(widgetId)

    fun createWidgetView(context: Context, widgetId: Int): AppWidgetHostView? {
        val info = appWidgetManager.getAppWidgetInfo(widgetId) ?: return null
        return appWidgetHost.createView(context, widgetId, info)
    }

    fun bindAppWidgetIdIfAllowed(appWidgetId: Int, provider: ComponentName): Boolean =
        appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)

    fun getWidgetProviderApps(): List<WidgetProviderAppInfo> =
        runCatching { appWidgetManager.installedProviders }
            .getOrElse { emptyList() }
            .groupBy { it.provider.packageName }
            .map { (packageName, infos) ->
                val appInfo = runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
                WidgetProviderAppInfo(
                    packageName = packageName,
                    label = appInfo?.loadLabel(packageManager)?.toString() ?: packageName,
                    icon = appInfo?.loadIcon(packageManager),
                    widgets = infos.toImmutableList()
                )
            }
            .sortedBy { it.label.lowercase() }

    fun startListening() = appWidgetHost.startListening()
    fun stopListening() = appWidgetHost.stopListening()

    private suspend fun updateIdsOnIo(block: suspend () -> Unit) {
        widgetMutations.withLock {
            try {
                _widgetIds.value = withContext(Dispatchers.IO) {
                    block()
                    widgetDataSource.getWidgetIds()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("WidgetRepository", "Failed to update widget ids", e)
            }
        }
    }
}
