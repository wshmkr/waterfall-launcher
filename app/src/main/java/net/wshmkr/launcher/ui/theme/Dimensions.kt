package net.wshmkr.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Spacing steps that don't scale with screen width; use instead of raw dp for padding and gaps.
object Spacing {
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val xLarge = 32.dp
}

data class Dimensions(
    val gutterSmall: Dp,
    val gutterLarge: Dp,
    val pagePadding: Dp,
    val iconGap: Dp,

    val iconSmall: Dp,
    val iconMedium: Dp,
    val iconLarge: Dp,
    val playButtonSize: Dp,
    val albumArtSize: Dp,
    val searchButtonEndInset: Dp,

    val fontCaption: TextUnit,
    val fontSmall: TextUnit,
    val fontMedium: TextUnit,
    val fontLarge: TextUnit,
    val fontXLarge: TextUnit,
    val fontTitle: TextUnit,
    val fontClock: TextUnit,
)

val standardDimensions = Dimensions(
    gutterSmall = 16.dp,
    gutterLarge = 32.dp,
    pagePadding = 24.dp,
    iconGap = 20.dp,

    iconSmall = 24.dp,
    iconMedium = 36.dp,
    iconLarge = 40.dp,
    playButtonSize = 56.dp,
    albumArtSize = 96.dp,
    searchButtonEndInset = 64.dp,

    fontCaption = 12.sp,
    fontSmall = 14.sp,
    fontMedium = 16.sp,
    fontLarge = 18.sp,
    fontXLarge = 20.sp,
    fontTitle = 24.sp,
    fontClock = 48.sp,
)

val compactDimensions = Dimensions(
    gutterSmall = 12.dp,
    gutterLarge = 24.dp,
    pagePadding = 20.dp,
    iconGap = 16.dp,

    iconSmall = 22.dp,
    iconMedium = 32.dp,
    iconLarge = 36.dp,
    playButtonSize = 48.dp,
    albumArtSize = 80.dp,
    searchButtonEndInset = 56.dp,

    fontCaption = 11.sp,
    fontSmall = 13.sp,
    fontMedium = 15.sp,
    fontLarge = 17.sp,
    fontXLarge = 18.sp,
    fontTitle = 22.sp,
    fontClock = 40.sp,
)

val largeDimensions = Dimensions(
    gutterSmall = 20.dp,
    gutterLarge = 36.dp,
    pagePadding = 24.dp,
    iconGap = 24.dp,

    iconSmall = 26.dp,
    iconMedium = 40.dp,
    iconLarge = 44.dp,
    playButtonSize = 64.dp,
    albumArtSize = 112.dp,
    searchButtonEndInset = 72.dp,

    fontCaption = 13.sp,
    fontSmall = 15.sp,
    fontMedium = 17.sp,
    fontLarge = 19.sp,
    fontXLarge = 22.sp,
    fontTitle = 26.sp,
    fontClock = 56.sp,
)

val LocalDimensions = staticCompositionLocalOf { standardDimensions }

fun dimensionsFor(screenWidthDp: Int): Dimensions = when {
    screenWidthDp < COMPACT_WIDTH_THRESHOLD_DP -> compactDimensions
    screenWidthDp < LARGE_WIDTH_THRESHOLD_DP -> standardDimensions
    else -> largeDimensions
}

private const val COMPACT_WIDTH_THRESHOLD_DP = 380
private const val LARGE_WIDTH_THRESHOLD_DP = 430

@Composable
fun alphabetBottomLift(): Dp =
    LocalConfiguration.current.screenHeightDp.dp * ALPHABET_BOTTOM_LIFT_FRACTION

@Composable
fun searchButtonBottomInset(): Dp =
    LocalConfiguration.current.screenHeightDp.dp * SEARCH_BUTTON_BOTTOM_FRACTION

@Composable
fun homeWidgetGap(): Dp =
    LocalConfiguration.current.screenHeightDp.dp * HOME_WIDGET_GAP_FRACTION

private const val ALPHABET_BOTTOM_LIFT_FRACTION = 0.11f
private const val SEARCH_BUTTON_BOTTOM_FRACTION = 0.055f
private const val HOME_WIDGET_GAP_FRACTION = 0.018f
