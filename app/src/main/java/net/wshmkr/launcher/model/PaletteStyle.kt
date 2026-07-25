package net.wshmkr.launcher.model

// How the wallpaper's seed color is expanded into a Material color scheme.
enum class PaletteStyle {
    VIBRANT,
    EXPRESSIVE,
    NEUTRAL;

    companion object {
        val Default = VIBRANT

        fun fromName(name: String?): PaletteStyle =
            entries.firstOrNull { it.name == name } ?: Default
    }
}
