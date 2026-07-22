package net.wshmkr.launcher.ui.feature.widgets

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.collections.immutable.ImmutableList
import net.wshmkr.launcher.datastore.WidgetDataSource
import net.wshmkr.launcher.ui.common.calculateCenteredContentTopPadding
import net.wshmkr.launcher.ui.common.icons.AddIcon
import net.wshmkr.launcher.ui.common.icons.DeleteIcon
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.viewmodel.ManagedWidget

@Composable
fun ManageWidgetsView(
    managedWidgets: ImmutableList<ManagedWidget>,
    onAddWidget: () -> Unit,
    onDeleteWidget: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val topPadding = calculateCenteredContentTopPadding()
    val dimensions = LocalDimensions.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = dimensions.manageWidgetsPagePadding,
                end = dimensions.manageWidgetsPagePadding,
                top = topPadding,
                bottom = 16.dp,
            )
    ) {
        Text(
            text = "Manage Widgets",
            fontSize = dimensions.manageWidgetsTitleFont,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(dimensions.manageWidgetsTitleGap))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = managedWidgets,
                    key = { item -> item.widgetId }
                ) { widget ->
                    val onDelete = remember(widget.widgetId, onDeleteWidget) {
                        { onDeleteWidget(widget.widgetId) }
                    }
                    ManagedWidgetRow(
                        item = widget,
                        onDelete = onDelete,
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
private fun ManagedWidgetRow(
    item: ManagedWidget,
    onDelete: () -> Unit,
) {
    val dimensions = LocalDimensions.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = item.appIcon),
            contentDescription = item.appName,
            modifier = Modifier
                .size(dimensions.managedWidgetIconSize)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.widgetName,
                color = Color.White,
                fontSize = dimensions.managedWidgetNameFont,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.appName,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = dimensions.managedWidgetSubtitleFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                painter = DeleteIcon(),
                contentDescription = "Remove widget",
                tint = Color.White
            )
        }
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = AddIcon(),
            contentDescription = "Add widget",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(dimensions.addWidgetIconSize)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Add Widget",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = dimensions.addWidgetLabelFont,
            fontWeight = FontWeight.Medium
        )
    }
}
