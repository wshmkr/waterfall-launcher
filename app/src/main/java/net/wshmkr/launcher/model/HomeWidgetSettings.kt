package net.wshmkr.launcher.model

import androidx.compose.runtime.Immutable

@Immutable
data class HomeWidgetSettings(
    val showClock: Boolean = true,
    val showCalendar: Boolean = true,
    val showWeather: Boolean = true,
    val showMediaControls: Boolean = true,
    val use24Hour: Boolean = false,
    val useFahrenheit: Boolean = false,
    val weatherLocationName: String? = null,
    val weatherLocationLatitude: Double? = null,
    val weatherLocationLongitude: Double? = null,
)
