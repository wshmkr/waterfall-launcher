package net.wshmkr.launcher.util

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.UserHandle
import android.provider.CalendarContract
import android.util.Log

private const val TAG = "PackageLauncher"

fun launchPackage(context: Context, packageName: String): Boolean {
    return try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Failed to launch $packageName", e)
        false
    }
}

fun openAppDetails(context: Context, packageName: String, userHandle: UserHandle) {
    try {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        // The system reads only the package, so a placeholder class still opens the right profile's details.
        val component = launcherApps.getActivityList(packageName, userHandle).firstOrNull()?.componentName
            ?: ComponentName(packageName, packageName)
        launcherApps.startAppDetailsActivity(component, userHandle, null, null)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to open app details for $packageName", e)
    }
}

fun requestUninstall(context: Context, packageName: String, userHandle: UserHandle) {
    try {
        val intent = Intent(Intent.ACTION_DELETE, packageUri(packageName))
            .putExtra(Intent.EXTRA_USER, userHandle)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to request uninstall of $packageName", e)
    }
}

private fun packageUri(packageName: String): Uri = Uri.fromParts("package", packageName, null)

private fun calendarAppIntent() = Intent(Intent.ACTION_MAIN).apply {
    addCategory(Intent.CATEGORY_APP_CALENDAR)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}

private fun launchCalendarApp(context: Context) {
    try {
        context.startActivity(calendarAppIntent())
    } catch (e: Exception) {
        Log.w(TAG, "Failed to launch calendar app", e)
    }
}

fun launchCalendarToday(context: Context) {
    launchCalendarAt(context, System.currentTimeMillis())
}

fun launchCalendarAt(context: Context, timeMillis: Long) {
    val uri = CalendarContract.CONTENT_URI.buildUpon()
        .appendPath("time")
        .also { ContentUris.appendId(it, timeMillis) }
        .build()
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to open calendar at $timeMillis", e)
        launchCalendarApp(context)
    }
}
