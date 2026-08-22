package net.wshmkr.launcher.ui.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.ui.common.calculateCenteredContentTopPadding
import net.wshmkr.launcher.ui.common.components.verticalSwipeDetection
import net.wshmkr.launcher.ui.common.dialog.AccessibilityServiceDialog
import net.wshmkr.launcher.ui.feature.home.widgets.CalendarEventsWidget
import net.wshmkr.launcher.ui.feature.home.widgets.ClockWidget
import net.wshmkr.launcher.ui.feature.home.widgets.MediaWidget
import net.wshmkr.launcher.ui.feature.widgets.WidgetStack
import net.wshmkr.launcher.ui.theme.Dimensions
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.util.NotificationPanelHelper
import net.wshmkr.launcher.viewmodel.HomeViewModel
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val MIN_REORDERABLE_FAVORITES = 2

@Composable
fun FavoritesView(
    navController: NavController,
    viewModel: HomeViewModel,
) {
    val reordering = viewModel.favoritesReordering

    // Always enabled: the launcher home is the bottom of the stack, so Back is consumed here.
    BackHandler(enabled = true) {
        if (reordering) viewModel.endFavoritesReorder()
    }

    val context = LocalContext.current
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    val showHomeOptionsMenu = remember { mutableStateOf(false) }
    val widgetTouched = remember { mutableStateOf(false) }
    val onWidgetTouchedChange = remember { { touched: Boolean -> widgetTouched.value = touched } }
    val activeProfiles by viewModel.activeProfiles.collectAsState()
    val favoritesVisible by viewModel.favoritesVisible.collectAsState()
    val favoriteApps = viewModel.favoriteApps
    val widgetSettings = viewModel.homeWidgetSettings
    val todayEvents by viewModel.todayEvents.collectAsState()
    val onCalendarPermissionGranted = remember(viewModel) { viewModel::refreshCalendarEvents }
    val onWeatherRefresh = remember(viewModel) { viewModel::requestWeatherRefresh }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromKey = from.key as? String
        val toKey = to.key as? String
        if (fromKey != null && toKey != null) {
            viewModel.moveFavorite(fromKey, toKey)
        }
    }

    // Also committed here, because a drag torn down with the screen never stops cleanly.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        if (viewModel.favoritesReordering) viewModel.endFavoritesReorder()
    }

    DisposableEffect(viewModel) {
        onDispose {
            if (viewModel.favoritesReordering) viewModel.endFavoritesReorder()
        }
    }

    LaunchedEffect(favoritesVisible) {
        if (favoritesVisible) {
            isVisible = true
        }
    }

    // HOME while already on favorites doesn't recompose this view, so reset the scroll here.
    LaunchedEffect(viewModel) {
        viewModel.returnHomeEvents.collect {
            listState.scrollToItem(0)
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
    val onLongPress = remember { { if (!widgetTouched.value) showHomeOptionsMenu.value = true } }
    val onEndReorder = remember(viewModel) { viewModel::endFavoritesReorder }
    val startReorder = remember(viewModel) { viewModel::startFavoritesReorder }
    val onStartReorder =
        if (favoriteApps.count { !it.isSuggested } >= MIN_REORDERABLE_FAVORITES) startReorder else null

    val onClick = remember(viewModel) {
        { app: AppInfo -> viewModel.launchApp(app.packageName, app.userHandle) }
    }
    val onToggleFavorite = remember(viewModel) { viewModel::toggleFavorite }
    val onToggleHidden = remember(viewModel) { viewModel::toggleHidden }
    val onToggleSuggest = remember(viewModel) { viewModel::toggleSuggest }
    val onClearNotifications = remember(viewModel) { viewModel::clearNotifications }
    val onMoveFavorite = remember(viewModel) { viewModel::moveFavorite }

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
        val dimensions = LocalDimensions.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (reordering) {
                        Modifier.endReorderOnPressOutside(listState, dimensions, onEndReorder)
                    } else {
                        Modifier
                    }
                ),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (reordering) {
                            Modifier
                        } else {
                            Modifier
                                .verticalSwipeDetection(
                                    listState = listState,
                                    onSwipeUp = onSwipeUp,
                                    onSwipeDown = onSwipeDown
                                )
                                .pointerInput(onLongPress) {
                                    detectTapGestures(
                                        onLongPress = { onLongPress() }
                                    )
                                }
                        }
                    ),
                contentPadding = PaddingValues(
                    start = dimensions.gutterLarge,
                    end = dimensions.gutterLarge,
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + Spacing.small,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                userScrollEnabled = reordering,
            ) {
                item { Spacer(modifier = Modifier.height(calculateCenteredContentTopPadding())) }

                item(key = "clock_widget") {
                    ClockWidget(
                        showClock = widgetSettings.showClock,
                        showCalendar = widgetSettings.showCalendar,
                        showWeather = widgetSettings.showWeather,
                        use24Hour = widgetSettings.use24Hour,
                        useFahrenheit = widgetSettings.useFahrenheit,
                        weatherState = viewModel.weatherState,
                        hasStaticWeatherLocation = widgetSettings.hasWeatherLocation,
                        onWeatherRefresh = onWeatherRefresh,
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

                item(key = "widget_stack") {
                    WidgetStack(onTouchedChange = onWidgetTouchedChange)
                }

                item(key = "media_widget") {
                    MediaWidget(
                        enabled = widgetSettings.showMediaControls,
                    )
                }

                itemsIndexed(
                    items = favoriteApps,
                    key = { _, item -> item.key },
                    contentType = { _, item ->
                        if (item.isSuggested) SUGGESTION_CONTENT_TYPE else FAVORITE_CONTENT_TYPE
                    },
                ) { index, item ->
                    FavoriteRow(
                        index = index,
                        item = item,
                        favoriteApps = favoriteApps,
                        reordering = reordering,
                        reorderState = reorderState,
                        activeProfiles = activeProfiles,
                        viewModel = viewModel,
                        onClick = onClick,
                        onToggleFavorite = onToggleFavorite,
                        onToggleHidden = onToggleHidden,
                        onToggleSuggest = onToggleSuggest,
                        onClearNotifications = onClearNotifications,
                        onMoveFavorite = onMoveFavorite,
                        onStartReorder = onStartReorder,
                    )
                }
            }

            val outlineAlpha = animateFloatAsState(
                targetValue = if (reordering) 1f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "outlineAlpha",
            )
            if (outlineAlpha.value > 0f) {
                FavoritesOutline(listState = listState, alphaProvider = outlineAlpha::value)
            }
        }
    }

    if (showHomeOptionsMenu.value) {
        HomeOptionsMenu(
            navController = navController,
            onDismiss = { showHomeOptionsMenu.value = false },
            onReorderFavorites = onStartReorder,
        )
    }
}

private fun Modifier.endReorderOnPressOutside(
    listState: LazyListState,
    dimensions: Dimensions,
    onEndReorder: () -> Unit,
) = pointerInput(listState, dimensions, onEndReorder) {
    awaitEachGesture {
        val down = awaitFirstDown(pass = PointerEventPass.Initial)
        if (!isInsideFavoritesOutline(down.position, listState, dimensions)) {
            down.consume()
            onEndReorder()
        }
    }
}

private fun PointerInputScope.isInsideFavoritesOutline(
    position: Offset,
    listState: LazyListState,
    dimensions: Dimensions,
): Boolean {
    val (top, height) = favoriteRowsBounds(listState) ?: return false
    return position.y >= top &&
        position.y <= top + height &&
        position.x >= dimensions.favoritesOutlineStart.toPx() &&
        position.x <= size.width - dimensions.favoritesOutlineEnd.toPx()
}
