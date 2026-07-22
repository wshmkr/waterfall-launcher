package net.wshmkr.launcher.model

import androidx.compose.runtime.Immutable

@Immutable
data class CalendarEvent(
    val instanceId: Long,
    val eventId: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val calendarColor: Int?,
)
