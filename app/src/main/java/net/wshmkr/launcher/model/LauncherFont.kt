package net.wshmkr.launcher.model

sealed interface LauncherFont {
    data object Bundled : LauncherFont

    data class UserFile(val path: String) : LauncherFont
}
