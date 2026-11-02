package net.wshmkr.launcher.repository

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.util.maxAppIconSizePx
import net.wshmkr.launcher.util.toBitmapPainter
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: BitmapPainter,
)

@Singleton
class IconPackRepository @Inject constructor(
    private val application: Application,
    private val userSettingsDataSource: UserSettingsDataSource,
) {
    private class AppFilter(
        val componentToDrawable: Map<String, String>,
        val packageFallback: Map<String, String>,
    )

    private class LoadedPack(
        val packageName: String,
        val resources: Resources,
        val appFilter: AppFilter,
    ) {
        // Immutable per pack; skips repeated getIdentifier lookups across refresh passes.
        val drawableIds = ConcurrentHashMap<String, Int>()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _iconsChanged = MutableSharedFlow<Unit>()
    val iconsChanged: SharedFlow<Unit> = _iconsChanged.asSharedFlow()

    @Volatile
    private var activePack: LoadedPack? = null

    private val applyMutex = Mutex()
    private val initialLoad = CompletableDeferred<Unit>()

    private val packChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.data?.schemeSpecificPart ?: return
            when (intent.action) {
                Intent.ACTION_PACKAGE_REMOVED ->
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        scope.launch { userSettingsDataSource.clearIconPackIfSelected(packageName) }
                    }
                Intent.ACTION_PACKAGE_REPLACED ->
                    scope.launch {
                        if (userSettingsDataSource.iconPack.first() == packageName) applySelectedPack()
                    }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        application.registerReceiver(packChangeReceiver, filter)

        scope.launch {
            userSettingsDataSource.iconPack.collect {
                applySelectedPack()
                initialLoad.complete(Unit)
            }
        }
    }

    suspend fun awaitInitialLoad() = initialLoad.await()

    // Pack drawables are third-party and may only fail once rasterized, so that stays inside the fallback.
    fun renderIcon(activity: LauncherActivityInfo): BitmapPainter {
        val iconSizePx = maxAppIconSizePx(application.resources)
        return try {
            resolvePackDrawable(activity.componentName)
                ?.let { application.packageManager.getUserBadgedIcon(it, activity.user) }
                ?.toBitmapPainter(iconSizePx)
        } catch (_: Exception) {
            null
        } ?: activity.getBadgedIcon(0).toBitmapPainter(iconSizePx)
    }

    private fun resolvePackDrawable(componentName: ComponentName): Drawable? {
        val pack = activePack ?: return null
        val drawableName = pack.appFilter.componentToDrawable[componentName.flattenToString()]
            ?: pack.appFilter.packageFallback[componentName.packageName]
            ?: return null
        val id = pack.drawableIds.getOrPut(drawableName) {
            pack.resources.getIdentifier(drawableName, "drawable", pack.packageName)
        }
        if (id == 0) return null
        return ResourcesCompat.getDrawable(pack.resources, id, null)
    }

    fun packLabel(packageName: String): String? = try {
        val packageManager = application.packageManager
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (_: Exception) {
        null
    }

    suspend fun discoverIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val packageManager = application.packageManager
        val iconSizePx = maxAppIconSizePx(application.resources)
        themeIntents
            .flatMap { packageManager.queryIntentActivities(it, PackageManager.ResolveInfoFlags.of(0)) }
            .distinctBy { it.activityInfo.packageName }
            .mapNotNull { resolveInfo ->
                try {
                    val appInfo = resolveInfo.activityInfo.applicationInfo
                    IconPackInfo(
                        packageName = appInfo.packageName,
                        label = packageManager.getApplicationLabel(appInfo).toString(),
                        icon = appInfo.loadIcon(packageManager).toBitmapPainter(iconSizePx),
                    )
                } catch (_: Exception) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
    }

    // Reads the selection under the lock so a queued reload can't overwrite a newer choice.
    private suspend fun applySelectedPack() {
        applyMutex.withLock {
            val selected = userSettingsDataSource.iconPack.first()
            // The uninstall broadcast is missed while the process is dead, so absence is rechecked here.
            if (selected != null && !isInstalled(selected)) {
                userSettingsDataSource.clearIconPackIfSelected(selected)
            }
            // A selected pack that fails to load degrades to system icons without erasing the choice.
            val loaded = selected?.let { withContext(Dispatchers.IO) { loadPack(it) } }
            val hadPack = activePack != null
            activePack = loaded
            if (hadPack || loaded != null) {
                _iconsChanged.emit(Unit)
            }
        }
    }

    private fun isInstalled(packageName: String): Boolean = try {
        application.packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun loadPack(packageName: String): LoadedPack? = try {
        val packResources = application.packageManager.getResourcesForApplication(packageName)
        val appFilter = parseAppFilterAsset(packResources)
            ?: parseAppFilterResource(packageName, packResources)
        appFilter?.let { LoadedPack(packageName, packResources, it) }
    } catch (_: Exception) {
        null
    }

    private fun parseAppFilterAsset(packResources: Resources): AppFilter? = try {
        packResources.assets.open(APPFILTER_ASSET).use { stream ->
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(stream, null)
            parseAppFilter(parser)
        }
    } catch (_: Exception) {
        null
    }

    private fun parseAppFilterResource(packageName: String, packResources: Resources): AppFilter? {
        val id = packResources.getIdentifier(APPFILTER_RESOURCE, "xml", packageName)
        if (id == 0) return null
        return try {
            val parser = packResources.getXml(id)
            try {
                parseAppFilter(parser)
            } finally {
                parser.close()
            }
        } catch (_: Exception) {
            null
        }
    }

    // Packs commonly ship slightly malformed XML; whatever parsed before a failure is kept.
    private fun parseAppFilter(parser: XmlPullParser): AppFilter? {
        val componentToDrawable = HashMap<String, String>()
        val packageFallback = HashMap<String, String>()
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    addAppFilterItem(parser, componentToDrawable, packageFallback)
                }
                event = parser.next()
            }
        } catch (_: Exception) {
        }
        return AppFilter(componentToDrawable, packageFallback).takeIf { componentToDrawable.isNotEmpty() }
    }

    private fun addAppFilterItem(
        parser: XmlPullParser,
        componentToDrawable: MutableMap<String, String>,
        packageFallback: MutableMap<String, String>,
    ) {
        val drawableName = parser.getAttributeValue(null, "drawable")?.takeIf { it.isNotBlank() } ?: return
        // Normalization matters: appfilters mix "pkg/pkg.Cls" and "pkg/.Cls" forms.
        val component = parser.getAttributeValue(null, "component")
            ?.takeIf { it.startsWith(COMPONENT_PREFIX) && it.endsWith("}") }
            ?.removeSurrounding(COMPONENT_PREFIX, "}")
            ?.let(ComponentName::unflattenFromString)
            ?: return
        componentToDrawable.putIfAbsent(component.flattenToString(), drawableName)
        packageFallback.putIfAbsent(component.packageName, drawableName)
    }

    // Must mirror the icon-pack <queries> block in AndroidManifest.xml, and vice versa.
    private val themeIntents = listOf(
        Intent("org.adw.launcher.THEMES"),
        Intent("com.novalauncher.THEME"),
        Intent("org.adw.launcher.icons.ACTION_PICK_ICON"),
        Intent("com.anddoes.launcher.THEME"),
        Intent("com.gau.go.launcherex.theme"),
        Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME"),
    )

    companion object {
        private const val APPFILTER_ASSET = "appfilter.xml"
        private const val APPFILTER_RESOURCE = "appfilter"
        private const val COMPONENT_PREFIX = "ComponentInfo{"
    }
}
