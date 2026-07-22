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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import net.wshmkr.launcher.viewmodel.WidgetViewModel

@Composable
fun WidgetStack(
    viewModel: WidgetViewModel = hiltViewModel(),
) {
    val widgetIds = viewModel.widgetIds

    if (widgetIds.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = viewModel.initialPageIndex.coerceIn(0, widgetIds.lastIndex),
    ) { widgetIds.size }

    LaunchedEffect(pagerState, viewModel) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.widgetIds.getOrNull(page)?.let(viewModel::updateCurrentPage)
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
            key = { widgetIds[it] },
        ) { page ->
            WidgetPage(
                widgetId = widgetIds[page],
                viewModel = viewModel,
            )
        }

        if (widgetIds.size > 1) {
            Spacer(modifier = Modifier.height(4.dp))
            PageDashes(pagerState = pagerState)
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
private fun PageDashes(pagerState: PagerState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(pagerState.pageCount) { index ->
            val color =
                if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.3f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(color),
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
