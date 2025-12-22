package net.wshmkr.launcher.ui.feature.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import net.wshmkr.launcher.viewmodel.WidgetViewModel

@Composable
fun WidgetsScreen(
    navController: NavController,
    viewModel: WidgetViewModel,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState = lifecycleOwner.lifecycle.currentStateAsState()

    LaunchedEffect(lifecycleState.value) {
        if (lifecycleState.value == Lifecycle.State.RESUMED) {
            viewModel.refreshBackground()
        }
    }

    var showAddWidget by remember { mutableStateOf(false) }

    BackHandler {
        if (showAddWidget) {
            viewModel.deselectLetter()
            showAddWidget = false
        } else {
            navController.popBackStack()
        }
    }

    val managedWidgets = viewModel.managedWidgets

    Box(modifier = Modifier.fillMaxSize()) {
        viewModel.backgroundUri?.let { uriString ->
            Image(
                painter = rememberAsyncImagePainter(uriString.toUri()),
                contentDescription = "Widgets screen background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0f, 0f, 0f, 0.5f))
        )

        if (showAddWidget) {
            AddWidgetView(
                viewModel = viewModel,
                onDismiss = {
                    viewModel.deselectLetter()
                    showAddWidget = false
                }
            )
        } else {
            ManageWidgetsView(
                managedWidgets = managedWidgets,
                onAddWidget = { showAddWidget = true },
                onDeleteWidget = { widgetId -> viewModel.removeWidget(widgetId) },
            )
        }
    }
}
