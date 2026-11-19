package io.github.vamsikalagaturu.curlew

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.TelecomManager
import androidx.core.app.NotificationCompat

// Telecom hands missed-call notifications to the default dialer instead of posting its own once the
// dialer listens for SHOW_MISSED_CALLS_NOTIFICATION; it re-sends the current count after boot too.
class MissedCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION -> MissedCalls.show(
                context,
                intent.getIntExtra(TelecomManager.EXTRA_NOTIFICATION_COUNT, 0),
                intent.getStringExtra(TelecomManager.EXTRA_NOTIFICATION_PHONE_NUMBER),
            )
            MissedCalls.ACTION_DISMISSED -> MissedCalls.clear(context)
        }
    }
}

object MissedCalls {
    const val ACTION_DISMISSED = "io.github.vamsikalagaturu.curlew.MISSED_DISMISSED"
    private const val CHANNEL = "missed"
    private const val ID = 2

    fun show(context: Context, count: Int, number: String?) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (count <= 0) {
            nm.cancel(ID)
            return
        }
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Missed calls", NotificationManager.IMPORTANCE_DEFAULT))
        val known = number?.takeIf { it.isNotBlank() }
        val name = known?.let { Phonebook.lookup(context, it).name }
        val title = when {
            count > 1 -> "$count missed calls"
            name != null -> name
            known != null -> formatNumber(known)
            else -> "Private number"
        }
        val open = PendingIntent.getActivity(
            context, 20,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_SHOW_RECENTS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val dismissed = PendingIntent.getBroadcast(
            context, 21,
            Intent(context, MissedCallReceiver::class.java).setAction(ACTION_DISMISSED),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(if (count > 1) "Tap to see who called" else "Missed call")
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setContentIntent(open)
            .setDeleteIntent(dismissed)
            .setAutoCancel(true)
            .setNumber(count)
        if (count == 1 && known != null) {
            val callBack = PendingIntent.getActivity(
                context, 22,
                Intent(context, MainActivity::class.java)
                    .setAction(MainActivity.ACTION_CALL_BACK)
                    .setData(Uri.fromParts("tel", known, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val message = PendingIntent.getActivity(
                context, 23,
                Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", known, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(0, "Call back", callBack).addAction(0, "Message", message)
        }
        nm.notify(ID, builder.build())
    }

    // Default-dialer callers are exempt from MODIFY_PHONE_STATE here; Telecom then marks the missed
    // calls read and answers with a zero-count broadcast that cancels the notification.
    @SuppressLint("MissingPermission")
    fun clear(context: Context) {
        runCatching { context.getSystemService(TelecomManager::class.java).cancelMissedCallsNotification() }
        context.getSystemService(NotificationManager::class.java).cancel(ID)
    }
}
