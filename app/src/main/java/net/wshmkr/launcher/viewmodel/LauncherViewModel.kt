package net.wshmkr.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.repository.AppsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


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

    fun toggleHidden(appInfo: AppInfo) {
        viewModelScope.launch {
            appsRepository.toggleHidden(appInfo.packageName, appInfo.userHandle)
        }
    }

    fun toggleFavorite(appInfo: AppInfo) {
        viewModelScope.launch {
            appsRepository.toggleFavorite(appInfo.packageName, appInfo.userHandle)
        }
    }

    fun toggleSuggest(appInfo: AppInfo) {
        viewModelScope.launch {
            appsRepository.toggleSuggest(appInfo.packageName, appInfo.userHandle)
        }
    }
}

