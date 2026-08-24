package net.wshmkr.launcher.repository

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
            val pack = activePack ?: return
            if (intent?.data?.schemeSpecificPart != pack.packageName) return
            when (intent.action) {
                Intent.ACTION_PACKAGE_REMOVED ->
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        scope.launch { userSettingsDataSource.setIconPack(null) }
                    }
                Intent.ACTION_PACKAGE_REPLACED ->
                    scope.launch { applyPack(pack.packageName) }
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
            userSettingsDataSource.iconPack.collect { packageName ->
                applyPack(packageName)
                initialLoad.complete(Unit)
            }
        }
    }

    suspend fun awaitInitialLoad() = initialLoad.await()

    fun resolveIcon(componentName: ComponentName): Drawable? {
        val pack = activePack ?: return null
        val drawableName = pack.appFilter.componentToDrawable[componentName.flattenToString()]
            ?: pack.appFilter.packageFallback[componentName.packageName]
            ?: return null
        val id = pack.drawableIds.getOrPut(drawableName) {
            pack.resources.getIdentifier(drawableName, "drawable", pack.packageName)
        }
        if (id == 0) return null
        return try {
            ResourcesCompat.getDrawable(pack.resources, id, null)
        } catch (_: Exception) {
            null
        }
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

    private suspend fun applyPack(packageName: String?) {
        applyMutex.withLock {
            // A selected pack that fails to load degrades to system icons without erasing the
            // user's choice; only the uninstall broadcast clears the setting.
            val loaded = packageName?.let { withContext(Dispatchers.IO) { loadPack(it) } }
            val hadPack = activePack != null
            activePack = loaded
            if (hadPack || loaded != null) {
                _iconsChanged.emit(Unit)
            }
        }
    }

    private fun loadPack(packageName: String): LoadedPack? {
        val packResources = try {
            application.packageManager.getResourcesForApplication(packageName)
        } catch (_: Exception) {
            return null
        }
        val appFilter = parseAppFilterAsset(packResources)
            ?: parseAppFilterResource(packageName, packResources)
            ?: return null
        return LoadedPack(packageName, packResources, appFilter)
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
    private fun parseAppFilter(parser: XmlPullParser): AppFilter {
        val componentToDrawable = HashMap<String, String>()
        val packageFallback = HashMap<String, String>()
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawableName = parser.getAttributeValue(null, "drawable")
                    if (component != null && !drawableName.isNullOrBlank() &&
                        component.startsWith(COMPONENT_PREFIX) && component.endsWith("}")
                    ) {
                        val flattened = component.substring(COMPONENT_PREFIX.length, component.length - 1)
                        // Normalization matters: appfilters mix "pkg/pkg.Cls" and "pkg/.Cls" forms.
                        ComponentName.unflattenFromString(flattened)?.let {
                            componentToDrawable.putIfAbsent(it.flattenToString(), drawableName)
                            packageFallback.putIfAbsent(it.packageName, drawableName)
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
        }
        return AppFilter(componentToDrawable, packageFallback)
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
