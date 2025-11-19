package net.wshmkr.launcher.model

sealed class ListItem {
    data class SectionHeader(val letter: String, val position: Int) : ListItem()
    data class AppItem(val appInfo: AppInfo) : ListItem()
    object ClockWidget : ListItem()
    object MediaWidget : ListItem()
    object WidgetHost : ListItem()
}
