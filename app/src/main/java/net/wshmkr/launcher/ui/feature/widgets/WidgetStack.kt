package net.wshmkr.launcher.ui.feature.widgets

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withTimeoutOrNull
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.viewmodel.WidgetViewModel

// Virtual page count for wrap-around scrolling; real page = virtual page % widget count.
private const val LOOP_PAGE_COUNT = Int.MAX_VALUE

// Debounce for AppWidgetManager.updateAppWidgetOptions during drag; per-frame IPC
// storms caused ANRs on some devices.
private const val WIDGET_SIZE_DEBOUNCE_MS = 80L

private fun loopStartPage(pageCount: Int, initialIndex: Int): Int {
    if (pageCount <= 1) return initialIndex
    val mid = LOOP_PAGE_COUNT / 2
    return mid - mid % pageCount + initialIndex
}

@Composable
fun WidgetStack(
    viewModel: WidgetViewModel = hiltViewModel(),
) {
    val widgetIds = viewModel.widgetIds

    if (widgetIds.isEmpty()) return

    var currentWidgetId by remember {
        mutableIntStateOf(widgetIds[viewModel.initialPageIndex.coerceIn(0, widgetIds.lastIndex)])
    }
    var editing by remember { mutableStateOf(false) }

    val heightDp = viewModel.stackHeightDp
    val hapticFeedback = LocalHapticFeedback.current

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

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp.dp),
            ) {
                val slotWidthDp = maxWidth.value.toInt()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (editing) {
                                Modifier.border(1.dp, Color.White.copy(alpha = 0.5f))
                            } else {
                                Modifier
                            }
                        )
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                                if (editing) return@awaitEachGesture
                                val timeoutMs = viewConfiguration.longPressTimeoutMillis
                                val slop = viewConfiguration.touchSlop
                                val slopSquared = slop * slop
                                val startPos = down.position
                                val outcome = withTimeoutOrNull(timeoutMs) {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: return@withTimeoutOrNull Unit
                                        if (!change.pressed) return@withTimeoutOrNull Unit
                                        val dx = change.position.x - startPos.x
                                        val dy = change.position.y - startPos.y
                                        if (dx * dx + dy * dy > slopSquared) {
                                            return@withTimeoutOrNull Unit
                                        }
                                    }
                                    @Suppress("UNREACHABLE_CODE")
                                    Unit
                                }
                                if (outcome != null) return@awaitEachGesture
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                editing = true
                                // Consume the rest of the gesture; the interop layer then sends
                                // ACTION_CANCEL to the widget view instead of a click.
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) break
                                }
                            }
                        },
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                    ) { page ->
                        WidgetPage(
                            widgetId = widgetIds[page % pageCount],
                            widthDp = slotWidthDp,
                            heightDp = heightDp,
                            viewModel = viewModel,
                        )
                    }

                    if (editing) {
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

            if (pageCount > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                PageDashes(pagerState = pagerState, pageCount = pageCount)
            }
        }
    }
}

@Composable
private fun WidgetPage(
    widgetId: Int,
    widthDp: Int,
    heightDp: Int,
    viewModel: WidgetViewModel,
) {
    LaunchedEffect(widgetId, widthDp) {
        if (widthDp <= 0) return@LaunchedEffect
        snapshotFlow { viewModel.stackHeightDp }
            .debounce(WIDGET_SIZE_DEBOUNCE_MS)
            .collect { h ->
                if (h > 0) viewModel.applyWidgetSize(widgetId, widthDp, h)
            }
    }
    AndroidView<AppWidgetHostView>(
        factory = { ctx ->
            val widgetView = viewModel.createWidgetView(ctx, widgetId)
                ?: unavailableWidgetView(ctx)

            widgetView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )

            if (widthDp > 0 && heightDp > 0) {
                viewModel.applyWidgetSize(widgetId, widthDp, heightDp)
            }
            widgetView
        },
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
            .size(width = 120.dp, height = 32.dp)
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
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
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
            .padding(horizontal = Spacing.medium),
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
