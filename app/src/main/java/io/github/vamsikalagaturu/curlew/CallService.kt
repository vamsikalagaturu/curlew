@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package io.github.vamsikalagaturu.curlew

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CallService : InCallService() {
    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        CallManager.attach(this)
        CallNotifier.createChannels(this)
        scope.launch { CallManager.calls.collect { CallNotifier.update(this@CallService, it) } }
    }

    override fun onDestroy() {
        scope.cancel()
        CallNotifier.cancel(this)
        CallManager.detach(this)
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        CallManager.add(applicationContext, call)
        // Incoming calls surface through the full-screen notification so a call never
        // steals the screen from whatever the user is doing while unlocked.
        if (call.stateCompat() != Call.STATE_RINGING) startActivity(InCallActivity.intent(this))
    }

    override fun onCallRemoved(call: Call) {
        CallManager.remove(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        CallManager.onAudio(audioState)
    }
}
