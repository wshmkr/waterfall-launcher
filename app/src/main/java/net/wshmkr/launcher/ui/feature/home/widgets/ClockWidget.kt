package net.wshmkr.launcher.ui.feature.home.widgets

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.wshmkr.launcher.util.ONE_SECOND
import net.wshmkr.launcher.util.getCurrentDate
import net.wshmkr.launcher.util.getCurrentTime
import androidx.core.net.toUri

@Composable
fun ClockWidget() {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var currentDate by remember { mutableStateOf(getCurrentDate()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(ONE_SECOND.toLong())
            currentTime = getCurrentTime()
            currentDate = getCurrentDate()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = currentTime,
            fontSize = 48.sp,
            color = Color.White,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    launchClockApp(context)
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentDate,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        launchCalendarApp(context)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            WeatherWidget(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                onClick = {
                    launchWeatherApp(context)
                }
            )
        }
    }
}

private fun launchClockApp(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        if (resolveInfo != null) {
            val packageName = resolveInfo.activityInfo.packageName
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
            }
        }
    } catch (e: Exception) { }
}

private fun launchCalendarApp(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) { }
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

        // Fall back to Samsung Weather (doesn't register CATEGORY_APP_WEATHER)
        val samsungPackages = listOf("com.sec.android.daemonapp", "com.samsung.android.weather")
        for (pkg in samsungPackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            }
        }

        // Fall back to web search
        val webIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://www.google.com/search?q=weather".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(webIntent)
    } catch (e: Exception) { }
}
