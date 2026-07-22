package net.wshmkr.launcher.model

import androidx.compose.runtime.Immutable

@Immutable
sealed class AppListItem {
    @Immutable
    data class SectionHeader(val letter: String, val position: Int) : AppListItem()

    @Immutable
    data class AppItem(val appInfo: AppInfo, val sectionLetter: String) : AppListItem()
}
