package net.wshmkr.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Dimensions(
    val listHorizontalGutter: Dp,
    val searchListHorizontalGutter: Dp,
    val homeOptionsMenuHorizontalPadding: Dp,

    val appRowOuterStartPadding: Dp,
    val appRowOuterEndPadding: Dp,
    val appRowInnerPadding: Dp,
    val appRowIconSize: Dp,
    val appRowIconGap: Dp,
    val appRowFont: TextUnit,
    val notificationPreviewFont: TextUnit,

    val sectionHeaderFont: TextUnit,
    val sectionHeaderStartPadding: Dp,
    val sectionHeaderEndPadding: Dp,
    val sectionHeaderTopPadding: Dp,
    val sectionHeaderBottomPadding: Dp,

    val clockFont: TextUnit,
    val clockChipFont: TextUnit,

    val weatherFont: TextUnit,

    val mediaPromptIconSize: Dp,
    val mediaPromptFont: TextUnit,
    val mediaAlbumArtSize: Dp,
    val mediaAlbumFallbackIconSize: Dp,
    val mediaTitleFont: TextUnit,
    val mediaArtistFont: TextUnit,
    val mediaArtTextGap: Dp,
    val mediaSideButtonSize: Dp,
    val mediaSideGlyphSize: Dp,
    val mediaPlayButtonSize: Dp,
    val mediaPlayGlyphSize: Dp,

    val alphabetColumnWidth: Dp,
    val alphabetLetterFont: TextUnit,
    val alphabetBottomLiftFraction: Float,

    val allAppsFabBottomFraction: Float,
    val allAppsFabEndOffset: Dp,

    val homeWidgetGapFraction: Float,

    val menuOptionIconSize: Dp,
    val menuOptionIconGap: Dp,
    val menuOptionHorizontalPadding: Dp,
    val menuOptionFontSmall: TextUnit,
    val menuOptionFontMedium: TextUnit,
    val menuOptionFontLarge: TextUnit,
    val menuOptionSubFontSmall: TextUnit,
    val menuOptionSubFontMedium: TextUnit,
    val menuOptionSubFontLarge: TextUnit,

    val appOptionsHorizontalPadding: Dp,
    val appOptionsIconSize: Dp,
    val appOptionsIconGap: Dp,
    val appOptionsLabelFont: TextUnit,
    val appOptionsNoteFont: TextUnit,

    val settingsPageHorizontalPadding: Dp,
    val settingsSectionGap: Dp,
    val settingsSectionHeaderFont: TextUnit,

    val manageWidgetsPagePadding: Dp,
    val manageWidgetsTitleFont: TextUnit,
    val manageWidgetsTitleGap: Dp,
    val managedWidgetIconSize: Dp,
    val managedWidgetNameFont: TextUnit,
    val managedWidgetSubtitleFont: TextUnit,
    val addWidgetIconSize: Dp,
    val addWidgetLabelFont: TextUnit,

    val widgetProviderIndent: Dp,
    val widgetProviderEndPadding: Dp,
    val widgetProviderIconSize: Dp,
    val widgetProviderLabelFont: TextUnit,
    val widgetProviderSubtitleFont: TextUnit,
    val widgetProviderChevronSize: Dp,

    val widgetListItemFallbackHeight: Dp,
    val widgetListItemPreviewFont: TextUnit,
    val widgetListItemLabelFont: TextUnit,

    val widgetAppListEmptyFont: TextUnit,

    val verticalSwipeThreshold: Dp,
    val verticalDragFeedbackScale: Float,

    val centeredContentTopFraction: Float,
)

val standardDimensions = Dimensions(
    listHorizontalGutter = 32.dp,
    searchListHorizontalGutter = 16.dp,
    homeOptionsMenuHorizontalPadding = 16.dp,

    appRowOuterStartPadding = 8.dp,
    appRowOuterEndPadding = 32.dp,
    appRowInnerPadding = 8.dp,
    appRowIconSize = 40.dp,
    appRowIconGap = 20.dp,
    appRowFont = 16.sp,
    notificationPreviewFont = 12.sp,

    sectionHeaderFont = 20.sp,
    sectionHeaderStartPadding = 16.dp,
    sectionHeaderEndPadding = 16.dp,
    sectionHeaderTopPadding = 12.dp,
    sectionHeaderBottomPadding = 8.dp,

    clockFont = 48.sp,
    clockChipFont = 16.sp,

    weatherFont = 16.sp,

    mediaPromptIconSize = 32.dp,
    mediaPromptFont = 14.sp,
    mediaAlbumArtSize = 96.dp,
    mediaAlbumFallbackIconSize = 40.dp,
    mediaTitleFont = 16.sp,
    mediaArtistFont = 14.sp,
    mediaArtTextGap = 16.dp,
    mediaSideButtonSize = 36.dp,
    mediaSideGlyphSize = 24.dp,
    mediaPlayButtonSize = 56.dp,
    mediaPlayGlyphSize = 40.dp,

    alphabetColumnWidth = 40.dp,
    alphabetLetterFont = 16.sp,
    alphabetBottomLiftFraction = 0.11f,

    allAppsFabBottomFraction = 0.055f,
    allAppsFabEndOffset = 64.dp,

    homeWidgetGapFraction = 0.018f,

    menuOptionIconSize = 24.dp,
    menuOptionIconGap = 24.dp,
    menuOptionHorizontalPadding = 16.dp,
    menuOptionFontSmall = 16.sp,
    menuOptionFontMedium = 18.sp,
    menuOptionFontLarge = 20.sp,
    menuOptionSubFontSmall = 14.sp,
    menuOptionSubFontMedium = 16.sp,
    menuOptionSubFontLarge = 18.sp,

    appOptionsHorizontalPadding = 16.dp,
    appOptionsIconSize = 40.dp,
    appOptionsIconGap = 16.dp,
    appOptionsLabelFont = 20.sp,
    appOptionsNoteFont = 14.sp,

    settingsPageHorizontalPadding = 24.dp,
    settingsSectionGap = 32.dp,
    settingsSectionHeaderFont = 20.sp,

    manageWidgetsPagePadding = 24.dp,
    manageWidgetsTitleFont = 24.sp,
    manageWidgetsTitleGap = 16.dp,
    managedWidgetIconSize = 36.dp,
    managedWidgetNameFont = 16.sp,
    managedWidgetSubtitleFont = 13.sp,
    addWidgetIconSize = 24.dp,
    addWidgetLabelFont = 16.sp,

    widgetProviderIndent = 24.dp,
    widgetProviderEndPadding = 32.dp,
    widgetProviderIconSize = 40.dp,
    widgetProviderLabelFont = 18.sp,
    widgetProviderSubtitleFont = 12.sp,
    widgetProviderChevronSize = 24.dp,

    widgetListItemFallbackHeight = 120.dp,
    widgetListItemPreviewFont = 12.sp,
    widgetListItemLabelFont = 16.sp,

    widgetAppListEmptyFont = 18.sp,

    verticalSwipeThreshold = 100.dp,
    verticalDragFeedbackScale = 5f,

    centeredContentTopFraction = 0.25f,
)

val compactDimensions = standardDimensions.copy(
    listHorizontalGutter = 24.dp,
    searchListHorizontalGutter = 12.dp,

    appRowOuterEndPadding = 24.dp,
    appRowIconSize = 36.dp,
    appRowIconGap = 16.dp,
    appRowFont = 15.sp,
    notificationPreviewFont = 11.sp,

    sectionHeaderFont = 18.sp,

    clockFont = 40.sp,
    clockChipFont = 14.sp,

    weatherFont = 14.sp,

    mediaPromptIconSize = 28.dp,
    mediaAlbumArtSize = 80.dp,
    mediaAlbumFallbackIconSize = 32.dp,
    mediaTitleFont = 15.sp,
    mediaArtistFont = 13.sp,
    mediaArtTextGap = 12.dp,
    mediaSideButtonSize = 32.dp,
    mediaSideGlyphSize = 22.dp,
    mediaPlayButtonSize = 48.dp,
    mediaPlayGlyphSize = 32.dp,

    alphabetColumnWidth = 36.dp,

    allAppsFabEndOffset = 56.dp,

    menuOptionIconSize = 22.dp,
    menuOptionIconGap = 20.dp,
    menuOptionFontSmall = 15.sp,
    menuOptionFontMedium = 17.sp,
    menuOptionFontLarge = 19.sp,
    menuOptionSubFontSmall = 13.sp,
    menuOptionSubFontMedium = 15.sp,
    menuOptionSubFontLarge = 17.sp,

    appOptionsIconSize = 36.dp,
    appOptionsLabelFont = 18.sp,

    settingsPageHorizontalPadding = 20.dp,

    manageWidgetsPagePadding = 20.dp,
    manageWidgetsTitleFont = 22.sp,
    managedWidgetIconSize = 32.dp,
    managedWidgetNameFont = 15.sp,

    widgetProviderIconSize = 36.dp,
    widgetProviderLabelFont = 17.sp,
)

val largeDimensions = standardDimensions.copy(
    listHorizontalGutter = 36.dp,
    searchListHorizontalGutter = 20.dp,

    appRowIconSize = 44.dp,
    appRowIconGap = 24.dp,
    appRowFont = 17.sp,
    notificationPreviewFont = 13.sp,

    sectionHeaderFont = 22.sp,

    clockFont = 56.sp,
    clockChipFont = 18.sp,

    weatherFont = 18.sp,

    mediaAlbumArtSize = 112.dp,
    mediaAlbumFallbackIconSize = 44.dp,
    mediaTitleFont = 17.sp,
    mediaArtistFont = 15.sp,
    mediaSideButtonSize = 40.dp,
    mediaSideGlyphSize = 26.dp,
    mediaPlayButtonSize = 64.dp,
    mediaPlayGlyphSize = 44.dp,

    alphabetColumnWidth = 44.dp,
    alphabetLetterFont = 17.sp,

    allAppsFabEndOffset = 72.dp,

    menuOptionIconSize = 26.dp,
    appOptionsIconSize = 44.dp,
    appOptionsLabelFont = 22.sp,

    manageWidgetsTitleFont = 26.sp,
    managedWidgetIconSize = 40.dp,

    widgetProviderIconSize = 44.dp,
    widgetProviderLabelFont = 19.sp,
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
fun Dimensions.alphabetBottomLift(): Dp =
    LocalConfiguration.current.screenHeightDp.dp * alphabetBottomLiftFraction

@Composable
fun Dimensions.allAppsFabBottom(): Dp =
    LocalConfiguration.current.screenHeightDp.dp * allAppsFabBottomFraction

@Composable
fun Dimensions.homeWidgetGap(): Dp =
    LocalConfiguration.current.screenHeightDp.dp * homeWidgetGapFraction

@Composable
fun Dimensions.centeredContentTopPadding(): Dp =
    LocalConfiguration.current.screenHeightDp.dp * centeredContentTopFraction
