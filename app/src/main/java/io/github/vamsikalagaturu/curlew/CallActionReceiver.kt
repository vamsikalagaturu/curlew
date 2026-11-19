package io.github.vamsikalagaturu.curlew

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.Call

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val calls = CallManager.calls.value
        when (intent.action) {
            ACTION_DECLINE -> calls.firstOrNull { it.state == Call.STATE_RINGING }?.let { CallManager.reject(it.call) }
            ACTION_HANGUP -> calls.firstOrNull { it.state != Call.STATE_RINGING }?.let { CallManager.hangup(it.call) }
        }
    }

    companion object {
        const val ACTION_DECLINE = "io.github.vamsikalagaturu.curlew.DECLINE"
        const val ACTION_HANGUP = "io.github.vamsikalagaturu.curlew.HANGUP"
    }
}
