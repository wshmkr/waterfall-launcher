package net.wshmkr.launcher.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.model.HomeTextColor
import net.wshmkr.launcher.model.HomeWidgetSettings
import net.wshmkr.launcher.model.ThemeMode
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsDataSource: UserSettingsDataSource
) : ViewModel() {

    // Kept for existing callers (e.g. HomeOptionsMenu) that consume the composed snapshot.
    var homeWidgetSettings by mutableStateOf(HomeWidgetSettings())
        private set

    private val subscribed = SharingStarted.WhileSubscribed(5_000)

    val showClock: StateFlow<Boolean> =
        userSettingsDataSource.showClock.stateIn(viewModelScope, subscribed, true)
    val showCalendar: StateFlow<Boolean> =
        userSettingsDataSource.showCalendar.stateIn(viewModelScope, subscribed, true)
    val showCalendarEvents: StateFlow<Boolean> =
        userSettingsDataSource.showCalendarEvents.stateIn(viewModelScope, subscribed, true)
    val showWeather: StateFlow<Boolean> =
        userSettingsDataSource.showWeather.stateIn(viewModelScope, subscribed, true)
    val showMediaControls: StateFlow<Boolean> =
        userSettingsDataSource.showMediaControls.stateIn(viewModelScope, subscribed, true)
    val use24Hour: StateFlow<Boolean> =
        userSettingsDataSource.use24Hour.stateIn(viewModelScope, subscribed, false)
    val useFahrenheit: StateFlow<Boolean> =
        userSettingsDataSource.useFahrenheit.stateIn(viewModelScope, subscribed, false)
    val weatherLocationName: StateFlow<String?> =
        userSettingsDataSource.weatherLocationName.stateIn(viewModelScope, subscribed, null)
    val weatherLat: StateFlow<Double?> =
        userSettingsDataSource.weatherLat.stateIn(viewModelScope, subscribed, null)
    val weatherLon: StateFlow<Double?> =
        userSettingsDataSource.weatherLon.stateIn(viewModelScope, subscribed, null)
    val themeMode: StateFlow<ThemeMode> =
        userSettingsDataSource.themeMode.stateIn(viewModelScope, subscribed, ThemeMode.SYSTEM)
    val homeTextColor: StateFlow<HomeTextColor> =
        userSettingsDataSource.homeTextColor.stateIn(viewModelScope, subscribed, HomeTextColor.AUTO)

    init {
        viewModelScope.launch {
            userSettingsDataSource.homeWidgetSettings.collect {
                homeWidgetSettings = it
            }
        }
    }

    fun setShowClock(enabled: Boolean) {
        viewModelScope.launch { userSettingsDataSource.setShowClock(enabled) }
    }

    fun setShowCalendar(enabled: Boolean) {
        viewModelScope.launch { userSettingsDataSource.setShowCalendar(enabled) }
    }

    fun setShowCalendarEvents(enabled: Boolean) {
        viewModelScope.launch { userSettingsDataSource.setShowCalendarEvents(enabled) }
    }

    fun setShowWeather(enabled: Boolean) {
        viewModelScope.launch { userSettingsDataSource.setShowWeather(enabled) }
    }

    fun setShowMedia(enabled: Boolean) {
        viewModelScope.launch { userSettingsDataSource.setShowMedia(enabled) }
    }

    fun setUse24Hour(enabled: Boolean) {
        viewModelScope.launch { userSettingsDataSource.setUse24Hour(enabled) }
    }

    fun setUseFahrenheit(enabled: Boolean) {
        viewModelScope.launch { userSettingsDataSource.setUseFahrenheit(enabled) }
    }

    fun setWeatherLocation(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            userSettingsDataSource.setWeatherLocation(name, latitude, longitude)
        }
    }

    fun clearWeatherLocation() {
        viewModelScope.launch {
            userSettingsDataSource.clearWeatherLocation()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userSettingsDataSource.setThemeMode(mode) }
    }

    fun setHomeTextColor(color: HomeTextColor) {
        viewModelScope.launch { userSettingsDataSource.setHomeTextColor(color) }
    }
}
