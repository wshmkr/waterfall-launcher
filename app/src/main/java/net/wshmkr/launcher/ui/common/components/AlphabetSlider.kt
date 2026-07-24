package net.wshmkr.launcher.ui.common.components

import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.wshmkr.launcher.ui.theme.LocalDimensions
import net.wshmkr.launcher.ui.theme.alphabetBottomLift
import net.wshmkr.launcher.ui.theme.alphabetHeight
import net.wshmkr.launcher.viewmodel.AlphabetSliderViewModel
import kotlin.math.roundToInt

const val STAR_SYMBOL = "★"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AlphabetSlider(
    letters: List<String>,
    onLetterSelected: (String) -> Unit,
    onSelectionCleared: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlphabetSliderViewModel = viewModel(),
) {
    if (letters.isEmpty()) return

    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    LaunchedEffect(letters) {
        viewModel.setLetters(letters)
    }

    val viewModelActiveLetter = viewModel.activeLetter

    LaunchedEffect(viewModelActiveLetter) {
        if (viewModelActiveLetter != null) {
            onLetterSelected(viewModelActiveLetter)
        } else {
            onSelectionCleared()
        }
    }

    val touchHandler = remember(viewModel, density, screenWidthDp) {
        { trackHorizontalMovement: Boolean ->
            Modifier.pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        viewModel.updateTouchPosition(
                            y = event.y,
                            x = if (trackHorizontalMovement) event.x else null,
                            isInitialTouch = true,
                            density = density.density,
                            screenWidthDp = screenWidthDp
                        )
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        viewModel.updateTouchPosition(
                            y = event.y,
                            x = if (trackHorizontalMovement) event.x else null,
                            isInitialTouch = false,
                            density = density.density,
                            screenWidthDp = screenWidthDp
                        )
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        viewModel.updateTouchPosition(null)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    val bottomLift = alphabetBottomLift()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Invisible mirror of the letter column gives the left touch region a real height on
        // first frame, so initial left-hand touches aren't dropped.
        Box(
            modifier = Modifier
                .padding(bottom = bottomLift)
                .then(touchHandler(false))
        ) {
            InvisibleLettersColumn(letters = letters)
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .padding(bottom = bottomLift)
                .then(touchHandler(true))
        ) {
            AnimatedLettersList(
                letters = letters,
                activeLetter = viewModelActiveLetter,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun InvisibleLettersColumn(letters: List<String>) {
    val dimensions = LocalDimensions.current
    Column(
        modifier = Modifier
            .height(alphabetHeight(letters.size))
            .width(dimensions.alphabetColumnWidth)
            .graphicsLayer { alpha = 0f },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    fontSize = dimensions.fontMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AnimatedLettersList(
    letters: List<String>,
    activeLetter: String?,
    viewModel: AlphabetSliderViewModel,
) {
    val sliderVerticalOffsetPx = viewModel.sliderVerticalOffset
    // Derived so the column recomposes only when the drag starts/stops, not on every touch-move.
    val isReleasing by remember { derivedStateOf { viewModel.touchYPosition == null } }

    val animatedVerticalOffset by animateFloatAsState(
        targetValue = sliderVerticalOffsetPx,
        animationSpec = if (isReleasing) tween(durationMillis = 250) else snap(),
        label = "sliderVerticalOffset"
    )

    val dimensions = LocalDimensions.current

    Column(
        modifier = Modifier
            .height(alphabetHeight(letters.size))
            .width(dimensions.alphabetColumnWidth)
            .offset { IntOffset(0, animatedVerticalOffset.roundToInt()) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEachIndexed { index, letter ->
            // Per-letter Animatable driven off snapshotFlow so the wave settles smoothly on
            // release without recomposing the Text — the value read stays inside graphicsLayer.
            val waveAnimatable = remember { Animatable(0f) }
            LaunchedEffect(index, viewModel) {
                snapshotFlow { viewModel.waveOffsetAt(index) to (viewModel.touchYPosition == null) }
                    .collect { (target, releasing) ->
                        if (releasing) {
                            waveAnimatable.animateTo(target, tween(durationMillis = 250))
                        } else {
                            waveAnimatable.snapTo(target)
                        }
                    }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInParent()
                        viewModel.updateLetterBounds(index, bounds.top, bounds.bottom)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    modifier = Modifier
                        .graphicsLayer { translationX = waveAnimatable.value.dp.toPx() },
                    fontSize = dimensions.fontMedium,
                    color = if (letter == activeLetter) Color.Red else Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
