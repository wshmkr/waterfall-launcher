package net.wshmkr.launcher.model

// AUTO follows the wallpaper's own lightness; LIGHT and DARK force it.
enum class ThemeMode {
    AUTO,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: AUTO
    }
}
