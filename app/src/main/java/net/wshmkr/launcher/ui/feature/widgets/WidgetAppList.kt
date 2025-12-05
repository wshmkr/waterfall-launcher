package net.wshmkr.launcher.ui.feature.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.wshmkr.launcher.ui.feature.home.SectionHeaderItem
import net.wshmkr.launcher.viewmodel.WidgetAppListItem
import net.wshmkr.launcher.viewmodel.WidgetViewModel

@Composable
fun WidgetAppList(
    viewModel: WidgetViewModel,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 32.dp),
    onWidgetSelected: () -> Unit = {},
) {
    val widgetListItems = viewModel.widgetAppListItems
    var expandedProviders by remember { mutableStateOf(setOf<String>()) }
    fun toggleProvider(packageName: String) {
        expandedProviders = if (expandedProviders.contains(packageName)) {
            expandedProviders - packageName
        } else {
            expandedProviders + packageName
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
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
                contentPadding = contentPadding
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
                                targetAlpha = viewModel.getAlpha(listItem.letter),
                                isActiveLetter = listItem.letter == viewModel.activeLetter
                            )
                        }

                        is WidgetAppListItem.Provider -> {
                            val isExpanded = expandedProviders.contains(listItem.packageName)
                            val letter = listItem.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                            val targetAlpha = viewModel.getAlpha(letter)
                            val isActiveLetter = viewModel.activeLetter == letter
                            WidgetProviderGroup(
                                provider = listItem,
                                isExpanded = isExpanded,
                                targetAlpha = targetAlpha,
                                isActiveLetter = isActiveLetter,
                                onProviderClick = { toggleProvider(listItem.packageName) },
                                onWidgetSelected = { option ->
                                    viewModel.onWidgetOptionSelected(option)
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
