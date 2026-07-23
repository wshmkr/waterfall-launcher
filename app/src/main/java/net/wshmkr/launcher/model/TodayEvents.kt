package net.wshmkr.launcher.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class TodayEvents(
    val events: ImmutableList<CalendarEvent> = persistentListOf(),
    val hiddenCount: Int = 0,
)
