package net.wshmkr.launcher.ui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.wshmkr.launcher.model.PaletteStyle
import net.wshmkr.launcher.model.ThemeMode
import net.wshmkr.launcher.ui.common.components.SegmentedMenuOption
import net.wshmkr.launcher.viewmodel.SettingsViewModel

@Composable
fun AppearanceSettings(viewModel: SettingsViewModel) {
    SettingsSectionHeader("Appearance")
    ThemeModeRow(viewModel)
    PaletteStyleRow(viewModel)
}

@Composable
private fun ThemeModeRow(viewModel: SettingsViewModel) {
    val mode by viewModel.themeMode.collectAsStateWithLifecycle()
    SegmentedMenuOption(
        text = "Theme",
        options = ThemeMode.entries,
        selected = mode,
        onSelect = viewModel::setThemeMode,
        optionLabel = ::themeModeLabel,
    )
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

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.AUTO -> "Auto"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun paletteStyleLabel(style: PaletteStyle): String = when (style) {
    PaletteStyle.FIDELITY -> "Fidelity"
    PaletteStyle.EXPRESSIVE -> "Expressive"
    PaletteStyle.NEUTRAL -> "Neutral"
}
