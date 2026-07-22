package net.wshmkr.launcher.repository

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.wshmkr.launcher.datastore.AppPreferencesDataSource
import net.wshmkr.launcher.datastore.UsageDataSource
import net.wshmkr.launcher.model.AppInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppsRepository @Inject constructor(
    private val application: Application,
    private val usageDataSource: UsageDataSource,
    private val appPreferencesDataSource: AppPreferencesDataSource
) {
    private val launcherApps: LauncherApps = application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager: UserManager = application.getSystemService(Context.USER_SERVICE) as UserManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val allApps = mutableStateListOf<AppInfo>()
    val mostUsedApps = mutableStateListOf<String>()

    private val _activeProfiles = MutableStateFlow<ImmutableSet<UserHandle>>(persistentSetOf())
    val activeProfiles = _activeProfiles.asStateFlow()

    private val appComparator =
        compareBy<AppInfo> { it.label.lowercase() }.thenBy { it.userHandle.hashCode() }

    private val profileStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateActiveProfiles()
        }
    }

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            scope.launch { syncPackage(packageName, user) }
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            removePackage(packageName, user)
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            scope.launch { syncPackage(packageName, user) }
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            scope.launch { packageNames.forEach { syncPackage(it, user) } }
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            packageNames.forEach { removePackage(it, user) }
        }
    }

    init {
        updateActiveProfiles()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
        }
        application.registerReceiver(profileStateReceiver, filter)
        launcherApps.registerCallback(launcherAppsCallback)
    }

    private fun updateActiveProfiles() {
        val userHandles = userManager.userProfiles.takeIf { it.isNotEmpty() }
            ?: listOf(Process.myUserHandle())
        _activeProfiles.value = userHandles.filter { isProfileActive(it) }.toPersistentSet()
    }

    fun isProfileActive(userHandle: UserHandle): Boolean {
        if (userHandle == Process.myUserHandle()) {
            return true
        }

        return try {
            !userManager.isQuietModeEnabled(userHandle)
        } catch (e: Exception) {
            true
        }
    }

    suspend fun loadInstalledApps() {
        val userHandles = userManager.userProfiles.takeIf { it.isNotEmpty() } ?: listOf(Process.myUserHandle())

        val apps = withContext(Dispatchers.IO) {
            val seen = mutableSetOf<Pair<String, UserHandle>>()
            buildList {
                for (userHandle in userHandles) {
                    val favorites = appPreferencesDataSource.favorites.get(userHandle)
                    val hidden = appPreferencesDataSource.hidden.get(userHandle)
                    val doNotSuggest = appPreferencesDataSource.doNotSuggest.get(userHandle)

                    val activities = launcherApps.getActivityList(null, userHandle)

                    for (activity in activities) {
                        val appPackageName = activity.componentName.packageName
                        if (seen.add(appPackageName to userHandle) && appPackageName != application.packageName) {
                            add(buildAppInfo(activity, userHandle, favorites, hidden, doNotSuggest))
                        }
                    }
                }
            }.sortedWith(appComparator)
        }

        Snapshot.withMutableSnapshot {
            allApps.clear()
            allApps.addAll(apps)
        }
    }

    private fun buildAppInfo(
        activity: LauncherActivityInfo,
        userHandle: UserHandle,
        favorites: Set<String>,
        hidden: Set<String>,
        doNotSuggest: Set<String>,
    ): AppInfo {
        val appPackageName = activity.componentName.packageName
        val isSystemApp = activity.applicationInfo.flags.and(ApplicationInfo.FLAG_SYSTEM) != 0
        val label = activity.label.toString()

        return AppInfo(
            label = label,
            packageName = appPackageName,
            icon = toBitmapPainter(activity.getBadgedIcon(0)),
            userHandle = userHandle,
            isSystemApp = isSystemApp,
            isFavorite = favorites.contains(appPackageName),
            isHidden = hidden.contains(appPackageName),
            doNotSuggest = doNotSuggest.contains(appPackageName),
            searchTokens = buildSearchTokens(label),
        )
    }

    private fun toBitmapPainter(drawable: Drawable): BitmapPainter {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: FALLBACK_ICON_SIZE_PX
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: FALLBACK_ICON_SIZE_PX
        return BitmapPainter(drawable.toBitmap(width = width, height = height).asImageBitmap())
    }

    private companion object {
        const val FALLBACK_ICON_SIZE_PX = 96
    }

    private fun buildSearchTokens(label: String) =
        label.lowercase()
            .split(' ')
            .filter { it.isNotEmpty() }
            .toImmutableList()

    private suspend fun syncPackage(packageName: String, userHandle: UserHandle) {
        if (packageName == application.packageName) return

        val updated = withContext(Dispatchers.IO) {
            val activity = try {
                launcherApps.getActivityList(packageName, userHandle)?.firstOrNull()
            } catch (_: Exception) {
                null
            } ?: return@withContext null

            buildAppInfo(
                activity = activity,
                userHandle = userHandle,
                favorites = appPreferencesDataSource.favorites.get(userHandle),
                hidden = appPreferencesDataSource.hidden.get(userHandle),
                doNotSuggest = appPreferencesDataSource.doNotSuggest.get(userHandle),
            )
        }

        Snapshot.withMutableSnapshot {
            allApps.removeAll { it.packageName == packageName && it.userHandle == userHandle }
            if (updated != null) {
                allApps.add(updated)
                allApps.sortWith(appComparator)
            }
        }
    }

    private fun removePackage(packageName: String, userHandle: UserHandle) {
        allApps.removeAll { it.packageName == packageName && it.userHandle == userHandle }
    }

    suspend fun recordAppLaunch(packageName: String, userHandle: UserHandle) {
        val usageList = usageDataSource.recordAppLaunch(packageName, userHandle)
        updateMostUsedApps(usageList)
    }

    suspend fun refreshAppIcons(profiles: Set<UserHandle>) {
        val appsToRefresh = allApps.filter { it.userHandle in profiles }

        val updatedIcons: Map<String, BitmapPainter> = withContext(Dispatchers.IO) {
            appsToRefresh.mapNotNull { app ->
                try {
                    launcherApps.getActivityList(app.packageName, app.userHandle)
                        ?.firstOrNull()
                        ?.getBadgedIcon(0)
                        ?.let { app.key to toBitmapPainter(it) }
                } catch (_: Exception) {
                    null
                }
            }.toMap()
        }

        Snapshot.withMutableSnapshot {
            for (index in allApps.indices) {
                val app = allApps[index]
                updatedIcons[app.key]?.let { allApps[index] = app.copy(icon = it) }
            }
        }
    }

    suspend fun updateMostUsedApps() = updateMostUsedApps(usageDataSource.getUsageList())

    private suspend fun updateMostUsedApps(usageList: List<String>) {
        val sortedByUsage = usageList
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

        Snapshot.withMutableSnapshot {
            mostUsedApps.clear()
            mostUsedApps.addAll(sortedByUsage)
        }
    }

    suspend fun toggleFavorite(packageName: String, userHandle: UserHandle) {
        togglePackageFlag(packageName, userHandle, appPreferencesDataSource.favorites,
            isSet = { it.isFavorite },
            withFlag = { app, value -> app.copy(isFavorite = value) })
    }

    suspend fun toggleHidden(packageName: String, userHandle: UserHandle) {
        togglePackageFlag(packageName, userHandle, appPreferencesDataSource.hidden,
            isSet = { it.isHidden },
            withFlag = { app, value -> app.copy(isHidden = value) })
    }

    suspend fun toggleSuggest(packageName: String, userHandle: UserHandle) {
        togglePackageFlag(packageName, userHandle, appPreferencesDataSource.doNotSuggest,
            isSet = { it.doNotSuggest },
            withFlag = { app, value -> app.copy(doNotSuggest = value, isSuggested = app.isSuggested && !value) })
    }

    private suspend fun togglePackageFlag(
        packageName: String,
        userHandle: UserHandle,
        store: AppPreferencesDataSource.PackageNameSetStore,
        isSet: (AppInfo) -> Boolean,
        withFlag: (AppInfo, Boolean) -> AppInfo,
    ) {
        val index = allApps.indexOfFirst { it.packageName == packageName && it.userHandle == userHandle }
        if (index == -1) return

        val app = allApps[index]
        val enable = !isSet(app)
        if (enable) {
            store.add(packageName, userHandle)
        } else {
            store.remove(packageName, userHandle)
        }
        allApps[index] = withFlag(app, enable)
    }
}
