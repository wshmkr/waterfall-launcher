package net.wshmkr.launcher.util

import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import net.wshmkr.launcher.model.NotificationAction

// Fires a captured PendingIntent, allowing background activity starts (API 34+) since the
// launcher usually isn't the top app. Returns false when the intent is null or already cancelled.
fun sendPendingIntent(
    context: Context,
    pendingIntent: PendingIntent?,
    fillInIntent: Intent? = null,
): Boolean {
    if (pendingIntent == null) return false
    return try {
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
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

// Reply actions read their result out of the fill-in intent. Firing one bare leaves the app with
// no results bundle, which is why an unfilled reply either no-ops or posts an empty message.
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
        // Only the result key is read back out, so rebuilding the input here keeps RemoteInput —
        // which compares by identity — out of the model, where it would break notification equality.
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
