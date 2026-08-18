package net.wshmkr.launcher.ui.feature.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing

// Traced over the favorite rows only, so it lines up with them rather than the wider widget area.
@Composable
fun FavoritesOutline(listState: LazyListState) {
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
