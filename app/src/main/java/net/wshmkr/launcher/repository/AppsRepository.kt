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
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.wshmkr.launcher.datastore.AppPreferencesDataSource
import net.wshmkr.launcher.datastore.UsageDataSource
import net.wshmkr.launcher.datastore.UsageEntry
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.keyFor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

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

    private val usageEntries = mutableMapOf<String, UsageEntry>()
    private var usageLoaded = false
    private var usageDirty = false
    private var pendingPublish = true

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
            // Temporary unavailability (quiet mode, ejected storage): keep usage history.
            packageNames.forEach { removeFromAllApps(it, user) }
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

        // Merge disk usage only once per process; in-memory entries stay authoritative afterwards.
        if (!usageLoaded) {
            val installedKeys = apps.mapTo(HashSet(apps.size)) { it.key }
            for ((key, diskEntry) in usageDataSource.loadAll()) {
                if (key !in installedKeys) {
                    usageDirty = true
                    continue
                }
                val existing = usageEntries[key]
                usageEntries[key] = if (existing == null) {
                    diskEntry
                } else {
                    UsageEntry(
                        count = existing.count + diskEntry.count,
                        lastUsed = maxOf(existing.lastUsed, diskEntry.lastUsed),
                    )
                }
            }
            usageLoaded = true
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

    private fun removeFromAllApps(packageName: String, userHandle: UserHandle) {
        allApps.removeAll { it.packageName == packageName && it.userHandle == userHandle }
    }

    private fun removePackage(packageName: String, userHandle: UserHandle) {
        removeFromAllApps(packageName, userHandle)
        if (usageEntries.remove(keyFor(packageName, userHandle)) != null) {
            usageDirty = true
        }
    }

    fun recordAppLaunch(packageName: String, userHandle: UserHandle) {
        val now = System.currentTimeMillis()
        val key = keyFor(packageName, userHandle)
        val existing = usageEntries[key]
        val next = when {
            existing == null -> UsageEntry(count = 1L, lastUsed = now)
            now - existing.lastUsed < SESSION_DEDUP_WINDOW_MS -> return
            else -> UsageEntry(count = existing.count + 1L, lastUsed = now)
        }
        usageEntries[key] = next
        usageDirty = true
        scope.launch { flushUsage() }
    }

    suspend fun flushUsage() {
        // Never flush before the disk merge: a partial snapshot would wipe persisted history.
        if (!usageLoaded || !usageDirty) return
        usageDirty = false
        val snapshot = usageEntries.toMap()
        try {
            usageDataSource.flush(snapshot)
        } catch (e: CancellationException) {
            usageDirty = true
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to flush usage", e)
            usageDirty = true
        }
    }

    fun releaseMostUsedPublish() {
        pendingPublish = true
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

    fun updateMostUsedApps() {
        if (!usageLoaded || !pendingPublish) return
        pendingPublish = false

        val now = System.currentTimeMillis()
        val ranked = usageEntries.entries
            .asSequence()
            .map { (key, entry) -> key to frecencyScore(entry, now) }
            .filter { it.second >= FRECENCY_MIN_SCORE }
            .sortedByDescending { it.second }
            .take(MAX_MOST_USED)
            .map { it.first }
            .toList()

        if (ranked == mostUsedApps.toList()) return
        Snapshot.withMutableSnapshot {
            mostUsedApps.clear()
            mostUsedApps.addAll(ranked)
        }
    }

    private fun frecencyScore(entry: UsageEntry, now: Long): Double {
        val ageDays = (now - entry.lastUsed).coerceAtLeast(0L) / MILLIS_PER_DAY.toDouble()
        return entry.count * exp(-DECAY_LAMBDA_PER_DAY * ageDays)
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

    companion object {
        private const val TAG = "AppsRepository"
        private const val SESSION_DEDUP_WINDOW_MS = 60_000L
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        private const val DECAY_LAMBDA_PER_DAY = 0.05
        private const val FRECENCY_MIN_SCORE = 0.5
        private const val MAX_MOST_USED = 20
        private const val FALLBACK_ICON_SIZE_PX = 96
    }
}
