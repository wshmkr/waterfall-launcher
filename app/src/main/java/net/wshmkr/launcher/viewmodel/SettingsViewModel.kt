package net.wshmkr.launcher.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.model.HomeWidgetSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsDataSource: UserSettingsDataSource
) : ViewModel() {

    var backgroundUri by mutableStateOf<String?>(null)
        private set
    var homeWidgetSettings by mutableStateOf(HomeWidgetSettings())
        private set

    init {
        viewModelScope.launch {
            backgroundUri = userSettingsDataSource.getBackgroundUri()
        }

        viewModelScope.launch {
            userSettingsDataSource.homeWidgetSettings.collect {
                homeWidgetSettings = it
            }
        }
    }

    fun setBackgroundUri(uri: Uri?) {
        viewModelScope.launch {
            val uriString = uri?.toString()
            userSettingsDataSource.setBackgroundUri(uriString)
            backgroundUri = uriString
        }
    }

    fun removeBackground() {
        viewModelScope.launch {
            userSettingsDataSource.setBackgroundUri(null)
            backgroundUri = null
        }
    }

    fun setShowClock(enabled: Boolean) {
        viewModelScope.launch { userSettingsDataSource.setShowClock(enabled) }
    }

    fun setShowCalendar(enabled: Boolean) {
        viewModelScope.launch { userSettingsDataSource.setShowCalendar(enabled) }
    }

    fun setShowWeather(enabled: Boolean) {
        viewModelScope.launch { userSettingsDataSource.setShowWeather(enabled) }
    }

    fun setShowMedia(enabled: Boolean) {
        viewModelScope.launch { userSettingsDataSource.setShowMedia(enabled) }
    }
}

