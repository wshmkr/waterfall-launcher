package net.wshmkr.launcher.ui.feature.widgets

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberWidgetPreviewDrawable(info: AppWidgetProviderInfo): State<Drawable?> {
    val context = LocalContext.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    return produceState<Drawable?>(null, info, densityDpi) {
        value = withContext(Dispatchers.IO) {
            try {
                info.loadPreviewImage(context, densityDpi)
                    ?: info.loadIcon(context, densityDpi)
            } catch (e: Exception) {
                null
            }
        }
    }
}
