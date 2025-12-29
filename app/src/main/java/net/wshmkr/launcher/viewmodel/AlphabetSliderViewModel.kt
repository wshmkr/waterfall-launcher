package net.wshmkr.launcher.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class AlphabetSliderViewModel : ViewModel() {

    var touchYPosition by mutableStateOf<Float?>(null)
        private set

    var isInitialTouch by mutableStateOf(false)
        private set

    var activeLetter by mutableStateOf<String?>(null)
        private set

    var sliderVerticalOffset by mutableStateOf(0f)
        private set

    var horizontalDelta by mutableStateOf(0f)
        private set

    private var touchXStart: Float? = null

    private data class LetterBounds(val top: Float, val bottom: Float)

    private val letterBounds = mutableStateMapOf<Int, LetterBounds>()
    private var letters: List<String> = emptyList()

    companion object {
        private const val WAVE_WIDTH = 100f
        private const val BASE_AMPLITUDE_DP = 75f
    }

    fun setLetters(lettersList: List<String>) {
        letters = lettersList
        letterBounds.clear()
        activeLetter = null
    }

    fun updateTouchPosition(y: Float?, x: Float? = null, isInitialTouch: Boolean = false) {
        touchYPosition = y
        if (y != null) {
            if (isInitialTouch) {
                touchXStart = x
                horizontalDelta = 0f
            }

            if (x != null && touchXStart != null) {
                horizontalDelta = abs(x - touchXStart!!)
            }

            this.isInitialTouch = isInitialTouch
            updateActiveLetter(y)
            updateSliderOffset(y)
        } else {
            activeLetter = null
            this.isInitialTouch = false
            sliderVerticalOffset = 0f
            touchXStart = null
            horizontalDelta = 0f
        }
    }

    private fun updateSliderOffset(touchY: Float) {
        // Letter bounds are stored as base positions (without offset)
        val firstLetterTopBase = getFirstLetterTop()
        val lastLetterBottomBase = getLastLetterBottom()

        if (firstLetterTopBase == null || lastLetterBottomBase == null) {
            sliderVerticalOffset = 0f
            return
        }

        // Calculate current positions of letters (base + current offset)
        val firstLetterTopCurrent = firstLetterTopBase + sliderVerticalOffset
        val lastLetterBottomCurrent = lastLetterBottomBase + sliderVerticalOffset

        when {
            touchY < firstLetterTopCurrent -> {
                // Touch is above first letter's current position - move slider up
                sliderVerticalOffset = touchY - firstLetterTopBase
            }
            touchY > lastLetterBottomCurrent -> {
                // Touch is below last letter's current position - move slider down, using bottom as the boundary
                sliderVerticalOffset = touchY - lastLetterBottomBase
            }
            else -> {
                // Touch is within current bounds; keep the existing offset so the slider
                // doesn't snap back when the finger re-enters the original bounds.
            }
        }
    }

    fun updateLetterBounds(index: Int, top: Float, bottom: Float) {
        letterBounds[index] = LetterBounds(top = top, bottom = bottom)
    }

    private fun getFirstLetterTop(): Float? {
        return letterBounds[0]?.top
    }

    private fun getLastLetterBottom(): Float? {
        if (letters.isEmpty()) return null
        val lastIndex = letters.size - 1
        return letterBounds[lastIndex]?.bottom
    }

    private fun updateActiveLetter(touchY: Float) {
        if (letterBounds.isEmpty() || letters.isEmpty()) {
            activeLetter = letters.firstOrNull()
            return
        }

        // Letter bounds are stored as base positions, so add current offset when comparing
        val index = letterBounds.entries
            .map { it.key to (it.value.top + sliderVerticalOffset) }
            .filter { it.second <= touchY }
            .maxByOrNull { it.second }
            ?.first
            ?: 0

        if (index < letters.size) {
            activeLetter = letters[index]
        }
    }

    fun calculateWaveOffset(
        letterY: Float,
        touchY: Float?,
        density: Float,
        horizontalDelta: Float,
        screenWidthDp: Int
    ): Float {
        if (touchY == null) return 0f

        // letterY is the current position from boundsInParent, which includes the slider offset
        // touchY is relative to the Box, so both are in the same coordinate system
        val distance = abs(letterY - touchY)
        val distanceDp = distance / density

        val horizontalDeltaDp = abs(horizontalDelta) / density
        val maxAmplitudeDp = max(screenWidthDp * 0.9f, BASE_AMPLITUDE_DP)
        val amplitude = min(horizontalDeltaDp + BASE_AMPLITUDE_DP, maxAmplitudeDp)

        val waveWidth = WAVE_WIDTH + (horizontalDeltaDp * 0.3f)
        val offset = amplitude * exp(-(distanceDp * distanceDp) / (waveWidth * waveWidth))

        return offset * -1
    }
}
