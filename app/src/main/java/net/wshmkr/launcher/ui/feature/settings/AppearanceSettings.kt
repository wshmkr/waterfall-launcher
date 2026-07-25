package net.wshmkr.launcher.ui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.wshmkr.launcher.model.PaletteStyle
import net.wshmkr.launcher.ui.common.components.SegmentedMenuOption
import net.wshmkr.launcher.viewmodel.SettingsViewModel

@Composable
fun AppearanceSettings(viewModel: SettingsViewModel) {
    SettingsSectionHeader("Appearance")
    PaletteStyleRow(viewModel)
}

@Composable
private fun PaletteStyleRow(viewModel: SettingsViewModel) {
    val paletteStyle by viewModel.paletteStyle.collectAsStateWithLifecycle()
    SegmentedMenuOption(
        text = "Accent palette",
        options = PaletteStyle.entries,
        selected = paletteStyle,
        onSelect = viewModel::setPaletteStyle,
        optionLabel = ::paletteStyleLabel,
    )
}

private fun paletteStyleLabel(style: PaletteStyle): String = when (style) {
    PaletteStyle.VIBRANT -> "Vibrant"
    PaletteStyle.EXPRESSIVE -> "Expressive"
    PaletteStyle.NEUTRAL -> "Neutral"
}
