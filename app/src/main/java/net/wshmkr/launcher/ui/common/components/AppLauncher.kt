package net.wshmkr.launcher.ui.common.components

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppLauncher(launchAppIntent: SharedFlow<String>) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        launchAppIntent.collectLatest { packageName ->
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                launchIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
