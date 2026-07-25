package net.wshmkr.launcher.model

// How the wallpaper's seed color is expanded into a Material color scheme.
enum class PaletteStyle {
    FIDELITY,
    EXPRESSIVE,
    NEUTRAL;

    companion object {
        fun fromName(name: String?): PaletteStyle =
            entries.firstOrNull { it.name == name } ?: FIDELITY
    }
}
