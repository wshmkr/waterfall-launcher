package net.wshmkr.launcher.ui.feature.widgets

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.flow.debounce
import net.wshmkr.launcher.ui.common.gesture.captureLongPress
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.viewmodel.WidgetViewModel

// Virtual page count for wrap-around scrolling; real page = virtual page % widget count.
private const val LOOP_PAGE_COUNT = Int.MAX_VALUE

private const val WIDGET_SIZE_DEBOUNCE_MS = 80L

private val DRAG_HANDLE_HEIGHT = 32.dp

private fun loopStartPage(pageCount: Int, initialIndex: Int): Int {
    if (pageCount <= 1) return initialIndex
    val mid = LOOP_PAGE_COUNT / 2
    return mid - mid % pageCount + initialIndex
}

@Composable
fun WidgetStack(
    viewModel: WidgetViewModel = hiltViewModel(),
    interactionSource: MutableInteractionSource? = null,
    onTouchedChange: (Boolean) -> Unit = {},
) {
    val widgetIds = viewModel.widgetIds

    if (widgetIds.isEmpty()) return

    var currentWidgetId by remember {
        mutableIntStateOf(widgetIds[viewModel.initialPageIndex.coerceIn(0, widgetIds.lastIndex)])
    }
    var editing by remember { mutableStateOf(false) }

    // Disposing the drag handle does not fire onDragEnd, so persist before tearing it down.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        if (editing) {
            viewModel.commitStackHeight()
            editing = false
        }
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

        val fallbackSource = remember { MutableInteractionSource() }
        val pressSource = interactionSource ?: fallbackSource

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Corners.medium)
                .captureLongPress(
                    enabled = { !editing },
                    interactionSource = pressSource,
                    onTouchedChange = onTouchedChange,
                ) { editing = true },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Read in the layout phase so a resize drag never recomposes the pager.
                        .layout { measurable, constraints ->
                            val height = viewModel.stackHeightDp.dp.roundToPx()
                            val placeable = measurable.measure(
                                constraints.copy(minHeight = height, maxHeight = height),
                            )
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        },
                ) {
                    val stackWidth = maxWidth
                    val slotWidthDp = stackWidth.value.toInt()

                    Box(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(Corners.medium),
                            beyondViewportPageCount = 1,
                        ) { page ->
                            WidgetPage(
                                widgetId = widgetIds[page % pageCount],
                                widthDp = slotWidthDp,
                                viewModel = viewModel,
                            )
                        }

                        if (editing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), Corners.medium),
                            )
                            Popup(
                                onDismissRequest = { editing = false },
                                properties = PopupProperties(focusable = true),
                            ) {
                                Box(
                                    modifier = Modifier.layout { measurable, _ ->
                                        val width = stackWidth.roundToPx()
                                        val height = (
                                            viewModel.stackHeightDp.dp + DRAG_HANDLE_HEIGHT / 2
                                            ).roundToPx()
                                        val placeable =
                                            measurable.measure(Constraints.fixed(width, height))
                                        layout(width, height) { placeable.place(0, 0) }
                                    },
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(Unit) {
                                                detectTapGestures(onTap = { editing = false })
                                            },
                                    )
                                    DragHandle(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        currentHeightDp = { viewModel.stackHeightDp },
                                        onResize = viewModel::previewStackHeight,
                                        onResizeEnd = viewModel::commitStackHeight,
                                    )
                                }
                            }
                        }
                    }
                }

                if (pageCount > 1) {
                    PageDashes(pagerState = pagerState, pageCount = pageCount)
                }
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .indication(pressSource, ripple()),
            )
        }
    }
}

@Composable
private fun WidgetPage(
    widgetId: Int,
    widthDp: Int,
    viewModel: WidgetViewModel,
) {
    val context = LocalContext.current
    val widgetView = remember(widgetId) {
        (viewModel.createWidgetView(context, widgetId) ?: unavailableWidgetView(context)).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    LaunchedEffect(widgetView, widthDp) {
        if (widthDp <= 0) return@LaunchedEffect
        snapshotFlow { viewModel.stackHeightDp }
            .debounce(WIDGET_SIZE_DEBOUNCE_MS)
            .collect { h ->
                if (h > 0) viewModel.applyWidgetSize(widgetView, widgetId, widthDp, h)
            }
    }

    AndroidView(
        factory = { widgetView },
        modifier = Modifier.fillMaxSize(),
    )
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
                    onDragEnd = {
                        pxAccum = 0f
                        onResizeEnd()
                    },
                    onDragCancel = {
                        pxAccum = 0f
                        onResizeEnd()
                    },
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
                .background(Color.White.copy(alpha = 0.9f), CircleShape),
        )
    }
}

@Composable
private fun PageDashes(
    pagerState: PagerState,
    pageCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        val currentPage = pagerState.currentPage % pageCount
        repeat(pageCount) { index ->
            val color =
                if (index == currentPage) Color.White else Color.White.copy(alpha = 0.3f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

private fun unavailableWidgetView(context: Context): AppWidgetHostView {
    return AppWidgetHostView(context).apply {
        addView(
            TextView(context).apply {
                text = "Unable to load widget"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 16f
                setPadding(24, 24, 24, 24)
            }
        )
    }
}
