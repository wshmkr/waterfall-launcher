package net.wshmkr.launcher.ui.common.components

import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    // Track RIGHT column height so LEFT touch spacer matches it for consistent y-coordinate mapping.
    var letterListHeightPx by remember { mutableIntStateOf(0) }
    val letterListHeightDp = with(density) { letterListHeightPx.toDp() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Spacer(
            modifier = Modifier
                .padding(bottom = 96.dp)
                .width(40.dp)
                .height(letterListHeightDp)
                .then(touchHandler(false))
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .padding(bottom = 96.dp)
                .onSizeChanged { letterListHeightPx = it.height }
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
private fun AnimatedLettersList(
    letters: List<String>,
    activeLetter: String?,
    viewModel: AlphabetSliderViewModel,
) {
    val sliderVerticalOffsetPx = viewModel.sliderVerticalOffset
    val isReleasing = viewModel.touchYPosition == null

    val animatedVerticalOffset by animateFloatAsState(
        targetValue = sliderVerticalOffsetPx,
        animationSpec = if (isReleasing) tween(durationMillis = 250) else snap(),
        label = "sliderVerticalOffset"
    )

    Column(
        modifier = Modifier
            .width(40.dp)
            .offset { IntOffset(0, animatedVerticalOffset.roundToInt()) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-6).dp),
    ) {
        letters.forEachIndexed { index, letter ->
            Text(
                text = letter,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = viewModel.waveOffsetAt(index).dp.toPx() }
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInParent()
                        viewModel.updateLetterBounds(index, bounds.top, bounds.bottom)
                    },
                fontSize = 16.sp,
                color = if (letter == activeLetter) Color.Red else Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}
