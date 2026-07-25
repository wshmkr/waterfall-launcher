package net.wshmkr.launcher.ui.feature.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import net.wshmkr.launcher.ui.theme.launcherScrim
import net.wshmkr.launcher.viewmodel.WidgetViewModel

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(launcherScrim())
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
