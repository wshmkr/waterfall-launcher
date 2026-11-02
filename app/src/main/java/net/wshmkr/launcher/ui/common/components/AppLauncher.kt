package net.wshmkr.launcher.ui.common.components

import android.content.Context
import android.content.pm.LauncherApps
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.SharedFlow
import net.wshmkr.launcher.viewmodel.LaunchAppIntent

@Composable
fun AppLauncher(launchAppIntent: SharedFlow<LaunchAppIntent>) {
    val context = LocalContext.current
    val launcherApps = remember(context) {
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    }

    LaunchedEffect(launchAppIntent, launcherApps) {
        launchAppIntent.collect { launchIntent ->
            try {
                val activities = launcherApps?.getActivityList(launchIntent.packageName, launchIntent.userHandle)
                activities?.firstOrNull()?.let { activityInfo ->
                    launcherApps.startMainActivity(
                        activityInfo.componentName,
                        launchIntent.userHandle,
                        null,
                        null
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch ${launchIntent.packageName}", e)
            }
        }
    }
}

private const val TAG = "AppLauncher"
