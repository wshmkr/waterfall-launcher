package net.wshmkr.launcher.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun calculateCenteredContentTopPadding(): Dp {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    return screenHeight * 0.25f
}

