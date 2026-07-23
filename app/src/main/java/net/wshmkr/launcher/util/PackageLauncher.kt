package net.wshmkr.launcher.util

import android.content.ContentUris
import android.content.Context
import android.content.Intent
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

private fun calendarAppIntent() = Intent(Intent.ACTION_MAIN).apply {
    addCategory(Intent.CATEGORY_APP_CALENDAR)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}

fun launchCalendarApp(context: Context) {
    try {
        context.startActivity(calendarAppIntent())
    } catch (e: Exception) {
        Log.w(TAG, "Failed to launch calendar app", e)
    }
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
