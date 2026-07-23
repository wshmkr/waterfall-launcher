package net.wshmkr.launcher.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun openAppDetailsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

// Only meaningful right after a denial result; also false before the first request.
fun isPermissionPermanentlyDenied(context: Context, permission: String): Boolean =
    context.findActivity()?.let { activity ->
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    } == true
