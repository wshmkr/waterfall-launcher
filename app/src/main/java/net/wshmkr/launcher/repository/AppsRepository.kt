package net.wshmkr.launcher.repository

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import net.wshmkr.launcher.ui.theme.maxAppIconSize
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.roundToInt

@Singleton
class AppsRepository @Inject constructor(
    private val application: Application,
    private val usageDataSource: UsageDataSource,
    private val appPreferencesDataSource: AppPreferencesDataSource
) {
    private val launcherApps: LauncherApps = application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager: UserManager = application.getSystemService(Context.USER_SERVICE) as UserManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val iconSizePx
        get() = (maxAppIconSize.value * application.resources.displayMetrics.density).roundToInt()

    private var rasterizedDensityDpi = application.resources.configuration.densityDpi
    private var iconRefreshJob: Job? = null

    val allApps = mutableStateListOf<AppInfo>()
    val mostUsedApps = mutableStateListOf<String>()

    var favorites: PersistentList<String> by mutableStateOf(persistentListOf())
        private set

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

    // Icons are rasterized for the density at load time, so a display-size change leaves them all stale.
    private val densityCallbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) = refreshIconsIfDensityChanged()

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onLowMemory() = Unit
    }

    // Density is recorded after the publish, so a refresh racing loadInstalledApps can't strand stale icons.
    private fun refreshIconsIfDensityChanged() {
        val density = application.resources.configuration.densityDpi
        if (density == rasterizedDensityDpi) return

        iconRefreshJob?.cancel()
        iconRefreshJob = scope.launch {
            refreshAppIcons(allApps.mapTo(HashSet()) { it.userHandle })
            rasterizedDensityDpi = density
        }
    }

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            scope.launch { syncPackage(packageName, user) }
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            removeFromAllApps(packageName, user)
            if (usageEntries.remove(keyFor(packageName, user)) != null) {
                usageDirty = true
            }
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
        application.registerComponentCallbacks(densityCallbacks)
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
        val density = application.resources.configuration.densityDpi
        val userHandles = userManager.userProfiles.takeIf { it.isNotEmpty() } ?: listOf(Process.myUserHandle())

        val storedFavorites = appPreferencesDataSource.getFavorites()
        val favoriteKeys = storedFavorites.toHashSet()

        val apps = withContext(Dispatchers.IO) {
            val seen = mutableSetOf<Pair<String, UserHandle>>()
            buildList {
                for (userHandle in userHandles) {
                    val hidden = appPreferencesDataSource.hidden.get(userHandle)
                    val doNotSuggest = appPreferencesDataSource.doNotSuggest.get(userHandle)

                    val activities = launcherApps.getActivityList(null, userHandle)

                    for (activity in activities) {
                        val appPackageName = activity.componentName.packageName
                        if (seen.add(appPackageName to userHandle) && appPackageName != application.packageName) {
                            add(buildAppInfo(activity, userHandle, favoriteKeys, hidden, doNotSuggest))
                        }
                    }
                }
            }.sortedWith(appComparator)
        }

        Snapshot.withMutableSnapshot {
            favorites = storedFavorites
            allApps.clear()
            allApps.addAll(apps)
        }
        rasterizedDensityDpi = density

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

        refreshIconsIfDensityChanged()
    }

    private fun buildAppInfo(
        activity: LauncherActivityInfo,
        userHandle: UserHandle,
        favoriteKeys: Collection<String>,
        hidden: Set<String>,
        doNotSuggest: Set<String>,
    ): AppInfo {
        val appPackageName = activity.componentName.packageName
        val isSystemApp = activity.applicationInfo.flags.and(ApplicationInfo.FLAG_SYSTEM) != 0
        val label = activity.label.toString()

        return AppInfo(
            label = label,
            packageName = appPackageName,
            icon = toBitmapPainter(activity.getBadgedIcon(0), iconSizePx),
            userHandle = userHandle,
            isSystemApp = isSystemApp,
            isFavorite = keyFor(appPackageName, userHandle) in favoriteKeys,
            isHidden = hidden.contains(appPackageName),
            doNotSuggest = doNotSuggest.contains(appPackageName),
            searchTokens = buildSearchTokens(label),
        )
    }

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
                favoriteKeys = favorites,
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
                        ?.let { app.key to toBitmapPainter(it, iconSizePx) }
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

    suspend fun toggleFavorite(packageName: String, userHandle: UserHandle) {
        val index = indexOfApp(packageName, userHandle)
        if (index == -1) return

        val app = allApps[index]
        val pinned = !app.isFavorite

        Snapshot.withMutableSnapshot {
            favorites = if (pinned) favorites.add(app.key) else favorites.remove(app.key)
            allApps[index] = app.copy(isFavorite = pinned)
        }
        commitFavorites()
    }

    fun previewFavorites(appKeys: List<String>) {
        favorites = appKeys.toPersistentList()
    }

    suspend fun commitFavorites() {
        try {
            appPreferencesDataSource.setFavorites(favorites)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist favorites", e)
        }
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

    private fun indexOfApp(packageName: String, userHandle: UserHandle) =
        allApps.indexOfFirst { it.packageName == packageName && it.userHandle == userHandle }

    private suspend fun togglePackageFlag(
        packageName: String,
        userHandle: UserHandle,
        store: AppPreferencesDataSource.PackageNameSetStore,
        isSet: (AppInfo) -> Boolean,
        withFlag: (AppInfo, Boolean) -> AppInfo,
    ) {
        val index = indexOfApp(packageName, userHandle)
        if (index == -1) return

        val app = allApps[index]
        val enable = !isSet(app)
        try {
            if (enable) {
                store.add(packageName, userHandle)
            } else {
                store.remove(packageName, userHandle)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist package flag", e)
        }
        allApps[index] = withFlag(app, enable)
    }

    companion object {
        private const val TAG = "AppsRepository"
        private const val SESSION_DEDUP_WINDOW_MS = 60_000L
        private const val FRECENCY_MIN_SCORE = 0.5
        private const val MAX_MOST_USED = 20
    }
}

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
private const val DECAY_LAMBDA_PER_DAY = 0.05

private fun frecencyScore(entry: UsageEntry, now: Long): Double {
    val ageDays = (now - entry.lastUsed).coerceAtLeast(0L) / MILLIS_PER_DAY.toDouble()
    return entry.count * exp(-DECAY_LAMBDA_PER_DAY * ageDays)
}

private fun buildSearchTokens(label: String) =
    label.lowercase()
        .split(' ')
        .filter { it.isNotEmpty() }
        .toImmutableList()

private fun toBitmapPainter(drawable: Drawable, maxSizePx: Int): BitmapPainter {
    val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: maxSizePx
    val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: maxSizePx
    val scale = minOf(1f, maxSizePx.toFloat() / maxOf(intrinsicWidth, intrinsicHeight))

    val bitmap = drawable.toBitmap(
        width = (intrinsicWidth * scale).roundToInt().coerceAtLeast(1),
        height = (intrinsicHeight * scale).roundToInt().coerceAtLeast(1),
    )
    return BitmapPainter(bitmap.asImageBitmap())
}
