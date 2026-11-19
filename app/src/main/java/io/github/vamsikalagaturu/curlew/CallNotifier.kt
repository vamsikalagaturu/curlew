package io.github.vamsikalagaturu.curlew

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telecom.Call
import androidx.core.app.NotificationCompat
import androidx.core.app.Person

object CallNotifier {
    private const val CHANNEL_INCOMING = "incoming"
    private const val CHANNEL_ONGOING = "ongoing"
    private const val ID = 1

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_INCOMING, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
                // Telecom plays the ringtone and vibration itself; a channel sound would double it.
                setSound(null, null)
                enableVibration(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, "Ongoing calls", NotificationManager.IMPORTANCE_LOW),
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(ID)
    }

    fun update(context: Context, calls: List<CallInfo>) {
        val top = calls.firstOrNull { it.state != Call.STATE_DISCONNECTED } ?: return cancel(context)
        val ringing = top.state == Call.STATE_RINGING
        val open = PendingIntent.getActivity(
            context, 0, InCallActivity.intent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, if (ringing) CHANNEL_INCOMING else CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (ringing) {
            val person = Person.Builder().setName(top.label).setImportant(true).build()
            val answer = PendingIntent.getActivity(
                context, 1, InCallActivity.intent(context).setAction(InCallActivity.ACTION_ANSWER),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder
                .setFullScreenIntent(open, true)
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, broadcast(context, CallActionReceiver.ACTION_DECLINE), answer))
        } else {
            builder
                .setContentTitle(top.label)
                .setContentText(stateLabel(top.state))
                .addAction(0, "Hang up", broadcast(context, CallActionReceiver.ACTION_HANGUP))
            if (top.state == Call.STATE_ACTIVE && top.connectedAt > 0) {
                builder.setUsesChronometer(true).setWhen(top.connectedAt).setShowWhen(true)
            }
        }
        context.getSystemService(NotificationManager::class.java).notify(ID, builder.build())
    }

    private fun broadcast(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
        context, action.hashCode(),
        Intent(context, CallActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
