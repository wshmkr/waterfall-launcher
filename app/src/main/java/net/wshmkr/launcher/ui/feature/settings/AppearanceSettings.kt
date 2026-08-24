package net.wshmkr.launcher.ui.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.wshmkr.launcher.model.PaletteStyle
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.ui.common.components.SegmentedMenuOption
import net.wshmkr.launcher.viewmodel.SettingsViewModel

@Composable
fun AppearanceSettings(viewModel: SettingsViewModel) {
    SettingsSectionHeader("Appearance")
    PaletteStyleRow(viewModel)
    IconPackRow(viewModel)
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

@Composable
private fun IconPackRow(viewModel: SettingsViewModel) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val selectedLabel by viewModel.iconPackLabel.collectAsStateWithLifecycle()

    MenuOption(
        text = "Icon pack",
        subtext = selectedLabel ?: "System default",
        onClick = { showPicker = true },
    )
    if (showPicker) {
        LaunchedEffect(Unit) { viewModel.loadIconPacks() }
        val selected by viewModel.iconPack.collectAsStateWithLifecycle()
        val packs by viewModel.availableIconPacks.collectAsStateWithLifecycle()
        IconPackPickerDialog(
            packs = packs,
            selectedPackage = selected,
            onSelect = { packageName ->
                viewModel.setIconPack(packageName)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

private fun paletteStyleLabel(style: PaletteStyle): String = when (style) {
    PaletteStyle.VIBRANT -> "Vibrant"
    PaletteStyle.EXPRESSIVE -> "Expressive"
    PaletteStyle.NEUTRAL -> "Neutral"
}
