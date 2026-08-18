package net.wshmkr.launcher.util

import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import net.wshmkr.launcher.model.NotificationAction

// Background activity starts are allowed explicitly (API 34+) as the launcher isn't the top app.
fun sendPendingIntent(
    context: Context,
    pendingIntent: PendingIntent?,
    fillInIntent: Intent? = null,
): Boolean {
    if (pendingIntent == null) return false
    return try {
        // ALLOW_ALWAYS inlines to a value pre-36 platforms don't recognize.
        val startMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
        } else {
            @Suppress("DEPRECATION")
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(startMode)
                .toBundle()
        } else {
            null
        }
        pendingIntent.send(context, 0, fillInIntent, null, null, null, options)
        true
    } catch (e: Exception) {
        false
    }
}

// Reply actions read their result out of the fill-in intent; fired bare they post nothing.
fun sendReply(
    context: Context,
    action: NotificationAction,
    response: String,
    fromChoice: Boolean,
): Boolean {
    val reply = action.reply ?: return false
    val results = Bundle().apply { putCharSequence(reply.resultKey, response) }
    val fillInIntent = Intent().apply {
        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        // Rebuilt here to keep RemoteInput, which compares by identity, out of the model.
        RemoteInput.addResultsToIntent(
            arrayOf(RemoteInput.Builder(reply.resultKey).build()),
            this,
            results,
        )
        RemoteInput.setResultsSource(
            this,
            if (fromChoice) RemoteInput.SOURCE_CHOICE else RemoteInput.SOURCE_FREE_FORM_INPUT,
        )
    }
    return sendPendingIntent(context, action.actionIntent, fillInIntent)
}
