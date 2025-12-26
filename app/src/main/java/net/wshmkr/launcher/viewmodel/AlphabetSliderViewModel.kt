package net.wshmkr.launcher.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.math.abs
import kotlin.math.exp

class AlphabetSliderViewModel : ViewModel() {

    var touchYPosition by mutableStateOf<Float?>(null)
        private set

    var isInitialTouch by mutableStateOf(false)
        private set

    var activeLetter by mutableStateOf<String?>(null)
        private set

    private val letterBounds = mutableStateMapOf<Int, Float>()
    private var letters: List<String> = emptyList()

    companion object {
        private const val WAVE_AMPLITUDE = 75f
        private const val WAVE_WIDTH = 100f
    }

    fun setLetters(lettersList: List<String>) {
        letters = lettersList
        letterBounds.clear()
        activeLetter = null
    }

    fun updateTouchPosition(y: Float?, isInitialTouch: Boolean = false) {
        touchYPosition = y
        if (y != null) {
            this.isInitialTouch = isInitialTouch
            updateActiveLetter(y)
        } else {
            activeLetter = null
            this.isInitialTouch = false
        }
    }

    fun updateLetterBounds(index: Int, top: Float) {
        letterBounds[index] = top
    }

    private fun updateActiveLetter(touchY: Float) {
        if (letterBounds.isEmpty() || letters.isEmpty()) {
            activeLetter = letters.firstOrNull()
            return
        }

        val index = letterBounds.entries
            .filter { it.value <= touchY }
            .maxByOrNull { it.value }
            ?.key
            ?: 0

        if (index < letters.size) {
            activeLetter = letters[index]
        }
    }

    fun calculateWaveOffset(letterY: Float, touchY: Float?, density: Float): Float {
        if (touchY == null) return 0f

        val distance = abs(letterY - touchY)
        val distanceDp = distance / density

        val offset = WAVE_AMPLITUDE * exp(-(distanceDp * distanceDp) / (WAVE_WIDTH * WAVE_WIDTH))

        return offset * -1
    }
}
