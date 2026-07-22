package net.wshmkr.launcher.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

const val ONE_SECOND = 1_000
const val ONE_MINUTE = 60_000
const val ONE_HOUR = 3_600_000
const val ONE_DAY = 86_400_000
const val ONE_WEEK = 604_800_000

private class LocalizedFormatters(val locale: Locale) {
    val time12Hour: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm", locale)
    val time24Hour: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    val date: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", locale)
}

private var formatters = LocalizedFormatters(Locale.getDefault())

private fun formattersForCurrentLocale(): LocalizedFormatters {
    val locale = Locale.getDefault()
    if (formatters.locale != locale) {
        formatters = LocalizedFormatters(locale)
    }
    return formatters
}

fun getCurrentTime(use24Hour: Boolean): String {
    val localizedFormatters = formattersForCurrentLocale()
    val formatter = if (use24Hour) localizedFormatters.time24Hour else localizedFormatters.time12Hour
    return LocalTime.now().format(formatter)
}

fun getCurrentDate(): String {
    return LocalDate.now().format(formattersForCurrentLocale().date)
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
