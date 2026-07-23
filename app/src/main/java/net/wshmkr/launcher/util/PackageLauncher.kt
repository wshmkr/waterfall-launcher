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

fun launchCalendarEvent(
    context: Context,
    eventId: Long,
    startMillis: Long,
    endMillis: Long,
    allDay: Boolean,
) {
    try {
        val intent = Intent(
            Intent.ACTION_VIEW,
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
        ).apply {
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to open event $eventId", e)
        launchCalendarApp(context)
    }
}
