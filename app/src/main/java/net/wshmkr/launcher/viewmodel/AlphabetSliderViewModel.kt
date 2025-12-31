package net.wshmkr.launcher.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

    var sliderVerticalOffset by mutableFloatStateOf(0f)
        private set

    var horizontalDelta by mutableFloatStateOf(0f)
        private set

    var waveOffsets by mutableStateOf<Map<Int, Float>>(emptyMap())
        private set

    private var touchXStart: Float? = null

    private data class LetterBounds(val top: Float, val bottom: Float)

    private val letterBounds = mutableMapOf<Int, LetterBounds>()
    
    private var letters: List<String> = emptyList()

    companion object {
        private const val WAVE_WIDTH = 100f
        private const val BASE_AMPLITUDE_DP = 70f
    }

    fun setLetters(lettersList: List<String>) {
        letters = lettersList
        letterBounds.clear()
        waveOffsets = emptyMap()
        activeLetter = null
    }

    fun updateTouchPosition(
        y: Float?, 
        x: Float? = null, 
        isInitialTouch: Boolean = false,
        density: Float = 1f,
        screenWidthDp: Int = 0
    ) {
        touchYPosition = y
        if (y != null) {
            if (isInitialTouch) {
                touchXStart = x
                horizontalDelta = 0f
            }

            if (x != null && touchXStart != null) {
                val delta = touchXStart!! - x
                horizontalDelta = if (delta > 0f) delta else 0f
            }

            this.isInitialTouch = isInitialTouch
            updateActiveLetter(y)
            updateSliderOffset(y)
            computeWaveOffsets(y, density, screenWidthDp)
        } else {
            activeLetter = null
            this.isInitialTouch = false
            sliderVerticalOffset = 0f
            touchXStart = null
            horizontalDelta = 0f
            waveOffsets = emptyMap()
        }
    }

    private fun updateSliderOffset(touchY: Float) {
        val firstLetterTopBase = getFirstLetterTop()
        val lastLetterBottomBase = getLastLetterBottom()

        if (firstLetterTopBase == null || lastLetterBottomBase == null) {
            sliderVerticalOffset = 0f
            return
        }

        val firstLetterTop = firstLetterTopBase + sliderVerticalOffset
        val lastLetterBottom = lastLetterBottomBase + sliderVerticalOffset

        when {
            touchY < firstLetterTop -> {
                sliderVerticalOffset = touchY - firstLetterTopBase
            }
            touchY > lastLetterBottom -> {
                sliderVerticalOffset = touchY - lastLetterBottomBase
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

    private fun computeWaveOffsets(touchY: Float, density: Float, screenWidthDp: Int) {
        val offsets = mutableMapOf<Int, Float>()
        
        for ((index, bounds) in letterBounds) {
            val letterY = bounds.top + sliderVerticalOffset
            offsets[index] = calculateWaveOffsetInternal(
                letterY = letterY,
                touchY = touchY,
                density = density,
                horizontalDelta = horizontalDelta,
                screenWidthDp = screenWidthDp
            )
        }
        
        waveOffsets = offsets
    }

    private fun calculateWaveOffsetInternal(
        letterY: Float,
        touchY: Float,
        density: Float,
        horizontalDelta: Float,
        screenWidthDp: Int
    ): Float {
        val distance = abs(letterY - touchY)
        val distanceDp = distance / density

        val horizontalDeltaDp = horizontalDelta / density
        val maxAmplitudeDp = max(screenWidthDp * 0.9f, BASE_AMPLITUDE_DP)
        val amplitude = min(horizontalDeltaDp + BASE_AMPLITUDE_DP, maxAmplitudeDp)

        val waveWidth = WAVE_WIDTH + (horizontalDeltaDp * 0.4f)
        val offset = amplitude * exp(-(distanceDp * distanceDp) / (waveWidth * waveWidth))

        return offset * -1
    }

    fun getWaveOffset(index: Int): Float {
        return waveOffsets[index] ?: 0f
    }
}
