package net.wshmkr.launcher.ui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.wshmkr.launcher.model.HomeTextColor
import net.wshmkr.launcher.model.ThemeMode
import net.wshmkr.launcher.ui.common.components.SegmentedMenuOption
import net.wshmkr.launcher.viewmodel.SettingsViewModel

@Composable
fun AppearanceSettings(viewModel: SettingsViewModel) {
    SettingsSectionHeader("Appearance")
    ThemeModeRow(viewModel)
    HomeTextColorRow(viewModel)
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
private fun HomeTextColorRow(viewModel: SettingsViewModel) {
    val homeTextColor by viewModel.homeTextColor.collectAsStateWithLifecycle()
    SegmentedMenuOption(
        text = "Home text",
        options = HomeTextColor.entries,
        selected = homeTextColor,
        onSelect = viewModel::setHomeTextColor,
        optionLabel = ::homeTextColorLabel,
    )
}

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun homeTextColorLabel(color: HomeTextColor): String = when (color) {
    HomeTextColor.AUTO -> "Auto"
    HomeTextColor.LIGHT -> "Light"
    HomeTextColor.DARK -> "Dark"
}
