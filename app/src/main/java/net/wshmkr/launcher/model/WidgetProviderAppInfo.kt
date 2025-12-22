package net.wshmkr.launcher.model

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable

data class WidgetProviderAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>
)
