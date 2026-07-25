package net.wshmkr.launcher.ui.common.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.wshmkr.launcher.ui.theme.accentButtonColors

// Material fills a Button with primary; this is the app's accent fill.
@Composable
fun AccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = accentButtonColors(),
        content = content,
    )
}
