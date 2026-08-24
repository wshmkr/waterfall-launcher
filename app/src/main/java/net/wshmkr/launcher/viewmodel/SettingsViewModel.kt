package net.wshmkr.launcher.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.model.HomeWidgetSettings
import net.wshmkr.launcher.model.PaletteStyle
import net.wshmkr.launcher.repository.IconPackInfo
import net.wshmkr.launcher.repository.IconPackRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsDataSource: UserSettingsDataSource,
    private val iconPackRepository: IconPackRepository,
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
    val paletteStyle: StateFlow<PaletteStyle> =
        userSettingsDataSource.paletteStyle.stateIn(viewModelScope, subscribed, PaletteStyle.Default)
    val iconPack: StateFlow<String?> =
        userSettingsDataSource.iconPack.stateIn(viewModelScope, subscribed, null)
    val iconPackLabel: StateFlow<String?> =
        userSettingsDataSource.iconPack
            .map { packageName -> packageName?.let(iconPackRepository::packLabel) }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, subscribed, null)

    private val _availableIconPacks = MutableStateFlow<List<IconPackInfo>?>(null)
    val availableIconPacks: StateFlow<List<IconPackInfo>?> = _availableIconPacks.asStateFlow()

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

    fun setPaletteStyle(style: PaletteStyle) {
        viewModelScope.launch { userSettingsDataSource.setPaletteStyle(style) }
    }

    fun loadIconPacks() {
        viewModelScope.launch { _availableIconPacks.value = iconPackRepository.discoverIconPacks() }
    }

    fun setIconPack(packageName: String?) {
        viewModelScope.launch { userSettingsDataSource.setIconPack(packageName) }
    }
}
