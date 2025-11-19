package net.wshmkr.launcher.ui.feature.widgets

import android.appwidget.AppWidgetHostView
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import net.wshmkr.launcher.MainActivity
import net.wshmkr.launcher.model.WidgetInfo
import net.wshmkr.launcher.viewmodel.WidgetViewModel

/**
 * WidgetHost composable that displays hosted Android widgets
 * When tapped without a widget, allows the user to select a widget to add
 */
@Composable
fun WidgetHost(
    viewModel: WidgetViewModel = hiltViewModel(LocalViewModelStoreOwner.current!!)
) {
    val context = LocalContext.current
    android.util.Log.d("WidgetHost", "WidgetHost composed with ViewModel: ${viewModel.hashCode()}")
    android.util.Log.d("WidgetHost", "Displaying ${viewModel.widgets.size} widgets")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Display all widgets
        viewModel.widgets.forEach { widget ->
            android.util.Log.d("WidgetHost", "Rendering widget: ${widget.label} (ID: ${widget.widgetId})")
            WidgetItem(
                widget = widget,
                onRemove = { viewModel.removeWidget(widget.widgetId) },
                viewModel = viewModel
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Debug controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AddWidgetButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    android.util.Log.d("WidgetHost", "Add widget button clicked")
                    val activity = context as? MainActivity
                    if (activity != null) {
                        android.util.Log.d("WidgetHost", "Got MainActivity instance, requesting widget")
                        activity.requestWidgetPicker()
                    } else {
                        android.util.Log.e("WidgetHost", "Context is not MainActivity!")
                        viewModel.requestAddWidget()
                    }
                }
            )
            RemoveAllWidgetsButton(
                modifier = Modifier.weight(1f),
                enabled = viewModel.widgets.isNotEmpty(),
                onClick = {
                    android.util.Log.d("WidgetHost", "Remove all widgets button clicked")
                    viewModel.removeAllWidgets()
                }
            )
        }
    }
}

@Composable
private fun WidgetItem(
    widget: WidgetInfo,
    onRemove: () -> Unit,
    viewModel: WidgetViewModel
) {
    val context = LocalContext.current
    var showRemoveButton by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .fillMaxWidth()
            .height(200.dp) // Match the AndroidView height for testing
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showRemoveButton = !showRemoveButton
                    }
                )
            }
    ) {
        // Widget content
        AndroidView<android.appwidget.AppWidgetHostView>(
            factory = { ctx ->
                android.util.Log.d("WidgetHost", "Creating widget view for ID: ${widget.widgetId}")
                val widgetRepository = viewModel.getWidgetRepository()
                val appWidgetManager = widgetRepository.getAppWidgetManager()
                val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widget.widgetId)
                val host = widgetRepository.getAppWidgetHost()

                if (appWidgetInfo == null) {
                    android.util.Log.e("WidgetHost", "AppWidgetInfo is null for ID: ${widget.widgetId}")
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
                android.util.Log.d(
                    "WidgetHost",
                    "Widget info: ${appWidgetInfo.provider} | size ${appWidgetInfo.minWidth}x${appWidgetInfo.minHeight}"
                )

                widgetView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                widgetView
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Fixed height for testing
        )

        // Remove button overlay
        if (showRemoveButton) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(Color.Black.copy(alpha = 0.7f))
            ) {
                IconButton(
                    onClick = {
                        onRemove()
                        showRemoveButton = false
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove widget",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AddWidgetButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add widget",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap to add a widget",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RemoveAllWidgetsButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Red.copy(alpha = if (enabled) 0.6f else 0.3f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove all widgets",
            tint = Color.White.copy(alpha = if (enabled) 0.8f else 0.4f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Remove all widgets",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = if (enabled) 0.9f else 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}