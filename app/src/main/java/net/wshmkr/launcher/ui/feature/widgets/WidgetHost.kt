package net.wshmkr.launcher.ui.feature.widgets

import android.appwidget.AppWidgetHostView
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import net.wshmkr.launcher.viewmodel.WidgetViewModel


@Composable
fun WidgetHost(
    viewModel: WidgetViewModel = hiltViewModel()
) {
    val widgetIds = viewModel.widgetIds
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        widgetIds.forEachIndexed { index, widgetId ->
            WidgetItem(
                widgetId = widgetId,
                viewModel = viewModel
            )
            if (index < widgetIds.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun WidgetItem(
    widgetId: Int,
    viewModel: WidgetViewModel
) {
    AndroidView<AppWidgetHostView>(
        factory = { ctx ->
            val widgetRepository = viewModel.getWidgetRepository()
            val appWidgetInfo = try {
                widgetRepository.appWidgetManager.getAppWidgetInfo(widgetId)
            } catch (e: Exception) {
                null
            }

            if (appWidgetInfo == null) {
                return@AndroidView AppWidgetHostView(ctx).apply {
                    addView(
                        TextView(ctx).apply {
                            text = "Unable to load widget"
                            setTextColor(android.graphics.Color.WHITE)
                            textSize = 16f
                            setPadding(24, 24, 24, 24)
                        }
                    )
                }
            }

            val widgetView = try {
                widgetRepository.appWidgetHost.createView(ctx, widgetId, appWidgetInfo)
            } catch (e: Exception) {
                AppWidgetHostView(ctx).apply {
                    addView(
                        TextView(ctx).apply {
                            text = "Error loading widget: ${e.message}"
                            setTextColor(android.graphics.Color.WHITE)
                            textSize = 16f
                            setPadding(24, 24, 24, 24)
                        }
                    )
                }
            }

            widgetView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            widgetView
        },
        modifier = Modifier
            .fillMaxWidth(),
        key = widgetId
    )
}
