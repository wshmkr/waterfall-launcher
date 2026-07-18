package net.wshmkr.launcher.repository

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import androidx.compose.runtime.mutableStateListOf
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
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

    val activeProfiles = MutableStateFlow<Set<UserHandle>>(emptySet())

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
        val activeSet = userHandles.filter { isProfileActive(it) }.toSet()
        activeProfiles.value = activeSet
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
        val seen = mutableSetOf<Pair<String, UserHandle>>()

        allApps.clear()
        
        for (userHandle in userHandles) {
            val favorites = appPreferencesDataSource.getFavorites(userHandle)
            val hidden = appPreferencesDataSource.getHidden(userHandle)
            val doNotSuggest = appPreferencesDataSource.getDoNotSuggest(userHandle)

            val activities = launcherApps.getActivityList(null, userHandle)

            for (activity in activities) {
                val appPackageName = activity.componentName.packageName

                if (!seen.add(appPackageName to userHandle)) continue
                if (appPackageName == application.packageName) continue

                allApps.add(buildAppInfo(activity, userHandle, favorites, hidden, doNotSuggest))
            }
        }

        allApps.sortWith(appComparator)
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

        return AppInfo(
            label = activity.label.toString(),
            packageName = appPackageName,
            icon = activity.getBadgedIcon(0),
            userHandle = userHandle,
            isSystemApp = isSystemApp,
            isFavorite = favorites.contains(appPackageName),
            isHidden = hidden.contains(appPackageName),
            doNotSuggest = doNotSuggest.contains(appPackageName),
        )
    }

    private suspend fun syncPackage(packageName: String, userHandle: UserHandle) {
        if (packageName == application.packageName) return

        allApps.removeAll { it.packageName == packageName && it.userHandle == userHandle }

        val activity = try {
            launcherApps.getActivityList(packageName, userHandle)?.firstOrNull()
        } catch (_: Exception) {
            null
        } ?: return

        val favorites = appPreferencesDataSource.getFavorites(userHandle)
        val hidden = appPreferencesDataSource.getHidden(userHandle)
        val doNotSuggest = appPreferencesDataSource.getDoNotSuggest(userHandle)

        allApps.add(buildAppInfo(activity, userHandle, favorites, hidden, doNotSuggest))
        allApps.sortWith(appComparator)
    }

    private fun removePackage(packageName: String, userHandle: UserHandle) {
        allApps.removeAll { it.packageName == packageName && it.userHandle == userHandle }
    }

    suspend fun recordAppLaunch(packageName: String, userHandle: UserHandle) {
        usageDataSource.recordAppLaunch(packageName, userHandle)
        updateMostUsedApps()
    }

    fun refreshAppIcons(profiles: Set<UserHandle>) {
        for (index in allApps.indices) {
            val app = allApps[index]
            if (app.userHandle !in profiles) continue
            
            try {
                val activityInfo = launcherApps
                    .getActivityList(app.packageName, app.userHandle)
                    ?.firstOrNull()
                val updatedIcon = activityInfo?.getBadgedIcon(0) ?: continue
                allApps[index] = app.copy(icon = updatedIcon)
            } catch (_: Exception) {
                // ignore and keep current icon
            }
        }
    }

    suspend fun updateMostUsedApps() {
        val usageList = usageDataSource.getUsageList()

        val usageCount = mutableMapOf<String, Int>()
        for (key in usageList) {
            usageCount[key] = (usageCount[key] ?: 0) + 1
        }

        val sortedByUsage = usageCount.entries
            .sortedByDescending { it.value }
            .map { it.key }
        
        mostUsedApps.clear()
        mostUsedApps.addAll(sortedByUsage)
    }

    suspend fun toggleFavorite(packageName: String, userHandle: UserHandle) {
        val index = allApps.indexOfFirst { it.packageName == packageName && it.userHandle == userHandle }
        if (index == -1) return
        
        val app = allApps[index]
        if (appPreferencesDataSource.isFavorite(packageName, userHandle)) {
            appPreferencesDataSource.removeFromFavorites(packageName, userHandle)
            allApps[index] = app.copy(isFavorite = false)
        } else {
            appPreferencesDataSource.addToFavorites(packageName, userHandle)
            allApps[index] = app.copy(isFavorite = true)
        }
    }

    suspend fun toggleHidden(packageName: String, userHandle: UserHandle) {
        val index = allApps.indexOfFirst { it.packageName == packageName && it.userHandle == userHandle }
        if (index == -1) return
        
        val app = allApps[index]
        if (appPreferencesDataSource.isHidden(packageName, userHandle)) {
            appPreferencesDataSource.removeFromHidden(packageName, userHandle)
            allApps[index] = app.copy(isHidden = false)
        } else {
            appPreferencesDataSource.addToHidden(packageName, userHandle)
            allApps[index] = app.copy(isHidden = true)
        }
    }

    suspend fun toggleSuggest(packageName: String, userHandle: UserHandle) {
        val index = allApps.indexOfFirst { it.packageName == packageName && it.userHandle == userHandle }
        if (index == -1) return
        
        val app = allApps[index]
        if (appPreferencesDataSource.isDoNotSuggest(packageName, userHandle)) {
            appPreferencesDataSource.removeFromDoNotSuggest(packageName, userHandle)
            allApps[index] = app.copy(doNotSuggest = false)
        } else {
            appPreferencesDataSource.addToDoNotSuggest(packageName, userHandle)
            allApps[index] = app.copy(doNotSuggest = true, isSuggested = false)
        }
    }
}
