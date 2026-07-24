package net.wshmkr.launcher.model

enum class HomeTextColor {
    AUTO,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): HomeTextColor =
            entries.firstOrNull { it.name == name } ?: AUTO
    }
}
