package net.wshmkr.launcher.ui.feature.home.widgets

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.collections.immutable.ImmutableList
import net.wshmkr.launcher.model.CalendarEvent
import net.wshmkr.launcher.repository.CalendarRepository
import net.wshmkr.launcher.ui.common.icons.CalendarTodayIcon
import net.wshmkr.launcher.util.formatEventStartTime
import net.wshmkr.launcher.util.launchCalendarApp

@Composable
fun CalendarEventsWidget(
    events: ImmutableList<CalendarEvent>,
    use24Hour: Boolean,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(CalendarRepository.hasReadCalendarPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) onPermissionGranted()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = CalendarRepository.hasReadCalendarPermission(context)
        if (now != hasPermission) {
            hasPermission = now
            if (now) onPermissionGranted()
        }
    }

    if (!hasPermission) {
        EnableCalendarRow(
            modifier = modifier,
            onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
        )
        return
    }

    if (events.isEmpty()) return

    val typography = MaterialTheme.typography
    val eventTextStyle = remember(typography) {
        typography.bodyMedium.copy(color = Color.White, fontSize = 14.sp)
    }

    val timeStyle = remember(eventTextStyle) {
        eventTextStyle.copy(color = Color.White.copy(alpha = 0.7f))
    }
    val timeLabels = remember(events, use24Hour) {
        events.map { event ->
            if (event.allDay) ALL_DAY_LABEL else formatEventStartTime(event.startMillis, use24Hour)
        }
    }

    // Size the time column to the widest label so times never wrap to a second line.
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val timeColumnWidth = remember(timeLabels, timeStyle, density) {
        val widest = timeLabels.maxOf { textMeasurer.measure(it, timeStyle).size.width }
        with(density) { widest.toDp() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = EVENTS_INDENT, end = 8.dp),
    ) {
        events.forEachIndexed { index, event ->
            EventRow(
                title = event.title,
                timeLabel = timeLabels[index],
                dotColor = event.calendarColor?.let(::Color) ?: DEFAULT_DOT_COLOR,
                timeStyle = timeStyle,
                timeColumnWidth = timeColumnWidth,
                textStyle = eventTextStyle,
                onClick = { launchCalendarApp(context) },
            )
        }
    }
}

@Composable
private fun EnableCalendarRow(modifier: Modifier, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            painter = CalendarTodayIcon(),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Show today's events",
            fontSize = 14.sp,
            color = Color.White,
        )
    }
}

@Composable
private fun EventRow(
    title: String,
    timeLabel: String,
    dotColor: Color,
    timeStyle: TextStyle,
    timeColumnWidth: Dp,
    textStyle: TextStyle,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(DOT_SIZE)
                .background(dotColor, CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = timeLabel,
            style = timeStyle,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(timeColumnWidth),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val ALL_DAY_LABEL = "All day"
private val EVENTS_INDENT = 16.dp
private val DOT_SIZE = 6.dp
private val DEFAULT_DOT_COLOR = Color.White.copy(alpha = 0.7f)
