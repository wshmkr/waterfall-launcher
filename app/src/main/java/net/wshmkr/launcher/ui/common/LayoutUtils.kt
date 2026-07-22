package net.wshmkr.launcher.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

@Composable
fun calculateCenteredContentTopPadding(): Dp {
    val density = LocalDensity.current
    val heightPx = LocalWindowInfo.current.containerSize.height
    val screenHeight = with(density) { heightPx.toDp() }
    return screenHeight * 0.25f
}
