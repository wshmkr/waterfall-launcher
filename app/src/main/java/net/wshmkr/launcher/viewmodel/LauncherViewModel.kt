package net.wshmkr.launcher.viewmodel

import android.os.UserHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.repository.AppsRepository

data class LaunchAppIntent(
    val packageName: String,
    val userHandle: UserHandle
)

abstract class LauncherViewModel(
    protected val appsRepository: AppsRepository
) : ViewModel() {

    val launchAppIntent = MutableSharedFlow<LaunchAppIntent>()

    val activeProfiles: StateFlow<ImmutableSet<UserHandle>> = appsRepository.activeProfiles

    fun launchApp(packageName: String, userHandle: UserHandle) {
        viewModelScope.launch {
            launchAppIntent.emit(LaunchAppIntent(packageName, userHandle))
        }
        appsRepository.recordAppLaunch(packageName, userHandle)
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
