package net.wshmkr.launcher.ui.feature.home.widgets

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.util.formatDate
import net.wshmkr.launcher.util.formatTime
import net.wshmkr.launcher.util.launchCalendarToday
import net.wshmkr.launcher.util.launchPackage
import net.wshmkr.launcher.util.rememberCurrentDate
import net.wshmkr.launcher.util.rememberCurrentLocalTime

@Composable
fun ClockWidget(
    showClock: Boolean = true,
    showCalendar: Boolean = true,
    showWeather: Boolean = true,
    use24Hour: Boolean = false,
    useFahrenheit: Boolean = false,
    weatherLocationLatitude: Double? = null,
    weatherLocationLongitude: Double? = null,
) {
    if (!showClock && !showCalendar && !showWeather) return

    val context = LocalContext.current
    val dimensions = LocalDimensions.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        if (showClock) {
            ClockTimeText(use24Hour = use24Hour, onClick = { launchClockApp(context) })
        }

        if (showCalendar || showWeather) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .heightIn(min = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showCalendar) {
                    ClockDateText(onClick = { launchCalendarToday(context) })
                }

                if (showCalendar && showWeather) {
                    Text(
                        text = "•",
                        fontSize = dimensions.clockChipFont,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }

                if (showWeather) {
                    WeatherWidget(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                launchWeatherApp(context)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        useFahrenheit,
                        weatherLocationLatitude,
                        weatherLocationLongitude,
                    )
                }
            }
        }
    }
}

// Leaf that owns the minute tick so ancestors don't recompose each minute.
@Composable
private fun ClockTimeText(use24Hour: Boolean, onClick: () -> Unit) {
    val now by rememberCurrentLocalTime()
    val display = remember(now, use24Hour) { formatTime(now, use24Hour) }
    Text(
        text = display,
        fontSize = LocalDimensions.current.clockFont,
        color = Color.White,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    )
}

// Leaf that reads only the date state; siblings stay unaffected.
@Composable
private fun ClockDateText(onClick: () -> Unit) {
    val today by rememberCurrentDate()
    val display = remember(today) { formatDate(today) }
    Text(
        text = display,
        fontSize = LocalDimensions.current.clockChipFont,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private fun launchClockApp(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, 0) ?: return
        launchPackage(context, resolveInfo.activityInfo.packageName)
    } catch (e: Exception) {
        Log.w("ClockWidget", "Failed to launch clock app", e)
    }
}

private fun launchWeatherApp(context: Context) {
    val pm = context.packageManager
    try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_WEATHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val resolveInfo = pm.resolveActivity(intent, 0)
        if (resolveInfo != null) {
            context.startActivity(intent)
            return
        }

        // fall back to Samsung Weather (doesn't register CATEGORY_APP_WEATHER)
        val samsungPackages = listOf("com.sec.android.daemonapp", "com.samsung.android.weather")
        for (pkg in samsungPackages) {
            if (launchPackage(context, pkg)) {
                return
            }
        }

        // fall back to web search
        val webIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://www.google.com/search?q=weather".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(webIntent)
    } catch (e: Exception) {
        Log.w("ClockWidget", "Failed to launch weather app", e)
    }
}
