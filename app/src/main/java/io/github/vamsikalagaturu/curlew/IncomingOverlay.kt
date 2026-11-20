package io.github.vamsikalagaturu.curlew

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.telecom.Call
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.vamsikalagaturu.curlew.ui.CurlewTheme
import io.github.vamsikalagaturu.curlew.ui.IncomingCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

// A Service has no lifecycle or saved-state owner, and ComposeView needs both before it can attach to a window.
class IncomingOverlay(context: Context) : LifecycleOwner, SavedStateRegistryOwner {
    private val context: Context = if (Build.VERSION.SDK_INT >= 31) {
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        context.createWindowContext(display, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    } else {
        context
    }
    private val windows = this.context.getSystemService(WindowManager::class.java)
    private val registry = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    private val ringing = MutableStateFlow<CallInfo?>(null)
    private val present = MutableStateFlow(false)
    private val removal = Runnable { if (!present.value) detach() }
    private var view: ComposeView? = null
    private var opened: Call? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP }

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

    init {
        saved.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    /** Puts [call] on the card, or removes the card when null. Returns whether the card is on screen. */
    fun update(call: CallInfo?): Boolean {
        if (call == null) opened = null
        if (call == null || call.call === opened || !Settings.canDrawOverlays(context)) {
            hide()
            return false
        }
        ringing.value = call
        present.value = true
        view?.let {
            it.removeCallbacks(removal)
            return true
        }
        return show()
    }

    fun destroy() {
        present.value = false
        detach()
        registry.currentState = Lifecycle.State.DESTROYED
    }

    private fun show(): Boolean {
        val card = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@IncomingOverlay)
            setViewTreeSavedStateRegistryOwner(this@IncomingOverlay)
            setContent {
                val info by ringing.collectAsState()
                val visible by present.collectAsState()
                CurlewTheme {
                    IncomingCard(
                        info,
                        visible,
                        onOpen = { info?.let { open(it, answer = false) } },
                        onAnswer = { info?.let { open(it, answer = true) } },
                        onDecline = { info?.let { CallManager.reject(it.call) } },
                        onSilence = { info?.let { CallManager.silence(context, it.call) } },
                        onDrag = ::move,
                    )
                }
            }
        }
        return runCatching { windows.addView(card, params) }
            .onSuccess {
                view = card
                registry.currentState = Lifecycle.State.RESUMED
            }
            .isSuccess
    }

    // The window stays until the card's slide-out animation (220 ms) has played.
    private fun hide() {
        val card = view ?: run {
            present.value = false
            return
        }
        if (!present.value) return
        present.value = false
        card.postDelayed(removal, 260)
    }

    private fun detach() {
        val card = view ?: return
        view = null
        card.removeCallbacks(removal)
        runCatching { windows.removeView(card) }
        registry.currentState = Lifecycle.State.CREATED
    }

    private fun open(info: CallInfo, answer: Boolean) {
        opened = info.call
        val intent = InCallActivity.intent(context)
        if (answer) intent.action = InCallActivity.ACTION_ANSWER
        context.startActivity(intent)
        // Removing the window from inside its own click dispatch would dispose the composition mid-event.
        view?.post { hide() }
    }

    private fun move(dy: Float) {
        val card = view ?: return
        params.y = (params.y + dy.roundToInt()).coerceAtLeast(0)
        runCatching { windows.updateViewLayout(card, params) }
    }
}
