package net.wshmkr.launcher.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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

fun formatTime(time: LocalTime, use24Hour: Boolean): String {
    val localized = formattersForCurrentLocale()
    val formatter = if (use24Hour) localized.time24Hour else localized.time12Hour
    return time.format(formatter)
}

fun formatDate(date: LocalDate): String = date.format(formattersForCurrentLocale().date)

// 12-hour event times use a compact marker: "10:00a", "11:00p".
fun formatEventTime(epochMillis: Long, use24Hour: Boolean): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    val localized = formattersForCurrentLocale()
    if (use24Hour) return time.format(localized.time24Hour)
    val marker = if (time.hour < 12) "a" else "p"
    return "${time.format(localized.time12Hour)}$marker"
}

@Composable
fun rememberCurrentLocalTime(): State<LocalTime> =
    rememberReadEachMinute { LocalTime.now().truncatedTo(ChronoUnit.MINUTES) }

@Composable
fun rememberCurrentDate(): State<LocalDate> = rememberReadEachMinute(LocalDate::now)

// Re-reads on every start because delay() doesn't advance while the device deep-sleeps.
@Composable
private fun <T> rememberReadEachMinute(read: () -> T): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return produceState(initialValue = remember { read() }, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = read()
                delay(millisUntilNextMinute())
            }
        }
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
