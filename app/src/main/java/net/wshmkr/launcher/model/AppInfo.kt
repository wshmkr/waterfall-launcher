package net.wshmkr.launcher.model

import android.os.UserHandle
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.BitmapPainter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: BitmapPainter,
    val userHandle: UserHandle,
    val isSystemApp: Boolean = false,

    val isFavorite: Boolean,
    val isHidden: Boolean,
    val doNotSuggest: Boolean,
    val isSuggested: Boolean = false,
    val searchTokens: ImmutableList<String> = persistentListOf(),
) {
    val key: String
        get() = keyFor(packageName, userHandle)
}

fun keyFor(packageName: String, userHandle: UserHandle): String {
    return "${packageName}_${userHandle.hashCode()}"
}

val String.sectionLetter: String
    get() = firstOrNull()?.uppercaseChar()?.toString() ?: "#"
