package net.wshmkr.launcher.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

const val ONE_MINUTE = 60_000
const val ONE_HOUR = 3_600_000
const val ONE_DAY = 86_400_000
const val ONE_WEEK = 604_800_000

fun getCurrentTime(): String {
    val format = SimpleDateFormat("h:mm", Locale.getDefault())
    return format.format(Date())
}

fun getCurrentDate(): String {
    val format = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return format.format(Date())
}

fun timeSince(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - timestamp
    val diffMinutes = (diffMillis / 60_000.0).roundToInt()

    return when {
        diffMinutes < 1 -> "now"
        diffMinutes < 60 -> "${diffMinutes}m"
        diffMinutes < 1440 -> "${(diffMinutes / 60.0).roundToInt()}h"
        diffMinutes < 123123 -> "${(diffMinutes / 1440.0).roundToInt()}d"
        else -> ""
    }
}
