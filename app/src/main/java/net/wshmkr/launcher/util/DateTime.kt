package net.wshmkr.launcher.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
    val time12HourWithMarker: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", locale)
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

fun formatEventStartTime(epochMillis: Long, use24Hour: Boolean): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    val localized = formattersForCurrentLocale()
    val formatter = if (use24Hour) localized.time24Hour else localized.time12HourWithMarker
    return time.format(formatter)
}

fun eventTimeLabel(startMillis: Long, endMillis: Long, allDay: Boolean, use24Hour: Boolean): String {
    if (allDay) return "All day"
    val zone = ZoneId.systemDefault()
    val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    val startOfTomorrow = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return when {
        startMillis < startOfToday && endMillis >= startOfTomorrow -> "All day"
        startMillis < startOfToday -> "Until ${formatEventStartTime(endMillis, use24Hour)}"
        startMillis >= startOfTomorrow -> "Tmrw ${formatEventStartTime(startMillis, use24Hour)}"
        else -> formatTimeRange(startMillis, endMillis, use24Hour)
    }
}

// 12-hour ranges within the same half-day show the am/pm marker once: "2:00 – 3:30 PM".
private fun formatTimeRange(startMillis: Long, endMillis: Long, use24Hour: Boolean): String {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalTime()
    val end = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalTime()
    val localized = formattersForCurrentLocale()
    if (use24Hour) return "${start.format(localized.time24Hour)} – ${end.format(localized.time24Hour)}"
    val sameMarker = (start.hour < 12) == (end.hour < 12) && endMillis - startMillis < ONE_DAY
    val startFormatter = if (sameMarker) localized.time12Hour else localized.time12HourWithMarker
    return "${start.format(startFormatter)} – ${end.format(localized.time12HourWithMarker)}"
}

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
