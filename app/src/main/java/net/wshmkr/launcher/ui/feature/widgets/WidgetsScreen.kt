package net.wshmkr.launcher.ui.feature.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import net.wshmkr.launcher.viewmodel.WidgetViewModel

private val ScrimColor = Color(0f, 0f, 0f, 0.5f)

@Composable
fun WidgetsScreen(
    navController: NavController,
    viewModel: WidgetViewModel,
) {
    var showAddWidget by remember { mutableStateOf(false) }

    // AddWidgetView's own BackHandler takes precedence while it is visible.
    val onBackToLauncher = remember(navController) { { navController.popBackStack(); Unit } }
    BackHandler(onBack = onBackToLauncher)

    val managedWidgets = viewModel.managedWidgets

    val onDismissAdd = remember(viewModel) {
        {
            viewModel.deselectLetter()
            showAddWidget = false
        }
    }
    val onShowAdd = remember { { showAddWidget = true } }
    val onDeleteWidget = remember(viewModel) { { id: Int -> viewModel.removeWidget(id) } }

    Box(modifier = Modifier.fillMaxSize()) {
        viewModel.backgroundUri?.let { uriString ->
            val uri = remember(uriString) { uriString.toUri() }
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Widgets screen background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrimColor)
        )

        if (showAddWidget) {
            AddWidgetView(
                viewModel = viewModel,
                onDismiss = onDismissAdd
            )
        } else {
            ManageWidgetsView(
                managedWidgets = managedWidgets,
                onAddWidget = onShowAdd,
                onDeleteWidget = onDeleteWidget,
            )
        }
    }
}
