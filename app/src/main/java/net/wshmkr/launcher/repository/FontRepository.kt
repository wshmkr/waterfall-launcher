package net.wshmkr.launcher.repository

import android.content.Context
import android.graphics.fonts.Font
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FontRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingsDataSource: UserSettingsDataSource,
) {
    private val fontsDir: File get() = File(context.filesDir, "fonts")

    suspend fun installCustomFont(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val installed = copyAndValidate(uri) ?: return@withContext false
        userSettingsDataSource.setCustomFontPath(installed.absolutePath)
        deleteFontsExcept(installed)
        true
    }

    suspend fun clearCustomFont() {
        userSettingsDataSource.clearCustomFontPath()
        withContext(Dispatchers.IO) { deleteFontsExcept(null) }
    }

    private fun copyAndValidate(uri: Uri): File? {
        // A fresh directory per install keeps the path unique, so re-picking a
        // same-named file still invalidates the theme's remember(font) cache.
        val target = File(File(fontsDir, System.currentTimeMillis().toString()), displayName(uri))
        return runCatching {
            target.parentFile?.mkdirs()
            requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                target.outputStream().use(input::copyTo)
            }
            // Rejects files the renderer can't parse before the path is persisted.
            Font.Builder(target).build()
            target
        }.getOrElse {
            target.parentFile?.deleteRecursively()
            null
        }
    }

    private fun displayName(uri: Uri): String {
        val name = context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        return name?.substringAfterLast('/')?.ifBlank { null } ?: "font"
    }

    private fun deleteFontsExcept(installed: File?) {
        fontsDir.listFiles()?.forEach { dir ->
            if (dir != installed?.parentFile) dir.deleteRecursively()
        }
    }
}
