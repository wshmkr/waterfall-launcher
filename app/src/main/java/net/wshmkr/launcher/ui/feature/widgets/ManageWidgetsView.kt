package net.wshmkr.launcher.ui.feature.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.collections.immutable.ImmutableList
import net.wshmkr.launcher.datastore.WidgetDataSource
import net.wshmkr.launcher.ui.common.calculateCenteredContentTopPadding
import net.wshmkr.launcher.ui.common.components.ReorderHandle
import net.wshmkr.launcher.ui.common.components.reorderActions
import net.wshmkr.launcher.ui.common.icons.AddIcon
import net.wshmkr.launcher.ui.common.icons.DeleteIcon
import net.wshmkr.launcher.ui.theme.Corners
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.viewmodel.ManagedWidget
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val MIN_REORDERABLE_WIDGETS = 2

private val PreviewThumbnailWidth = 72.dp
private val PreviewThumbnailHeight = 56.dp

@Composable
fun ManageWidgetsView(
    managedWidgets: ImmutableList<ManagedWidget>,
    onAddWidget: () -> Unit,
    onDeleteWidget: (Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onCommitOrder: () -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromId = from.key as? Int
        val toId = to.key as? Int
        if (fromId != null && toId != null) {
            onMoveWidget(fromId, toId)
        }
    }
    val reorderable = managedWidgets.size >= MIN_REORDERABLE_WIDGETS

    // A drag torn down with the screen never stops cleanly, so the order is also committed here.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        onCommitOrder()
    }
    DisposableEffect(onCommitOrder) {
        onDispose { onCommitOrder() }
    }

    val topPadding = calculateCenteredContentTopPadding()
    val dimensions = LocalDimensions.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = dimensions.pagePadding,
                end = dimensions.pagePadding,
                top = topPadding,
                bottom = Spacing.medium,
            )
    ) {
        Text(
            text = "Manage Widgets",
            fontSize = dimensions.fontTitle,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = managedWidgets,
                    key = { _, item -> item.widgetId }
                ) { index, widget ->
                    ManagedWidgetRow(
                        item = widget,
                        previousId = managedWidgets.getOrNull(index - 1)?.widgetId,
                        nextId = managedWidgets.getOrNull(index + 1)?.widgetId,
                        reorderable = reorderable,
                        reorderState = reorderState,
                        onDelete = onDeleteWidget,
                        onMove = onMoveWidget,
                        onCommitOrder = onCommitOrder,
                    )
                }

                if (managedWidgets.size < WidgetDataSource.MAX_WIDGETS) {
                    item {
                        AddWidgetRow(onClick = onAddWidget)
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.ManagedWidgetRow(
    item: ManagedWidget,
    previousId: Int?,
    nextId: Int?,
    reorderable: Boolean,
    reorderState: ReorderableLazyListState,
    onDelete: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onCommitOrder: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    ReorderableItem(
        state = reorderState,
        key = item.widgetId,
        enabled = reorderable,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetPreviewThumbnail(item = item)

            Spacer(modifier = Modifier.width(Spacing.medium))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.widgetName,
                    fontSize = dimensions.fontMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.appName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = dimensions.fontCaption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val onDeleteClick = remember(item.widgetId, onDelete) {
                { onDelete(item.widgetId) }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = DeleteIcon(),
                    contentDescription = "Remove widget",
                )
            }

            if (reorderable) {
                val actions = remember(item.widgetId, previousId, nextId, onMove) {
                    reorderActions(item.widgetId, previousId, nextId, onMove)
                }
                ReorderHandle(
                    label = item.widgetName,
                    modifier = Modifier
                        .draggableHandle(onDragStopped = onCommitOrder)
                        .semantics { customActions = actions },
                )
            }
        }
    }
}

@Composable
private fun WidgetPreviewThumbnail(item: ManagedWidget) {
    val previewDrawable by rememberWidgetPreviewDrawable(item.providerInfo)
    val thumbnailModifier = Modifier
        .size(width = PreviewThumbnailWidth, height = PreviewThumbnailHeight)
        .clip(Corners.small)

    val drawable = previewDrawable
    if (drawable != null) {
        Image(
            painter = rememberDrawablePainter(drawable = drawable),
            contentDescription = item.widgetName,
            modifier = thumbnailModifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = thumbnailModifier
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        )
    }
}

@Composable
private fun AddWidgetRow(
    onClick: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Corners.large)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.medium, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = AddIcon(),
            contentDescription = "Add widget",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dimensions.iconSmall)
        )

        Spacer(modifier = Modifier.width(Spacing.medium))

        Text(
            text = "Add Widget",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = dimensions.fontMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
