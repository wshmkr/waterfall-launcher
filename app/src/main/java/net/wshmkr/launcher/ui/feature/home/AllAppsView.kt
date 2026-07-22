package net.wshmkr.launcher.ui.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.wshmkr.launcher.model.AppListItem
import net.wshmkr.launcher.ui.common.calculateCenteredContentTopPadding
import net.wshmkr.launcher.ui.common.components.AppListItem
import net.wshmkr.launcher.ui.common.components.animateLetterFilterAlpha
import net.wshmkr.launcher.ui.common.components.rememberLetterIndexedListState
import net.wshmkr.launcher.ui.common.icons.SearchIcon
import net.wshmkr.launcher.viewmodel.HomeViewModel

@Composable
fun AllAppsView(
    viewModel: HomeViewModel,
) {
    BackHandler {
        viewModel.navigateToFavorites()
    }

    var isVisible by remember { mutableStateOf(false) }
    val activeProfiles by viewModel.activeProfiles.collectAsState()

    LaunchedEffect(viewModel.allAppsListItems.isNotEmpty()) {
        if (viewModel.allAppsListItems.isNotEmpty()) {
            isVisible = true
        }
    }

    val topPadding = calculateCenteredContentTopPadding()

    val listState = rememberLetterIndexedListState(
        activeLetter = viewModel.activeLetter,
        getScrollPosition = viewModel::getScrollPosition,
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0f, 0f, 0f, 0.5f))
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = topPadding, horizontal = 32.dp)
            ) {
                items(
                    items = viewModel.allAppsListItems,
                    key = { item ->
                        when (item) {
                            is AppListItem.SectionHeader -> "header_${item.letter}"
                            is AppListItem.AppItem -> item.appInfo.key
                        }
                    },
                ) { item ->
                    when (item) {
                        is AppListItem.SectionHeader -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeaderItem(
                                letter = item.letter,
                                targetAlpha = viewModel.getAlpha(item.letter),
                                isActiveLetter = item.letter == viewModel.activeLetter,
                            )
                        }
                        is AppListItem.AppItem -> {
                            AppListItem(
                                appInfo = item.appInfo,
                                isActiveUser = item.appInfo.userHandle in activeProfiles,
                                onClick = { viewModel.launchApp(it.packageName, it.userHandle) },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onToggleHidden = viewModel::toggleHidden,
                                onToggleSuggest = viewModel::toggleSuggest,
                                targetAlpha = viewModel.getAlpha(item.sectionLetter),
                                isActiveLetter = item.sectionLetter == viewModel.activeLetter,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = viewModel.activeLetter == null,
                enter = fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 48.dp, end = 64.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.showSearchOverlay = true },
                    shape = CircleShape
                ) {
                    Icon(painter = SearchIcon(), contentDescription = "Search")
                }
            }
        }
    }
}

@Composable
fun SectionHeaderItem(letter: String, targetAlpha: Float, isActiveLetter: Boolean) {
    val animatedAlpha by animateLetterFilterAlpha(
        targetAlpha = targetAlpha,
        isActiveLetter = isActiveLetter,
        label = "section_header_alpha"
    )

    Text(
        text = letter,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
            .alpha(animatedAlpha),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}
