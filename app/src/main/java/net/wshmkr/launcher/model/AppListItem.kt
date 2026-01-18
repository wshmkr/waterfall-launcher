package net.wshmkr.launcher.model

sealed class AppListItem {
    data class SectionHeader(val letter: String, val position: Int) : AppListItem()
    data class AppItem(val appInfo: AppInfo) : AppListItem()
}
