package net.wshmkr.launcher.ui.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember

@Composable
fun animateLetterFilterAlpha(
    targetAlpha: Float,
    isActiveLetter: Boolean,
    label: String = "letter_filter_alpha",
): State<Float> = animateFloatAsState(
    targetValue = targetAlpha,
    animationSpec = if (isActiveLetter || targetAlpha < 1f) {
        snap()
    } else {
        tween(durationMillis = 300)
    },
    label = label,
)

@Composable
fun rememberLetterIndexedListState(
    activeLetter: String?,
    getScrollPosition: (String) -> Int?,
): LazyListState {
    val initialPosition = remember { activeLetter?.let(getScrollPosition) ?: 0 }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPosition)

    LaunchedEffect(activeLetter) {
        activeLetter?.let { letter ->
            getScrollPosition(letter)?.let { position ->
                listState.scrollToItem(position, scrollOffset = 0)
            }
        }
    }

    return listState
}
