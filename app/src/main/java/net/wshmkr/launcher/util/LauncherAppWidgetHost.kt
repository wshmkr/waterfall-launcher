package net.wshmkr.launcher.util

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

/**
 * Custom AppWidgetHost for the launcher
 * Manages the lifecycle of widgets hosted by the launcher
 */
class LauncherAppWidgetHost(
    context: Context,
    hostId: Int = WIDGET_HOST_ID
) : AppWidgetHost(context, hostId) {

    companion object {
        const val WIDGET_HOST_ID = 1024
    }
}
