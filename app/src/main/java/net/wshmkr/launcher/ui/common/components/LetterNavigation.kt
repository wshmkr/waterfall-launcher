package net.wshmkr.launcher.ui.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

// One animation for a whole letter-indexed list: rows read it instead of each animating their own.
@Composable
fun rememberLetterAlpha(activeLetter: String?): (String) -> Float {
    val dimAlpha = animateFloatAsState(
        targetValue = if (activeLetter == null) 1f else DIMMED_LETTER_ALPHA,
        animationSpec = if (activeLetter == null) tween(durationMillis = 300) else snap(),
        label = "letter_dim_alpha",
    )
    // Outlives the scrub so the section jumped to stays opaque instead of fading in from dim on release.
    // Set from an effect, not composition, so it lands in the same dispatch as the scroll in
    // rememberLetterIndexedListState — applied during composition it lights the letter a frame early.
    val opaqueLetter = remember { mutableStateOf(activeLetter) }
    LaunchedEffect(activeLetter) {
        if (activeLetter != null) {
            opaqueLetter.value = activeLetter
        }
    }

    return remember(dimAlpha) {
        { letter: String -> if (letter == opaqueLetter.value) 1f else dimAlpha.value }
    }
}

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
