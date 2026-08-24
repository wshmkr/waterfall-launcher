package net.wshmkr.launcher.repository

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
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
    val appWidgetHost: AppWidgetHost = AppWidgetHost(context, WIDGET_HOST_ID)
    val packageManager: PackageManager = context.packageManager

    private val _widgetIds = MutableStateFlow<List<Int>>(emptyList())
    val widgetIds = _widgetIds.asStateFlow()

    private var persistedWidgetIds: List<Int> = emptyList()

    private val widgetMutations = Mutex()

    suspend fun getLastPageWidgetId(): Int? = widgetDataSource.getLastPageWidgetId()

    suspend fun setLastPageWidgetId(widgetId: Int) = widgetDataSource.setLastPageWidgetId(widgetId)

    suspend fun getStackHeightDp(): Int = widgetDataSource.getStackHeightDp()

    suspend fun setStackHeightDp(dp: Int) = widgetDataSource.setStackHeightDp(dp)

    suspend fun loadWidgets() = updateIdsOnIo { }

    suspend fun addWidget(widgetId: Int) = updateIdsOnIo {
        widgetDataSource.addWidget(widgetId)
    }

    suspend fun removeWidget(widgetId: Int) = updateIdsOnIo {
        appWidgetHost.deleteAppWidgetId(widgetId)
        widgetDataSource.removeWidget(widgetId)
    }

    fun previewWidgetOrder(ids: List<Int>) {
        _widgetIds.value = ids
    }

    suspend fun commitWidgetOrder() = widgetMutations.withLock {
        val ids = _widgetIds.value
        if (ids == persistedWidgetIds) return@withLock
        withContext(Dispatchers.IO) {
            widgetDataSource.setWidgetOrder(ids)
        }
        // Only marked persisted after the write succeeds, so a failed commit stays retryable.
        persistedWidgetIds = ids
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
                    widgets = infos.toImmutableList()
                )
            }
            .sortedBy { it.label.lowercase() }

    fun startListening() = appWidgetHost.startListening()
    fun stopListening() = appWidgetHost.stopListening()

    private suspend fun updateIdsOnIo(block: suspend () -> Unit) = widgetMutations.withLock {
        val updated = withContext(Dispatchers.IO) {
            block()
            widgetDataSource.getWidgetIds()
        }
        persistedWidgetIds = updated
        _widgetIds.value = updated
    }
}
