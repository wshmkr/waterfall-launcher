package net.wshmkr.launcher.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun calculateCenteredContentTopPadding(): Dp {
    val density = LocalDensity.current
    val heightPx = LocalWindowInfo.current.containerSize.height
    // containerSize is IntSize.Zero until first measurement; fall back to Configuration so the
    // first frame isn't pinned to the top.
    val screenHeight = if (heightPx > 0) {
        with(density) { heightPx.toDp() }
    } else {
        LocalConfiguration.current.screenHeightDp.dp
    }
    return screenHeight * 0.25f
}
