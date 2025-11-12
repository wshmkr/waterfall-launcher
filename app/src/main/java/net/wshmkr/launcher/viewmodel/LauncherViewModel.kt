package net.wshmkr.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.wshmkr.launcher.repository.AppsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for launcher screens
 * Contains shared functionality for app launching and management
 */
abstract class LauncherViewModel(
    protected val appsRepository: AppsRepository
) : ViewModel() {

    private val _launchAppIntent = MutableSharedFlow<String>()
    val launchAppIntent = _launchAppIntent.asSharedFlow()

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            appsRepository.recordAppLaunch(packageName)
            _launchAppIntent.emit(packageName)
        }
    }

    fun toggleHidden(packageName: String) {
        viewModelScope.launch {
            appsRepository.toggleHidden(packageName)
        }
    }

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            appsRepository.toggleFavorite(packageName)
        }
    }

    fun toggleSuggest(packageName: String) {
        viewModelScope.launch {
            appsRepository.toggleSuggest(packageName)
        }
    }
}

