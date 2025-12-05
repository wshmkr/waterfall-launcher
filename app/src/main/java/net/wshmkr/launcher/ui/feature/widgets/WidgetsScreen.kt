package net.wshmkr.launcher.ui.feature.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import net.wshmkr.launcher.viewmodel.WidgetViewModel

@Composable
fun WidgetsScreen(
    navController: NavController,
    viewModel: WidgetViewModel,
) {
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
