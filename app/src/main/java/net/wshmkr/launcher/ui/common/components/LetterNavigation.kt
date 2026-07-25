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

// One animation for a whole letter-indexed list: rows read it instead of each animating their own.
@Composable
fun animateLetterDimAlpha(activeLetter: String?): State<Float> = animateFloatAsState(
    targetValue = if (activeLetter == null) 1f else DIMMED_LETTER_ALPHA,
    animationSpec = if (activeLetter == null) tween(durationMillis = 300) else snap(),
    label = "letter_dim_alpha",
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

private const val DIMMED_LETTER_ALPHA = 0.2f
