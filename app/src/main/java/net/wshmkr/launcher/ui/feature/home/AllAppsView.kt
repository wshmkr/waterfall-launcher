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
import androidx.compose.runtime.derivedStateOf
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
import net.wshmkr.launcher.model.AppInfo
import net.wshmkr.launcher.model.AppListItem
import net.wshmkr.launcher.ui.common.calculateCenteredContentTopPadding
import net.wshmkr.launcher.ui.common.components.AppListItem
import net.wshmkr.launcher.ui.common.components.animateLetterFilterAlpha
import net.wshmkr.launcher.ui.common.components.rememberLetterIndexedListState
import net.wshmkr.launcher.ui.common.icons.SearchIcon
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.allAppsFabBottom
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
    val activeLetter = viewModel.activeLetter
    val listItems = viewModel.allAppsListItems

    LaunchedEffect(listItems.isNotEmpty()) {
        if (listItems.isNotEmpty()) {
            isVisible = true
        }
    }

    val topPadding = calculateCenteredContentTopPadding()
    val dimensions = LocalDimensions.current
    val fabBottomOffset = dimensions.allAppsFabBottom()

    val listState = rememberLetterIndexedListState(
        activeLetter = activeLetter,
        getScrollPosition = viewModel::getScrollPosition,
    )

    val alphaByLetter by remember(viewModel) {
        derivedStateOf {
            viewModel.alphabetLetters.associateWith { viewModel.getAlpha(it) }
        }
    }

    val onClick = remember(viewModel) {
        { app: AppInfo -> viewModel.launchApp(app.packageName, app.userHandle) }
    }
    val onToggleFavorite = remember(viewModel) { viewModel::toggleFavorite }
    val onToggleHidden = remember(viewModel) { viewModel::toggleHidden }
    val onToggleSuggest = remember(viewModel) { viewModel::toggleSuggest }
    val onOpenSearch = remember(viewModel) { { viewModel.showSearchOverlay = true } }

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
                contentPadding = PaddingValues(vertical = topPadding, horizontal = dimensions.listHorizontalGutter)
            ) {
                items(
                    items = listItems,
                    key = { item ->
                        when (item) {
                            is AppListItem.SectionHeader -> "header_${item.letter}"
                            is AppListItem.AppItem -> item.appInfo.key
                        }
                    },
                    contentType = { it::class },
                ) { item ->
                    when (item) {
                        is AppListItem.SectionHeader -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeaderItem(
                                letter = item.letter,
                                targetAlpha = alphaByLetter[item.letter] ?: 1f,
                                isActiveLetter = item.letter == activeLetter,
                            )
                        }
                        is AppListItem.AppItem -> {
                            val notifications by viewModel
                                .notificationsFor(item.appInfo.packageName, item.appInfo.userHandle)
                                .collectAsState()
                            AppListItem(
                                appInfo = item.appInfo,
                                isActiveUser = item.appInfo.userHandle in activeProfiles,
                                onClick = onClick,
                                onToggleFavorite = onToggleFavorite,
                                onToggleHidden = onToggleHidden,
                                onToggleSuggest = onToggleSuggest,
                                targetAlpha = alphaByLetter[item.sectionLetter] ?: 1f,
                                isActiveLetter = item.sectionLetter == activeLetter,
                                notifications = notifications,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = activeLetter == null,
                enter = fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = fabBottomOffset, end = dimensions.allAppsFabEndOffset)
            ) {
                FloatingActionButton(
                    onClick = onOpenSearch,
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
    val dimensions = LocalDimensions.current

    Text(
        text = letter,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensions.sectionHeaderStartPadding,
                end = dimensions.sectionHeaderEndPadding,
                top = dimensions.sectionHeaderTopPadding,
                bottom = dimensions.sectionHeaderBottomPadding,
            )
            .alpha(animatedAlpha),
        fontSize = dimensions.sectionHeaderFont,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}
