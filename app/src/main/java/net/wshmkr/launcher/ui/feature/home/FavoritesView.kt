package net.wshmkr.launcher.ui.feature.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import net.wshmkr.launcher.model.ListItem
import net.wshmkr.launcher.ui.Screen
import net.wshmkr.launcher.ui.common.components.AppListItem
import net.wshmkr.launcher.ui.common.dialog.AccessibilityServiceDialog
import net.wshmkr.launcher.util.NotificationPanelHelper
import net.wshmkr.launcher.ui.common.components.verticalSwipeDetection
import net.wshmkr.launcher.ui.feature.home.widgets.ClockWidget
import net.wshmkr.launcher.ui.feature.home.widgets.MediaWidget
import net.wshmkr.launcher.viewmodel.HomeViewModel

@Composable
fun FavoritesView(
    navController: NavController,
    viewModel: HomeViewModel,
) {
    val context = LocalContext.current
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    if (showAccessibilityDialog) {
        AccessibilityServiceDialog(
            onDismiss = { showAccessibilityDialog = false },
            onOpenSettings = {
                NotificationPanelHelper.openAccessibilitySettings(context)
                showAccessibilityDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalSwipeDetection(
                onSwipeUp = { viewModel.showSearchOverlay = true },
                onSwipeDown = {
                    if (!NotificationPanelHelper.expandNotificationPanel()) {
                        showAccessibilityDialog = true
                    }
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { navController.navigate(Screen.Settings.route) }
                )
            },
        contentPadding = PaddingValues(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        userScrollEnabled = false,
    ) {
        items(
            items = viewModel.favoriteListItems,
            key = { item ->
                when (item) {
                    is ListItem.ClockWidget -> "clock_widget"
                    is ListItem.MediaWidget -> "media_widget"
                    is ListItem.AppItem -> item.appInfo.packageName
                    is ListItem.SectionHeader -> "header_${item.letter}"
                }
            },
        ) { item ->
            when (item) {
                is ListItem.ClockWidget -> {
                    ClockWidget()
                    Spacer(modifier = Modifier.height(16.dp))
                }
                is ListItem.MediaWidget -> {
                    MediaWidget()
                    Spacer(modifier = Modifier.height(16.dp))
                }
                is ListItem.AppItem -> {
                    AppListItem(
                        appInfo = item.appInfo,
                        viewModel = viewModel,
                    )
                }
                else -> null
            }
        }
    }
}
