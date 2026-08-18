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
import net.wshmkr.launcher.ui.theme.Dimensions
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing

// The list gutter plus the insets AppListItem applies on top of it.
internal val Dimensions.favoritesOutlineStart get() = gutterLarge + Spacing.small
internal val Dimensions.favoritesOutlineEnd get() = gutterLarge * 2

internal fun favoriteRowsBounds(listState: LazyListState): Pair<Int, Int>? {
    var first: LazyListItemInfo? = null
    var last: LazyListItemInfo? = null
    for (row in listState.layoutInfo.visibleItemsInfo) {
        if (row.contentType != FAVORITE_CONTENT_TYPE) continue
        if (first == null) first = row
        last = row
    }
    if (first == null || last == null) return null
    return first.offset to (last.offset + last.size - first.offset)
}

@Composable
fun FavoritesOutline(listState: LazyListState) {
    val dimensions = LocalDimensions.current
    val bounds by remember(listState) { derivedStateOf { favoriteRowsBounds(listState) } }
    val (top, height) = bounds ?: return

    Box(
        modifier = Modifier
            .offset { IntOffset(0, top) }
            .fillMaxWidth()
            .height(with(LocalDensity.current) { height.toDp() })
            .padding(start = dimensions.favoritesOutlineStart, end = dimensions.favoritesOutlineEnd)
            .border(1.dp, MaterialTheme.colorScheme.outline, Corners.medium),
    )
}
