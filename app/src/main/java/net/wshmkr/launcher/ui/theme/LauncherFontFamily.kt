package net.wshmkr.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import net.wshmkr.launcher.R
import net.wshmkr.launcher.model.LauncherFont
import java.io.File
import android.graphics.fonts.Font as PlatformFont

@Composable
fun rememberFontFamily(font: LauncherFont): FontFamily = remember(font) {
    when (font) {
        LauncherFont.Bundled -> bundledFontFamily
        is LauncherFont.UserFile -> userFontFamily(font.path) ?: bundledFontFamily
    }
}

private val launcherWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
)

// Geom is variable, so one file covers every weight.
private val bundledFontFamily = FontFamily(launcherWeights.map(::geom))

@OptIn(ExperimentalTextApi::class)
private fun geom(weight: FontWeight) = Font(
    resId = R.font.geom,
    weight = weight,
    variationSettings = weightSettings(weight),
)

// hasWeightAxis reads the file up front, so a bad one fails here rather than during text layout.
private fun userFontFamily(path: String): FontFamily? =
    runCatching { FontFamily(userFonts(File(path))) }.getOrNull()

// Declaring weights a static file cannot render would make FontSynthesis stop faking the bold.
private fun userFonts(file: File): List<Font> =
    if (file.hasWeightAxis()) {
        launcherWeights.map { Font(file, it, variationSettings = weightSettings(it)) }
    } else {
        listOf(Font(file))
    }

private fun File.hasWeightAxis(): Boolean {
    val font = PlatformFont.Builder(this).build()
    return (0 until font.axisCount).any { font.getAxis(it).tag == WEIGHT_AXIS_TAG }
}

private fun weightSettings(weight: FontWeight) =
    FontVariation.Settings(FontVariation.weight(weight.weight))

private const val WEIGHT_AXIS_TAG = "wght"
