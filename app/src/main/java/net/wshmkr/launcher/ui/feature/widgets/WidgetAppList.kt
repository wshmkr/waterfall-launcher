package net.wshmkr.launcher.ui.feature.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import net.wshmkr.launcher.ui.feature.home.SectionHeaderItem
import net.wshmkr.launcher.viewmodel.WidgetAppListItem
import net.wshmkr.launcher.viewmodel.WidgetViewModel

@Composable
fun WidgetAppList(
    viewModel: WidgetViewModel,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onWidgetSelected: () -> Unit = {},
) {
    BackHandler(enabled = true) {
        onDismiss()
    }

    val widgetListItems = viewModel.widgetAppListItems
    var expandedProviders by remember { mutableStateOf(setOf<String>()) }
    fun toggleProvider(packageName: String) {
        expandedProviders = if (expandedProviders.contains(packageName)) {
            expandedProviders - packageName
        } else {
            expandedProviders + packageName
        }
    }

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val topPadding = screenHeight * 0.25f

    val listState = rememberLazyListState()

    AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = androidx.compose.animation.fadeOut(animationSpec = tween(durationMillis = 150))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0f, 0f, 0f, 0.5f))
        ) {
            if (widgetListItems.isEmpty()) {
                Text(
                    text = "No widget-enabled apps found",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = topPadding, horizontal = 32.dp)
                ) {
                    items(
                        items = widgetListItems,
                        key = { item ->
                            when (item) {
                                is WidgetAppListItem.SectionHeader -> "widget_header_${item.letter}"
                                is WidgetAppListItem.Provider -> "widget_provider_${item.packageName}"
                            }
                        }
                    ) { listItem ->
                        when (listItem) {
                            is WidgetAppListItem.SectionHeader -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionHeaderItem(
                                    letter = listItem.letter,
                                    targetAlpha = 1f,
                                    isActiveLetter = false
                                )
                            }
                            is WidgetAppListItem.Provider -> {
                                val isExpanded = expandedProviders.contains(listItem.packageName)
                                WidgetProviderGroup(
                                    provider = listItem,
                                    isExpanded = isExpanded,
                                    onProviderClick = { toggleProvider(listItem.packageName) },
                                    onWidgetSelected = {
                                        viewModel.onWidgetProviderSelected(listItem.packageName)
                                        onWidgetSelected()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetProviderGroup(
    provider: WidgetAppListItem.Provider,
    isExpanded: Boolean,
    onProviderClick: () -> Unit,
    onWidgetSelected: () -> Unit,
) {
    Column {
        WidgetProviderRow(
            provider = provider,
            isExpanded = isExpanded,
            onClick = onProviderClick
        )
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 32.dp, top = 8.dp)
            ) {
                provider.widgetNames.forEachIndexed { index, widgetName ->
                    WidgetListItem(
                        widgetName = widgetName,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onWidgetSelected
                    )
                    if (index != provider.widgetNames.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetProviderRow(
    provider: WidgetAppListItem.Provider,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 250),
        label = "widget_row_alpha"
    )

    Row(
        modifier = modifier
            .padding(start = 8.dp, end = 32.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(animatedAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProviderIcon(provider)
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = provider.label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = widgetCountLabel(provider.widgetCount),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        Text(
            text = if (isExpanded) "−" else "+",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ProviderIcon(provider: WidgetAppListItem.Provider) {
    val iconDrawable = provider.icon
    if (iconDrawable != null) {
        Image(
            painter = rememberDrawablePainter(drawable = iconDrawable),
            contentDescription = provider.label,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = provider.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun widgetCountLabel(count: Int): String {
    return if (count == 1) {
        "1 widget available"
    } else {
        "$count widgets available"
    }
}