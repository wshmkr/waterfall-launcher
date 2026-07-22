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
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.ui.common.calculateCenteredContentTopPadding
import net.wshmkr.launcher.ui.common.components.AppListItem
import net.wshmkr.launcher.ui.common.components.verticalSwipeDetection
import net.wshmkr.launcher.ui.common.dialog.AccessibilityServiceDialog
import net.wshmkr.launcher.ui.feature.home.widgets.CalendarEventsWidget
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
    val showHomeOptionsMenu = remember { mutableStateOf(false) }
    val activeProfiles by viewModel.activeProfiles.collectAsState()
    val favoritesVisible by viewModel.favoritesVisible.collectAsState()
    val favoriteApps = viewModel.favoriteApps
    val widgetSettings = viewModel.homeWidgetSettings
    val todayEvents by viewModel.todayEvents.collectAsState()
    val onCalendarPermissionGranted = remember(viewModel) { viewModel::refreshCalendarEvents }

    LaunchedEffect(favoritesVisible) {
        if (favoritesVisible) {
            isVisible = true
        }
    }

    val onSwipeUp = remember(viewModel) { { viewModel.showSearchOverlay = true } }
    val onSwipeDown = remember {
        {
            if (!NotificationPanelHelper.expandNotificationPanel()) {
                showAccessibilityDialog = true
            }
        }
    }
    val onLongPress = remember(showHomeOptionsMenu) { { showHomeOptionsMenu.value = true } }

    val onClick = remember(viewModel) {
        { app: AppInfo -> viewModel.launchApp(app.packageName, app.userHandle) }
    }
    val onToggleFavorite = remember(viewModel) { viewModel::toggleFavorite }
    val onToggleHidden = remember(viewModel) { viewModel::toggleHidden }
    val onToggleSuggest = remember(viewModel) { viewModel::toggleSuggest }

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
                .pointerInput(onLongPress) {
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
                    showClock = widgetSettings.showClock,
                    showCalendar = widgetSettings.showCalendar,
                    showWeather = widgetSettings.showWeather,
                    use24Hour = widgetSettings.use24Hour,
                    useFahrenheit = widgetSettings.useFahrenheit,
                    weatherLocationLatitude = widgetSettings.weatherLocationLatitude,
                    weatherLocationLongitude = widgetSettings.weatherLocationLongitude,
                )
            }

            if (widgetSettings.showCalendarEvents) {
                item(key = "calendar_events") {
                    CalendarEventsWidget(
                        events = todayEvents,
                        use24Hour = widgetSettings.use24Hour,
                        onPermissionGranted = onCalendarPermissionGranted,
                    )
                }
            }

            item(key = "widget_host") {
                WidgetHost()
            }

            item(key = "media_widget") {
                MediaWidget(
                    enabled = widgetSettings.showMediaControls,
                )
            }

            items(
                items = favoriteApps,
                key = { item -> item.key },
                contentType = { "favorite_app" },
            ) { item ->
                val isActiveUser = remember(item.userHandle, activeProfiles) {
                    item.userHandle in activeProfiles
                }
                val notifications by viewModel
                    .notificationsFor(item.packageName, item.userHandle)
                    .collectAsState()
                AppListItem(
                    appInfo = item,
                    isActiveUser = isActiveUser,
                    onClick = onClick,
                    onToggleFavorite = onToggleFavorite,
                    onToggleHidden = onToggleHidden,
                    onToggleSuggest = onToggleSuggest,
                    notifications = notifications,
                )
            }
        }
    }

    if (showHomeOptionsMenu.value) {
        HomeOptionsMenu(
            navController = navController,
            onDismiss = { showHomeOptionsMenu.value = false }
        )
    }
}
