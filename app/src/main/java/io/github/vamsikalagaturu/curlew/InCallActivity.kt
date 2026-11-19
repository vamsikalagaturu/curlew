@file:Suppress("DEPRECATION")

package io.github.vamsikalagaturu.curlew

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.vamsikalagaturu.curlew.ui.CurlewTheme
import io.github.vamsikalagaturu.curlew.ui.InCallScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class InCallActivity : ComponentActivity() {
    private var proximity: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handle(intent)
        setContent {
            CurlewTheme {
                InCallScreen(
                    onAddCall = {
                        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    onNoCalls = ::finish,
                )
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(CallManager.calls, CallManager.audio) { calls, audio ->
                    val live = calls.any { it.state in LIVE_STATES }
                    live && (audio?.route ?: CallAudioState.ROUTE_EARPIECE) == CallAudioState.ROUTE_EARPIECE
                }.distinctUntilChanged().collect(::setProximity)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onStop() {
        setProximity(false)
        super.onStop()
    }

    private fun handle(intent: Intent) {
        if (intent.action == ACTION_ANSWER) {
            CallManager.calls.value.firstOrNull { it.state == Call.STATE_RINGING }?.let { CallManager.answer(it.call) }
        }
    }

    private fun setProximity(on: Boolean) {
        val held = proximity
        if (on && held == null) {
            val pm = getSystemService(PowerManager::class.java)
            if (pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                proximity = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "curlew:proximity").apply { acquire() }
            }
        } else if (!on && held != null) {
            held.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
            proximity = null
        }
    }

    companion object {
        const val ACTION_ANSWER = "io.github.vamsikalagaturu.curlew.ANSWER"
        private val LIVE_STATES = setOf(Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING)

        fun intent(context: Context): Intent = Intent(context, InCallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
