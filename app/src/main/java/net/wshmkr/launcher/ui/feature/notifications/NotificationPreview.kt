package net.wshmkr.launcher.ui.feature.notifications

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.util.ONE_DAY
import net.wshmkr.launcher.util.ONE_HOUR
import net.wshmkr.launcher.util.ONE_MINUTE
import net.wshmkr.launcher.util.ONE_WEEK
import net.wshmkr.launcher.util.timeSince
import kotlinx.coroutines.delay
import net.wshmkr.launcher.ui.common.components.AppTitle

@Composable
fun NotificationPreview(appInfo: AppInfo) {
    val notification = appInfo.mostRecentNotification
    val contentText = notification?.bigText ?: notification?.text

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(appInfo.mostRecentNotification?.timestamp) {
        appInfo.mostRecentNotification?.let { notification ->
            while (true) {
                val age = System.currentTimeMillis() - notification.timestamp
                val delay = when {
                    age < ONE_HOUR -> ONE_MINUTE/2
                    age < ONE_DAY -> ONE_HOUR/2
                    age < ONE_WEEK -> ONE_DAY/2
                    else -> ONE_WEEK/2
                }

                delay(delay.toLong())
                currentTime = System.currentTimeMillis()
            }
        }
    }

    val age = remember(appInfo.mostRecentNotification?.timestamp, currentTime) {
        appInfo.mostRecentNotification?.let { notification ->
            " · ${timeSince(notification.timestamp)}"
        } ?: ""
    }

    AppTitle(appInfo.label + age, appInfo.isHidden)

    if (notification != null) {
        Spacer(modifier = Modifier.height(4.dp))

        appInfo.mostRecentNotification?.title?.let {
            if (!it.isEmpty()) {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }

        contentText?.let {
            if (!it.isEmpty()) {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 2,
                )
            }
        }
    }
}
