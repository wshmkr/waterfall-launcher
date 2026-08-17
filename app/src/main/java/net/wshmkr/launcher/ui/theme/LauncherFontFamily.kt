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
import java.io.RandomAccessFile

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

// No platform API lists a file's variation axes, so this reads the OpenType fvar table itself.
private fun File.hasWeightAxis(): Boolean = RandomAccessFile(this, "r").use { font ->
    font.seek(4)                // sfnt version
    val tableCount = font.readUnsignedShort()
    font.skipBytes(6)           // searchRange, entrySelector, rangeShift
    repeat(tableCount) {
        val tag = font.readInt()
        font.skipBytes(4)       // checksum
        val tableOffset = font.readInt()
        font.skipBytes(4)       // length
        if (tag == FVAR_TABLE_TAG) return font.fvarHasWeightAxis(tableOffset.toLong())
    }
    false
}

private fun RandomAccessFile.fvarHasWeightAxis(fvarOffset: Long): Boolean {
    seek(fvarOffset + 4)        // major and minor version
    val axesOffset = readUnsignedShort()
    skipBytes(2)                // reserved
    val axisCount = readUnsignedShort()
    val axisSize = readUnsignedShort()
    return (0 until axisCount).any { axis ->
        seek(fvarOffset + axesOffset + axis.toLong() * axisSize)
        readInt() == WEIGHT_AXIS_TAG
    }
}

private fun weightSettings(weight: FontWeight) =
    FontVariation.Settings(FontVariation.weight(weight.weight))

private fun openTypeTag(tag: String) = tag.fold(0) { packed, char -> (packed shl 8) or char.code }

private val FVAR_TABLE_TAG = openTypeTag("fvar")

private val WEIGHT_AXIS_TAG = openTypeTag("wght")
