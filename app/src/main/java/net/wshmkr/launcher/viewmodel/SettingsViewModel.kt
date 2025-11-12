package net.wshmkr.launcher.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsDataSource: UserSettingsDataSource
) : ViewModel() {

    var backgroundUri by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            backgroundUri = userSettingsDataSource.getBackgroundUri()
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
}

