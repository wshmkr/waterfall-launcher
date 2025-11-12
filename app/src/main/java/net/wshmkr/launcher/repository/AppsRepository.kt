package net.wshmkr.launcher.repository

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf
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
    private val packageManager: PackageManager = application.packageManager
    
    val allApps = mutableStateListOf<AppInfo>()
    val mostUsedApps = mutableStateListOf<String>()

    suspend fun loadInstalledApps() {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val favorites = appPreferencesDataSource.getFavorites()
        val hidden = appPreferencesDataSource.getHidden()
        val doNotSuggest = appPreferencesDataSource.getDoNotSuggest()

        allApps.clear()
        
        for (resolveInfo in activities) {
            val appPackageName = resolveInfo.activityInfo.packageName

            if (appPackageName == application.packageName) {
                continue
            }

            val applicationInfo = try {
                packageManager.getApplicationInfo(appPackageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            }
            val isSystemApp = applicationInfo.flags.and(ApplicationInfo.FLAG_SYSTEM) != 0
            
            val appInfo = AppInfo(
                label = resolveInfo.loadLabel(packageManager).toString(),
                packageName = appPackageName,
                icon = resolveInfo.loadIcon(packageManager),
                isSystemApp = isSystemApp,
                isFavorite = favorites.contains(appPackageName),
                isHidden = hidden.contains(appPackageName),
                doNotSuggest = doNotSuggest.contains(appPackageName),
            )
            allApps.add(appInfo)
        }

        allApps.sortBy { it.label.lowercase() }
    }

    suspend fun recordAppLaunch(packageName: String) {
        usageDataSource.recordAppLaunch(packageName)
        updateMostUsedApps()
    }

    suspend fun updateMostUsedApps() {
        val usageList = usageDataSource.getUsageList()

        val usageCount = mutableMapOf<String, Int>()
        for (packageName in usageList) {
            usageCount[packageName] = (usageCount[packageName] ?: 0) + 1
        }

        val sortedByUsage = usageCount.entries
            .sortedByDescending { it.value }
            .map { it.key }
        
        mostUsedApps.clear()
        mostUsedApps.addAll(sortedByUsage)
    }

    suspend fun toggleFavorite(packageName: String) {
        val index = allApps.indexOfFirst { it.packageName == packageName }
        if (index == -1) return
        
        val app = allApps[index]
        if (appPreferencesDataSource.isFavorite(packageName)) {
            appPreferencesDataSource.removeFromFavorites(packageName)
            allApps[index] = app.copy(isFavorite = false)
        } else {
            appPreferencesDataSource.addToFavorites(packageName)
            allApps[index] = app.copy(isFavorite = true)
        }
    }

    suspend fun toggleHidden(packageName: String) {
        val index = allApps.indexOfFirst { it.packageName == packageName }
        if (index == -1) return
        
        val app = allApps[index]
        if (appPreferencesDataSource.isHidden(packageName)) {
            appPreferencesDataSource.removeFromHidden(packageName)
            allApps[index] = app.copy(isHidden = false)
        } else {
            appPreferencesDataSource.addToHidden(packageName)
            allApps[index] = app.copy(isHidden = true)
        }
    }

    suspend fun toggleSuggest(packageName: String) {
        val index = allApps.indexOfFirst { it.packageName == packageName }
        if (index == -1) return
        
        val app = allApps[index]
        if (appPreferencesDataSource.isDoNotSuggest(packageName)) {
            appPreferencesDataSource.removeFromDoNotSuggest(packageName)
            allApps[index] = app.copy(doNotSuggest = false)
        } else {
            appPreferencesDataSource.addToDoNotSuggest(packageName)
            allApps[index] = app.copy(doNotSuggest = true, isSuggested = false)
        }
    }
}
