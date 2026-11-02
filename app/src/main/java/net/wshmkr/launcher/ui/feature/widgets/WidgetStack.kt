package net.wshmkr.launcher.ui.feature.widgets

import android.appwidget.AppWidgetHostView
import android.os.Bundle
import android.util.SizeF
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import net.wshmkr.launcher.ui.common.gesture.captureLongPress
import net.wshmkr.launcher.ui.theme.ContentAlpha
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.viewmodel.WidgetViewModel
import kotlin.math.roundToInt

// Virtual page count for wrap-around scrolling; real page = virtual page % widget count.
private const val LOOP_PAGE_COUNT = Int.MAX_VALUE

private const val WIDGET_SIZE_DEBOUNCE_MS = 80L

private val DRAG_HANDLE_HEIGHT = 32.dp

private val PAGE_DASH_TOUCH_HEIGHT = 12.dp
private val PAGE_DASH_TOP_GAP = 4.dp

private fun loopStartPage(pageCount: Int, initialIndex: Int): Int {
    if (pageCount <= 1) return initialIndex
    val mid = LOOP_PAGE_COUNT / 2
    return mid - mid % pageCount + initialIndex
}

// The host view overload also writes OPTION_APPWIDGET_SIZES and accounts for padding.
private fun AppWidgetHostView.applyWidgetSize(widthDp: Int, heightDp: Int) =
    updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())))

// Looping pages repeat each widget; only the copy nearest the scroll position hosts its view.
private fun PagerState.isNearestCopyOfPage(page: Int, pageCount: Int): Boolean {
    if (pageCount <= 1) return true
    val offsetInPages = (currentPage - page) + currentPageOffsetFraction
    return (offsetInPages / pageCount).roundToInt() == 0
}

// AppWidgetHost delivers updates only to the most recently created view per id, so each id gets one view.
private class WidgetViewCache(private val createView: (Int) -> AppWidgetHostView?) {
    private val views = HashMap<Int, AppWidgetHostView>()

    fun get(widgetId: Int): AppWidgetHostView? =
        views[widgetId] ?: createView(widgetId)?.also { views[widgetId] = it }

    fun retainOnly(widgetIds: List<Int>) {
        views.keys.retainAll(widgetIds.toSet())
    }
}

private fun FrameLayout.showOnly(widgetView: AppWidgetHostView?) {
    for (index in childCount - 1 downTo 0) {
        if (getChildAt(index) !== widgetView) removeViewAt(index)
    }
    if (widgetView == null || widgetView.parent === this) return
    (widgetView.parent as? ViewGroup)?.removeView(widgetView)
    addView(
        widgetView,
        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
    )
}

/** Reads the height in the layout phase so a resize drag never recomposes the stack. */
private fun Modifier.stackHeight(heightDp: Density.() -> Dp) =
    layout { measurable, constraints ->
        val height = heightDp().roundToPx()
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

// Requires the app's single WidgetViewModel: a defaulted hiltViewModel() resolves per nav destination.
@Composable
fun WidgetStack(
    viewModel: WidgetViewModel,
    onTouchedChange: (Boolean) -> Unit = {},
    onManageWidgets: () -> Unit = {},
    // Preview mode: the resize handle is always shown and widget taps are blocked.
    previewMode: Boolean = false,
) {
    val widgetIds = viewModel.widgetIds

    if (widgetIds.isEmpty()) return

    var currentWidgetId by remember {
        mutableIntStateOf(widgetIds[viewModel.initialPageIndex.coerceIn(0, widgetIds.lastIndex)])
    }
    var editing by remember { mutableStateOf(false) }
    val pressSource = remember { MutableInteractionSource() }
    var stackWidthPx by remember { mutableIntStateOf(0) }
    val stackWidth = with(LocalDensity.current) { stackWidthPx.toDp() }
    val context = LocalContext.current
    val widgetViews = remember(context, viewModel) {
        WidgetViewCache { widgetId -> viewModel.createWidgetView(context, widgetId) }
    }
    LaunchedEffect(widgetIds) { widgetViews.retainOnly(widgetIds) }

    // Disposing the drag handle does not fire onDragEnd, so persist before tearing it down.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.commitStackHeight()
        editing = false
    }

    // Recreate the pager whenever the list changes so virtual pages always map
    // to a fixed list snapshot, re-seeded at the widget the user was viewing.
    key(widgetIds) {
        val pageCount = widgetIds.size
        val pagerState = rememberPagerState(
            initialPage = loopStartPage(
                pageCount = pageCount,
                initialIndex = widgetIds.indexOf(currentWidgetId).coerceAtLeast(0),
            ),
        ) { if (pageCount > 1) LOOP_PAGE_COUNT else pageCount }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                val widgetId = widgetIds[page % pageCount]
                currentWidgetId = widgetId
                viewModel.updateCurrentPage(widgetId)
            }
        }

        val flingBehavior = PagerDefaults.flingBehavior(state = pagerState)

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Corners.medium)
                    .stackHeight { viewModel.stackHeightDp.dp }
                    .onSizeChanged { stackWidthPx = it.width }
                    .then(
                        if (previewMode) {
                            Modifier
                        } else {
                            Modifier.captureLongPress(
                                interactionSource = pressSource,
                                enabled = { !editing },
                                onTouchedChange = onTouchedChange,
                            ) { editing = true }
                        }
                    ),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    flingBehavior = flingBehavior,
                ) { page ->
                    val widgetId = widgetIds[page % pageCount]
                    val isNearestCopy by remember(pagerState, page, pageCount) {
                        derivedStateOf { pagerState.isNearestCopyOfPage(page, pageCount) }
                    }
                    WidgetPage(
                        widgetView = remember(widgetViews, widgetId) { widgetViews.get(widgetId) },
                        showsWidgetView = isNearestCopy,
                        widthDp = stackWidth.value.toInt(),
                        viewModel = viewModel,
                        blockTouches = previewMode,
                    )
                }

                if (previewMode) {
                    StackResizeHandle(viewModel = viewModel)
                }

                if (editing || previewMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                Corners.medium,
                            ),
                    )
                }

                if (editing) {
                    Popup(
                        onDismissRequest = { editing = false },
                        properties = PopupProperties(focusable = true),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(stackWidth)
                                .stackHeight {
                                    viewModel.stackHeightDp.dp + DRAG_HANDLE_HEIGHT / 2
                                },
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = { editing = false })
                                    },
                            )
                            StackResizeHandle(viewModel = viewModel)
                        }
                    }
                }

                if (!previewMode) {
                    // Sibling so the ripple draws above the opaque interop widget views.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .indication(pressSource, ripple()),
                    )
                }
            }

            if (pageCount > 1) {
                PageDashes(
                    pagerState = pagerState,
                    pageCount = pageCount,
                    flingBehavior = flingBehavior,
                    onLongPress = onManageWidgets.takeUnless { previewMode },
                )
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun WidgetPage(
    widgetView: AppWidgetHostView?,
    showsWidgetView: Boolean,
    widthDp: Int,
    viewModel: WidgetViewModel,
    blockTouches: Boolean,
) {
    LaunchedEffect(widgetView, widthDp) {
        if (widgetView == null || widthDp <= 0) return@LaunchedEffect
        snapshotFlow { viewModel.stackHeightDp }
            .debounce(WIDGET_SIZE_DEBOUNCE_MS)
            .collect { widgetView.applyWidgetSize(widthDp, it) }
    }

    if (widgetView == null) {
        UnavailableWidget()
        return
    }
    Box {
        AndroidView(
            factory = { FrameLayout(it) },
            modifier = Modifier.fillMaxSize(),
            onRelease = { it.removeAllViews() },
            update = { it.showOnly(widgetView.takeIf { showsWidgetView }) },
        )
        if (blockTouches) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures(onTap = {}) },
            )
        }
    }
}

@Composable
private fun BoxScope.StackResizeHandle(viewModel: WidgetViewModel) {
    DragHandle(
        modifier = Modifier.align(Alignment.BottomCenter),
        currentHeightDp = { viewModel.stackHeightDp },
        onResize = viewModel::previewStackHeight,
        onResizeEnd = viewModel::commitStackHeight,
    )
}

@Composable
private fun UnavailableWidget() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Unable to load widget",
            modifier = Modifier.padding(Spacing.large),
        )
    }
}

@Composable
private fun DragHandle(
    modifier: Modifier,
    currentHeightDp: () -> Int,
    onResize: (Int) -> Unit,
    onResizeEnd: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(width = 120.dp, height = DRAG_HANDLE_HEIGHT)
            .pointerInput(Unit) {
                var pxAccum = 0f
                detectVerticalDragGestures(
                    onDragStart = { pxAccum = 0f },
                    onDragEnd = onResizeEnd,
                    onDragCancel = onResizeEnd,
                ) { _, dragAmount ->
                    pxAccum += dragAmount
                    val dpDelta = pxAccum.toDp().value.toInt()
                    if (dpDelta != 0) {
                        onResize(currentHeightDp() + dpDelta)
                        pxAccum -= dpDelta.dp.toPx()
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 56.dp, height = 5.dp)
                .background(
                    MaterialTheme.colorScheme.onSurface,
                    CircleShape,
                ),
        )
    }
}

private fun PagerState.nearestVirtualPage(targetIndex: Int, pageCount: Int): Int {
    var delta = targetIndex - currentPage % pageCount
    if (delta > pageCount / 2) delta -= pageCount
    if (delta < -pageCount / 2) delta += pageCount
    return currentPage + delta
}

@Composable
private fun PageDashes(
    pagerState: PagerState,
    pageCount: Int,
    flingBehavior: FlingBehavior,
    onLongPress: (() -> Unit)?,
) {
    val scope = rememberCoroutineScope()
    val pressSource = remember { MutableInteractionSource() }
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val dashInsetPx = with(LocalDensity.current) { Spacing.medium.toPx() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Corners.small)
            .indication(pressSource, ripple())
            .heightIn(min = PAGE_DASH_TOUCH_HEIGHT)
            .scrollable(
                state = pagerState,
                orientation = Orientation.Horizontal,
                reverseDirection = ScrollableDefaults.reverseDirection(
                    layoutDirection,
                    Orientation.Horizontal,
                    reverseScrolling = false,
                ),
                flingBehavior = flingBehavior,
            )
            .then(
                if (onLongPress == null) {
                    Modifier
                } else {
                    Modifier.captureLongPress(interactionSource = pressSource, onLongPress = onLongPress)
                }
            )
            .pointerInput(pageCount, isRtl, dashInsetPx) {
                detectTapGestures(
                    onTap = { offset ->
                        val dashesWidth = (size.width - 2 * dashInsetPx).coerceAtLeast(1f)
                        val visualIndex = ((offset.x - dashInsetPx) / dashesWidth * pageCount).toInt()
                            .coerceIn(0, pageCount - 1)
                        val tappedIndex = if (isRtl) pageCount - 1 - visualIndex else visualIndex
                        scope.launch {
                            pagerState.animateScrollToPage(
                                pagerState.nearestVirtualPage(tappedIndex, pageCount),
                            )
                        }
                    },
                )
            }
            .padding(start = Spacing.medium, top = PAGE_DASH_TOP_GAP, end = Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        val currentPage = pagerState.currentPage % pageCount
        val dotColor = MaterialTheme.colorScheme.primary
        repeat(pageCount) { index ->
            val color =
                if (index == currentPage) {
                    dotColor
                } else {
                    dotColor.copy(alpha = ContentAlpha.disabled)
                }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(color, CircleShape),
            )
        }
    }
}
