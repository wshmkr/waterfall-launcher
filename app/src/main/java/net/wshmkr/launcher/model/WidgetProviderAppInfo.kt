package net.wshmkr.launcher.model

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class WidgetProviderAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val widgets: ImmutableList<AppWidgetProviderInfo>
)
