package net.wshmkr.launcher.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.wshmkr.launcher.model.ListItem
import net.wshmkr.launcher.ui.common.components.AppListItem
import net.wshmkr.launcher.viewmodel.HomeViewModel

@Composable
fun AllAppsView(
    viewModel: HomeViewModel,
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val topPadding = screenHeight * 0.2f

    val initialPosition = if (viewModel.activeLetter != null && viewModel.activeLetter != STAR_SYMBOL) {
        val header = viewModel.allAppsListItems.find { 
            it is ListItem.SectionHeader && it.letter == viewModel.activeLetter 
        } as? ListItem.SectionHeader
        header?.position ?: 0
    } else {
        0
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPosition)
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        viewModel.setScrollCallback { position ->
            coroutineScope.launch {
                listState.scrollToItem(position, scrollOffset = 0)
            }
        }
        onDispose {
            viewModel.setScrollCallback { }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0f, 0f, 0f, 0.5f))
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = topPadding, bottom = topPadding)
        ) {
            items(
                items = viewModel.allAppsListItems,
                key = { item ->
                    when (item) {
                        is ListItem.SectionHeader -> "header_${item.letter}"
                        is ListItem.AppItem -> item.appInfo.packageName
                        else -> item.hashCode()
                    }
                },
            ) { item ->
                when (item) {
                    is ListItem.SectionHeader -> {
                        SectionHeaderItem(
                            letter = item.letter,
                            alpha = viewModel.getAlpha(item.letter),
                        )
                    }
                    is ListItem.AppItem -> {
                        AppListItem(
                            appInfo = item.appInfo,
                            alpha = viewModel.getAlpha(
                                item.appInfo.label.first().uppercaseChar().toString()
                            ),
                            viewModel = viewModel,
                        )
                    }
                    else -> null
                }
            }
        }

        SmallFloatingActionButton(
            onClick = { viewModel.showSearchOverlay = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 48.dp, end = 64.dp)
        ) {
            Icon(Icons.Filled.Search, "Search")
        }
    }
}

@Composable
fun SectionHeaderItem(letter: String, alpha: Float) {
    Text(
        text = letter,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
            .alpha(alpha),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}
