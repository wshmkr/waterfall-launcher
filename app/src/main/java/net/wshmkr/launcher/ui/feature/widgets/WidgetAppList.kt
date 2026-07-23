package net.wshmkr.launcher.ui.feature.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.wshmkr.launcher.ui.feature.home.SectionHeaderItem
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.Spacing
import net.wshmkr.launcher.viewmodel.WidgetAppListItem
import net.wshmkr.launcher.viewmodel.WidgetOption
import net.wshmkr.launcher.viewmodel.WidgetViewModel

@Composable
fun WidgetAppList(
    viewModel: WidgetViewModel,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = LocalDimensions.current.gutterLarge),
    onWidgetSelected: () -> Unit = {},
) {
    val widgetListItems = viewModel.widgetAppListItems
    // Per-key snapshot map: expanding one provider only invalidates readers of that key.
    val expandedProviders = remember { mutableStateMapOf<String, Boolean>() }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        if (widgetListItems.isEmpty()) {
            Text(
                text = "No widget-enabled apps found",
                color = Color.White,
                fontSize = LocalDimensions.current.fontLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = contentPadding
            ) {
                itemsIndexed(
                    items = widgetListItems,
                    key = { _, item ->
                        when (item) {
                            is WidgetAppListItem.SectionHeader -> "widget_header_${item.letter}"
                            is WidgetAppListItem.Provider -> "widget_provider_${item.packageName}"
                        }
                    }
                ) { index, listItem ->
                    when (listItem) {
                        is WidgetAppListItem.SectionHeader -> {
                            Spacer(modifier = Modifier.height(Spacing.small))
                            SectionHeaderItem(
                                letter = listItem.letter,
                                targetAlpha = viewModel.getAlpha(listItem.letter),
                                isActiveLetter = listItem.letter == viewModel.activeLetter
                            )
                        }

                        is WidgetAppListItem.Provider -> {
                            val isExpanded = expandedProviders[listItem.packageName] == true
                            val targetAlpha = viewModel.getAlpha(listItem.letter)
                            val isActiveLetter = viewModel.activeLetter == listItem.letter
                            val onProviderClick = remember(listItem.packageName) {
                                {
                                    val pkg = listItem.packageName
                                    expandedProviders[pkg] = !(expandedProviders[pkg] == true)
                                }
                            }
                            val onWidgetClick = remember(listItem.packageName, onWidgetSelected) {
                                { option: WidgetOption ->
                                    viewModel.onWidgetOptionSelected(option)
                                    onWidgetSelected()
                                }
                            }
                            WidgetProviderGroup(
                                provider = listItem,
                                isExpanded = isExpanded,
                                targetAlpha = targetAlpha,
                                isActiveLetter = isActiveLetter,
                                onProviderClick = onProviderClick,
                                onWidgetSelected = onWidgetClick
                            )

                            if (index < widgetListItems.size - 1 && widgetListItems[index + 1] is WidgetAppListItem.Provider) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
