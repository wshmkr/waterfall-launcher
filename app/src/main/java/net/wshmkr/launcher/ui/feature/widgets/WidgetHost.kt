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
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import net.wshmkr.launcher.model.WidgetInfo
import net.wshmkr.launcher.viewmodel.WidgetViewModel


@Composable
fun WidgetHost(
    viewModel: WidgetViewModel = hiltViewModel(LocalViewModelStoreOwner.current!!)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        viewModel.widgets.forEach { widget ->
            WidgetItem(
                widget = widget,
                onRemove = { viewModel.removeWidget(widget.widgetId) },
                viewModel = viewModel
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

    }
}

@Composable
private fun WidgetItem(
    widget: WidgetInfo,
    onRemove: () -> Unit,
    viewModel: WidgetViewModel
) {
    // Widget content
    AndroidView<AppWidgetHostView>(
        factory = { ctx ->
            val widgetRepository = viewModel.getWidgetRepository()
            val appWidgetManager = widgetRepository.getAppWidgetManager()
            val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widget.widgetId)
            val host = widgetRepository.getAppWidgetHost()

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

            // Use AppWidgetHost.createView so the host can manage RemoteViews updates
            val widgetView = host.createView(ctx, widget.widgetId, appWidgetInfo)

            widgetView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            widgetView
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp) // todo: variable height
    )
}
