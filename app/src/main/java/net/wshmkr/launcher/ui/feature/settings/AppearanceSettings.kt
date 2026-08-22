package net.wshmkr.launcher.ui.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.wshmkr.launcher.model.LauncherFont
import net.wshmkr.launcher.model.PaletteStyle
import net.wshmkr.launcher.ui.common.components.MenuOption
import net.wshmkr.launcher.ui.common.components.SegmentedMenuOption
import net.wshmkr.launcher.ui.common.icons.CloseIcon
import net.wshmkr.launcher.viewmodel.SettingsViewModel
import java.io.File

@Composable
fun AppearanceSettings(viewModel: SettingsViewModel) {
    SettingsSectionHeader("Appearance")
    PaletteStyleRow(viewModel)
    FontRow(viewModel)
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
private fun FontRow(viewModel: SettingsViewModel) {
    val font by viewModel.launcherFont.collectAsStateWithLifecycle()
    val pickFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::setCustomFont)
    }
    val customFont = font as? LauncherFont.UserFile
    MenuOption(
        text = "Font",
        subtext = when {
            viewModel.customFontFailed -> "Couldn't read that file as a font"
            customFont != null -> File(customFont.path).name
            else -> "Geom (bundled)"
        },
        onClick = { pickFont.launch(FONT_MIME_TYPES) },
        endContent = customFont?.let {
            {
                IconButton(onClick = viewModel::clearCustomFont) {
                    Icon(CloseIcon(), contentDescription = "Restore bundled font")
                }
            }
        },
    )
}

// Pickers disagree on font MIME types, so octet-stream keeps oddly-typed files selectable.
private val FONT_MIME_TYPES = arrayOf(
    "font/ttf",
    "font/otf",
    "font/sfnt",
    "application/x-font-ttf",
    "application/octet-stream",
)

private fun paletteStyleLabel(style: PaletteStyle): String = when (style) {
    PaletteStyle.VIBRANT -> "Vibrant"
    PaletteStyle.EXPRESSIVE -> "Expressive"
    PaletteStyle.NEUTRAL -> "Neutral"
}
