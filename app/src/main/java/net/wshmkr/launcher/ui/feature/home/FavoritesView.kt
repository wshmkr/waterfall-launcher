package net.wshmkr.launcher.ui.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import net.wshmkr.launcher.ui.common.calculateCenteredContentTopPadding
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
    val activeProfiles by viewModel.activeProfiles.collectAsState()

    LaunchedEffect(viewModel.favoriteApps.isNotEmpty()) {
        if (viewModel.favoriteApps.isNotEmpty()) {
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
            userScrollEnabled = false,
        ) {
            item { Spacer(modifier = Modifier.height(calculateCenteredContentTopPadding())) }

            item(key = "clock_widget") {
                ClockWidget(
                    showClock = viewModel.homeWidgetSettings.showClock,
                    showCalendar = viewModel.homeWidgetSettings.showCalendar,
                    showWeather = viewModel.homeWidgetSettings.showWeather,
                    use24Hour = viewModel.homeWidgetSettings.use24Hour,
                    useFahrenheit = viewModel.homeWidgetSettings.useFahrenheit,
                )
            }

            item(key = "widget_host") {
                WidgetHost()
            }

            item(key = "media_widget") {
                MediaWidget(
                    enabled = viewModel.homeWidgetSettings.showMediaControls,
                )
            }

            items(
                items = viewModel.favoriteApps,
                key = { item -> item.key },
            ) { item ->
                AppListItem(
                    appInfo = item,
                    activeProfiles = activeProfiles,
                    viewModel = viewModel,
                )
            }
        }
    }

    if (showHomeOptionsMenu) {
        HomeOptionsMenu(
            navController = navController,
            viewModel = viewModel,
            onDismiss = { showHomeOptionsMenu = false }
        )
    }
}
