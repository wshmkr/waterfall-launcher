package net.wshmkr.launcher.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

fun getCurrentTime(use24Hour: Boolean): String = formatTime(LocalTime.now(), use24Hour)

fun getCurrentDate(): String = formatDate(LocalDate.now())

fun formatTime(time: LocalTime, use24Hour: Boolean): String {
    val localized = formattersForCurrentLocale()
    val formatter = if (use24Hour) localized.time24Hour else localized.time12Hour
    return time.format(formatter)
}

fun formatDate(date: LocalDate): String = date.format(formattersForCurrentLocale().date)

// Minute-precision clock — ticks at the next minute boundary so the reader
// invalidates at most once per minute.
@Composable
fun rememberCurrentLocalTime(): State<LocalTime> =
    produceState(initialValue = LocalTime.now()) {
        while (isActive) {
            delay(millisUntilNextMinute())
            value = LocalTime.now()
        }
    }

@Composable
fun rememberCurrentDate(): State<LocalDate> =
    produceState(initialValue = LocalDate.now()) {
        while (isActive) {
            delay(millisUntilNextMinute())
            val today = LocalDate.now()
            if (today != value) value = today
        }
    }

private fun millisUntilNextMinute(): Long {
    val now = LocalTime.now()
    val elapsed = now.second * 1_000L + now.nano / 1_000_000L
    return (ONE_MINUTE - elapsed).coerceAtLeast(1L)
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
