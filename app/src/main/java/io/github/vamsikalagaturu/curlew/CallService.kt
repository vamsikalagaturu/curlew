@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package io.github.vamsikalagaturu.curlew

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.core.content.ContextCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CallService : InCallService() {
    private val scope = MainScope()
    private lateinit var overlay: IncomingOverlay
    private val locked = MutableStateFlow(false)
    private val screen = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            locked.value = isLocked()
        }
    }

    override fun onCreate() {
        super.onCreate()
        CallManager.attach(this)
        CallNotifier.createChannels(this)
        overlay = IncomingOverlay(this)
        locked.value = isLocked()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screen, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        scope.launch {
            combine(CallManager.calls, InCallActivity.visible, locked, ::Triple).collect { (calls, visible, locked) ->
                val ringing = calls.firstOrNull()?.takeIf { it.state == Call.STATE_RINGING }
                // The card is for an unlocked phone in use; when locked, the full-screen intent takes over.
                val card = overlay.update(ringing?.takeUnless { visible || locked })
                CallNotifier.update(this@CallService, calls, quiet = card || visible)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        unregisterReceiver(screen)
        overlay.destroy()
        CallNotifier.cancel(this)
        CallManager.detach(this)
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        CallManager.add(applicationContext, call)
        // Incoming calls surface through the floating card or the full-screen notification, so a call
        // never steals the screen from whatever the user is doing while unlocked.
        if (call.stateCompat() != Call.STATE_RINGING) startActivity(InCallActivity.intent(this))
    }

    override fun onCallRemoved(call: Call) {
        CallManager.remove(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        CallManager.onAudio(audioState)
    }

    // A screen that is off counts as locked: the keyguard may lock only after a delay.
    private fun isLocked() =
        !getSystemService(PowerManager::class.java).isInteractive || getSystemService(KeyguardManager::class.java).isKeyguardLocked
}
