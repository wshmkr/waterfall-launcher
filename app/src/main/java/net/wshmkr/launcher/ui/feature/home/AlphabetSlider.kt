package net.wshmkr.launcher.ui.feature.home

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.wshmkr.launcher.viewmodel.AlphabetSliderViewModel

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

    LaunchedEffect(letters) {
        viewModel.setLetters(letters)
    }

    val touchYPosition = viewModel.touchYPosition
    val isInitialTouch = viewModel.isInitialTouch
    val viewModelActiveLetter = viewModel.activeLetter

    LaunchedEffect(viewModelActiveLetter) {
        if (viewModelActiveLetter != null) {
            onLetterSelected(viewModelActiveLetter)
        } else {
            onSelectionCleared()
        }
    }

    fun Modifier.touchHandler(): Modifier = this.pointerInteropFilter { event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                viewModel.updateTouchPosition(event.y, isInitialTouch = true)
                true
            }
            MotionEvent.ACTION_MOVE -> {
                viewModel.updateTouchPosition(event.y, isInitialTouch = false)
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                viewModel.updateTouchPosition(null)
                true
            }
            else -> false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 96.dp)
                .alpha(0f)
                .touchHandler()
        ) {
            LettersList(
                letters = letters,
                activeLetter = viewModelActiveLetter,
                touchYPosition = touchYPosition,
                isInitialTouch = isInitialTouch,
                viewModel = viewModel
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .padding(bottom = 96.dp)
                .touchHandler()
        ) {
            LettersList(
                letters = letters,
                activeLetter = viewModelActiveLetter,
                touchYPosition = touchYPosition,
                isInitialTouch = isInitialTouch,
                viewModel = viewModel,
                onLetterPositioned = { index, top ->
                    viewModel.updateLetterBounds(index, top)
                }
            )
        }
    }
}

@Composable
private fun LettersList(
    letters: List<String>,
    activeLetter: String?,
    touchYPosition: Float?,
    isInitialTouch: Boolean,
    viewModel: AlphabetSliderViewModel,
    onLetterPositioned: (index: Int, top: Float) -> Unit = { _, _ -> }
) {
    val density = LocalDensity.current

    Column(
        modifier = Modifier.width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-6).dp),
    ) {
        letters.forEachIndexed { index, letter ->
            val letterY = remember { mutableStateOf<Float?>(null) }

            val offset = if (touchYPosition != null && letterY.value != null) {
                viewModel.calculateWaveOffset(letterY.value!!, touchYPosition, density.density)
            } else {
                0f
            }

            val animatedOffset by animateFloatAsState(
                targetValue = offset,
                animationSpec = when {
                    touchYPosition == null || isInitialTouch -> {
                        tween(durationMillis = 250)
                    }
                    else -> {
                        snap()
                    }
                },
                label = "waveOffset"
            )

            Text(
                text = letter,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = animatedOffset.dp)
                    .onGloballyPositioned { coordinates ->
                        val top = coordinates.boundsInParent().top
                        letterY.value = top
                        onLetterPositioned(index, top)
                    },
                fontSize = 16.sp,
                color = if (letter == activeLetter) Color.Red else Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}
