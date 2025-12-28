package net.wshmkr.launcher.ui.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import net.wshmkr.launcher.ui.common.components.AppListItem
import net.wshmkr.launcher.ui.common.components.verticalSwipeDetection
import net.wshmkr.launcher.ui.common.dialog.AccessibilityServiceDialog
import net.wshmkr.launcher.ui.feature.home.widgets.ClockWidget
import net.wshmkr.launcher.ui.feature.home.widgets.MediaWidget
import net.wshmkr.launcher.ui.feature.widgets.WidgetHost
import net.wshmkr.launcher.util.NotificationPanelHelper
import net.wshmkr.launcher.viewmodel.HomeViewModel

@Composable
fun FavoritesView(
    navController: NavController,
    viewModel: HomeViewModel,
) {
    BackHandler(enabled = true) { }

    val context = LocalContext.current
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var showHomeOptionsMenu by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.favoriteListItems.isNotEmpty()) {
        if (viewModel.favoriteListItems.isNotEmpty()) {
            isVisible = true
        }
    }

    val onSwipeUp = remember {{ viewModel.showSearchOverlay = true }}
    val onSwipeDown = remember {{
        if (!NotificationPanelHelper.expandNotificationPanel()) {
            showAccessibilityDialog = true
        }
    }}
    val onLongPress = { showHomeOptionsMenu = true }

    if (showAccessibilityDialog) {
        AccessibilityServiceDialog(
            onDismiss = { showAccessibilityDialog = false },
            onOpenSettings = {
                NotificationPanelHelper.openAccessibilitySettings(context)
                showAccessibilityDialog = false
            }
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .verticalSwipeDetection(
                    onSwipeUp = onSwipeUp,
                    onSwipeDown = onSwipeDown
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() }
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
                        is ListItem.WidgetHost -> "widget_host"
                        is ListItem.AppItem -> item.appInfo.key
                        is ListItem.SectionHeader -> "header_${item.letter}"
                    }
                },
            ) { item ->
                when (item) {
                    is ListItem.ClockWidget -> {
                        ClockWidget()
                    }
                    is ListItem.MediaWidget -> {
                        MediaWidget()
                    }
                    is ListItem.WidgetHost -> {
                        WidgetHost()
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

    if (showHomeOptionsMenu) {
        HomeOptionsMenu(
            navController = navController,
            onDismiss = { showHomeOptionsMenu = false }
        )
    }
}
