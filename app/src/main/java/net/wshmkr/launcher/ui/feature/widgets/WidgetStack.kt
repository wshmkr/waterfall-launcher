package net.wshmkr.launcher.ui.feature.widgets

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import net.wshmkr.launcher.viewmodel.WidgetViewModel

// Virtual page count for wrap-around scrolling; real page = virtual page % widget count.
private const val LOOP_PAGE_COUNT = Int.MAX_VALUE

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
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                beyondViewportPageCount = 1,
            ) { page ->
                WidgetPage(
                    widgetId = widgetIds[page % pageCount],
                    viewModel = viewModel,
                )
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
    viewModel: WidgetViewModel,
) {
    AndroidView<AppWidgetHostView>(
        factory = { ctx ->
            val widgetView = viewModel.createWidgetView(ctx, widgetId)
                ?: unavailableWidgetView(ctx)

            widgetView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )

            widgetView
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PageDashes(
    pagerState: PagerState,
    pageCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
