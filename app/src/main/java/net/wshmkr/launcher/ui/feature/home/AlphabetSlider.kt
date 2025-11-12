package net.wshmkr.launcher.ui.feature.home

import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import net.wshmkr.launcher.viewmodel.HomeViewModel
import kotlin.collections.set

const val STAR_SYMBOL = "★"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AlphabetSlider(
    navController: NavController,
    viewModel: HomeViewModel,
) {
    val letterBounds = remember { mutableStateMapOf<Int, Float>() }

    fun letterAtPosition(position: Float) : String {
        if (letterBounds.isEmpty()) return viewModel.alphabetLetters.first()
        
        val index = letterBounds.entries
            .filter { it.value <= position }
            .maxByOrNull { it.value }
            ?.key
            ?: 0

        return viewModel.alphabetLetters[index]
    }

    fun Modifier.touchHandler(): Modifier = this.pointerInteropFilter { event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                viewModel.scrollToLetter(letterAtPosition(event.y))
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                viewModel.deselectLetter()
                true
            }
            else -> false
        }
    }

    Row(
        modifier = Modifier
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
                letters = viewModel.alphabetLetters,
                activeLetter = viewModel.activeLetter
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .padding(bottom = 96.dp)
                .touchHandler()
        ) {
            LettersList(
                letters = viewModel.alphabetLetters,
                activeLetter = viewModel.activeLetter,
                onLetterPositioned = { index, top ->
                    letterBounds[index] = top
                }
            )
        }
    }
}

@Composable
private fun LettersList(
    letters: List<String>,
    activeLetter: String?,
    onLetterPositioned: (index: Int, top: Float) -> Unit = { _, _ -> }
) {
    LazyColumn(
        modifier = Modifier.width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-6).dp),
        userScrollEnabled = false
    ) {
        itemsIndexed(letters) { index, letter ->
            Text(
                text = letter,
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        onLetterPositioned(index, coordinates.boundsInParent().top)
                    },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (letter == activeLetter) Color.Red else Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}
