package net.wshmkr.launcher.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

const val ONE_SECOND = 1_000
const val ONE_MINUTE = 60_000
const val ONE_HOUR = 3_600_000
const val ONE_DAY = 86_400_000
const val ONE_WEEK = 604_800_000

fun getCurrentTime(use24Hour: Boolean): String {
    val pattern = if (use24Hour) "HH:mm" else "h:mm"
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    return format.format(Date())
}

fun getCurrentDate(): String {
    val format = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return format.format(Date())
}

fun timeSince(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = (now - timestamp).toFloat()

    return when {
        diffMillis < ONE_MINUTE -> "now"
        diffMillis < ONE_HOUR -> "${(diffMillis / ONE_MINUTE).roundToInt()}m"
        diffMillis < ONE_DAY -> "${(diffMillis / ONE_HOUR).roundToInt()}h"
        diffMillis < ONE_WEEK -> "${(diffMillis / ONE_DAY).roundToInt()}d"
        else -> "${(diffMillis / ONE_WEEK).roundToInt()}W"
    }
}
