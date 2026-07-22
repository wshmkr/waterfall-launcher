package net.wshmkr.launcher.util

import android.content.Context
import android.content.Intent
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

fun launchCalendarApp(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to launch calendar app", e)
    }
}
