package net.wshmkr.launcher.util

import android.content.Context
import android.content.Intent
import android.util.Log

fun launchPackage(context: Context, packageName: String): Boolean {
    return try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        Log.w("PackageLauncher", "Failed to launch $packageName", e)
        false
    }
}
