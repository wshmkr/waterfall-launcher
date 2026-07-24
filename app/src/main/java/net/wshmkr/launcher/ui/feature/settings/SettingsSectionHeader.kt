package net.wshmkr.launcher.ui.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.wshmkr.launcher.ui.theme.LocalDimensions

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = LocalDimensions.current.fontXLarge,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))
}
