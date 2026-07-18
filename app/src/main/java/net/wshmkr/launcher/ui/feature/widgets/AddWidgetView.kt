package net.wshmkr.launcher.ui.feature.widgets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.wshmkr.launcher.ui.common.calculateCenteredContentTopPadding
import net.wshmkr.launcher.ui.common.components.AlphabetSlider
import net.wshmkr.launcher.ui.common.components.rememberLetterIndexedListState
import net.wshmkr.launcher.viewmodel.WidgetViewModel

@Composable
fun AddWidgetView(
    viewModel: WidgetViewModel,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    BackHandler(enabled = true) {
        onDismiss()
    }

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.widgetAppListItems) {
        isVisible = viewModel.widgetAppListItems.isNotEmpty()
    }

    val listState = rememberLetterIndexedListState(
        activeLetter = viewModel.activeLetter,
        getScrollPosition = viewModel::getScrollPosition,
    )

    val topPadding = calculateCenteredContentTopPadding()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 150))
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            WidgetAppList(
                viewModel = viewModel,
                listState = listState,
                contentPadding = PaddingValues(vertical = topPadding, horizontal = 32.dp),
                onWidgetSelected = onDismiss
            )

            if (viewModel.alphabetLetters.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    AlphabetSlider(
                        letters = viewModel.alphabetLetters,
                        onLetterSelected = { letter -> viewModel.scrollToLetter(letter) },
                        onSelectionCleared = { viewModel.deselectLetter() }
                    )
                }
            }
        }
    }
}

