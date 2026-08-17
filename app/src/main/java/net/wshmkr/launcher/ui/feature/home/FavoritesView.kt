package net.wshmkr.launcher.ui.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import android.os.UserHandle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.NotificationInfo
import net.wshmkr.launcher.ui.common.calculateCenteredContentTopPadding
import net.wshmkr.launcher.ui.common.components.AppListItem
import net.wshmkr.launcher.ui.common.components.verticalSwipeDetection
import net.wshmkr.launcher.ui.common.dialog.AccessibilityServiceDialog
import net.wshmkr.launcher.ui.common.icons.DragIndicatorIcon
import net.wshmkr.launcher.ui.feature.home.widgets.CalendarEventsWidget
import net.wshmkr.launcher.ui.feature.home.widgets.ClockWidget
import net.wshmkr.launcher.ui.feature.home.widgets.MediaWidget
import net.wshmkr.launcher.ui.feature.widgets.WidgetStack
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.util.NotificationPanelHelper
import net.wshmkr.launcher.viewmodel.HomeViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val FAVORITE_CONTENT_TYPE = "favorite_app"
private const val SUGGESTION_CONTENT_TYPE = "suggested_app"
private const val MIN_REORDERABLE_FAVORITES = 2

private val FULL_ALPHA: () -> Float = { 1f }
private val DIMMED_ALPHA: () -> Float = { 0.4f }

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
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (reordering) {
                            Modifier.pointerInput(onEndReorder) {
                                detectTapGestures(onTap = { onEndReorder() })
                            }
                        } else {
                            Modifier
                                .verticalSwipeDetection(
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
                contentPadding = PaddingValues(horizontal = LocalDimensions.current.gutterLarge),
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

            if (reordering) {
                FavoritesOutline(listState = listState)
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

@Composable
private fun LazyItemScope.FavoriteRow(
    index: Int,
    item: AppInfo,
    favoriteApps: List<AppInfo>,
    reordering: Boolean,
    reorderState: ReorderableLazyListState,
    activeProfiles: Set<UserHandle>,
    viewModel: HomeViewModel,
    onClick: (AppInfo) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onToggleHidden: (AppInfo) -> Unit,
    onToggleSuggest: (AppInfo) -> Unit,
    onClearNotifications: (List<NotificationInfo>) -> Boolean,
    onMoveFavorite: (String, String) -> Unit,
    onStartReorder: (() -> Unit)?,
) {
    val isActiveUser = remember(item.userHandle, activeProfiles) {
        item.userHandle in activeProfiles
    }
    val notifications by remember(item.key) {
        viewModel.notificationsFor(item.packageName, item.userHandle)
    }

    val draggable = reordering && !item.isSuggested
    ReorderableItem(
        state = reorderState,
        key = item.key,
        enabled = draggable,
        animateItemModifier = if (reordering) Modifier.animateItem() else Modifier,
    ) {
        AppListItem(
            appInfo = item,
            // The drag gesture is invisible to accessibility services, so
            // reordering is mirrored as custom actions on the row.
            modifier = if (draggable) {
                Modifier.semantics {
                    customActions = reorderActions(
                        item = item,
                        previous = favoriteApps.getOrNull(index - 1),
                        next = favoriteApps.getOrNull(index + 1),
                        moveFavorite = onMoveFavorite,
                    )
                }
            } else Modifier,
            isActiveUser = isActiveUser,
            onClick = onClick,
            onToggleFavorite = onToggleFavorite,
            onToggleHidden = onToggleHidden,
            onToggleSuggest = onToggleSuggest,
            alphaProvider = if (reordering && item.isSuggested) DIMMED_ALPHA else FULL_ALPHA,
            notifications = notifications,
            onClearNotifications = onClearNotifications,
            onReorderFavorites = if (!reordering && !item.isSuggested) onStartReorder else null,
            clickEnabled = !reordering,
            dragHandle = if (draggable) {
                { ReorderHandle(item.label, Modifier.draggableHandle()) }
            } else null,
        )
    }
}

private fun reorderActions(
    item: AppInfo,
    previous: AppInfo?,
    next: AppInfo?,
    moveFavorite: (String, String) -> Unit,
): List<CustomAccessibilityAction> = buildList {
    if (previous != null && !previous.isSuggested) {
        add(CustomAccessibilityAction("Move up") { moveFavorite(item.key, previous.key); true })
    }
    if (next != null && !next.isSuggested) {
        add(CustomAccessibilityAction("Move down") { moveFavorite(item.key, next.key); true })
    }
}

@Composable
private fun ReorderHandle(label: String, modifier: Modifier = Modifier) {
    Icon(
        painter = DragIndicatorIcon(),
        contentDescription = "Reorder $label",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .size(LocalDimensions.current.iconLarge)
            .padding(Spacing.small),
    )
}

// Traced over the favorite rows only, so it lines up with them rather than the wider widget area.
@Composable
private fun FavoritesOutline(listState: LazyListState) {
    val dimensions = LocalDimensions.current
    val bounds by remember(listState) {
        derivedStateOf {
            var first: LazyListItemInfo? = null
            var last: LazyListItemInfo? = null
            for (row in listState.layoutInfo.visibleItemsInfo) {
                if (row.contentType != FAVORITE_CONTENT_TYPE) continue
                if (first == null) first = row
                last = row
            }
            if (first == null || last == null) return@derivedStateOf null
            first.offset to (last.offset + last.size - first.offset)
        }
    }
    val (top, height) = bounds ?: return

    Box(
        modifier = Modifier
            .offset { IntOffset(0, top) }
            .fillMaxWidth()
            .height(with(LocalDensity.current) { height.toDp() })
            // The list gutter plus the insets AppListItem applies on top of it.
            .padding(start = dimensions.gutterLarge + Spacing.small, end = dimensions.gutterLarge * 2)
            .border(1.dp, MaterialTheme.colorScheme.outline, Corners.medium),
    )
}
