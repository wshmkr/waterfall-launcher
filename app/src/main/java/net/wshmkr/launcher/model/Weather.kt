package net.wshmkr.launcher.model

import androidx.compose.runtime.Immutable

@Immutable
data class WeatherReading(
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val sunriseTime: String?,
    val sunsetTime: String?,
    val latitude: Double,
    val longitude: Double,
    val fetchedAtMillis: Long,
)

@Immutable
sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Ready(val reading: WeatherReading, val isStale: Boolean) : WeatherUiState
    data object Error : WeatherUiState
}
