package net.wshmkr.launcher.model

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
