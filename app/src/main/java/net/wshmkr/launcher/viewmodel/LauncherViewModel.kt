package net.wshmkr.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.os.UserHandle
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.repository.AppsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LaunchAppIntent(
    val packageName: String,
    val userHandle: UserHandle
)

abstract class LauncherViewModel(
    protected val appsRepository: AppsRepository
) : ViewModel() {

    val launchAppIntent = MutableSharedFlow<LaunchAppIntent>()
    
    val activeProfiles: StateFlow<Set<UserHandle>> = appsRepository.activeProfiles
    
    fun isProfileActive(userHandle: UserHandle): Boolean {
        return appsRepository.isProfileActive(userHandle)
    }

    var launchPending = false
    var observedStop = false

    fun launchApp(packageName: String, userHandle: UserHandle) {
        viewModelScope.launch {
            launchPending = true
            observedStop = false
            appsRepository.recordAppLaunch(packageName)
            launchAppIntent.emit(LaunchAppIntent(packageName, userHandle))
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

