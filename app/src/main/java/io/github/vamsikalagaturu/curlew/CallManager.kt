@file:Suppress("DEPRECATION")

package io.github.vamsikalagaturu.curlew

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.PhoneAccountHandle
import android.telecom.VideoProfile
import androidx.core.os.BundleCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CallInfo(
    val call: Call,
    val state: Int,
    val number: String,
    val name: String?,
    val photo: String?,
    val sim: String?,
    val connectedAt: Long,
    val isConference: Boolean,
    val canHold: Boolean,
    val canMerge: Boolean,
    val canSwap: Boolean,
    val accounts: List<PhoneAccountHandle>,
) {
    val label: String get() = name ?: number.ifEmpty { "Unknown" }
}

fun Call.stateCompat(): Int = if (Build.VERSION.SDK_INT >= 31) details.state else state

fun Call.number(): String = details.handle?.schemeSpecificPart.orEmpty()

fun stateLabel(state: Int): String = when (state) {
    Call.STATE_RINGING, Call.STATE_SIMULATED_RINGING -> "Incoming call"
    Call.STATE_NEW, Call.STATE_DIALING -> "Calling…"
    Call.STATE_CONNECTING -> "Connecting…"
    Call.STATE_ACTIVE -> "Connected"
    Call.STATE_HOLDING -> "On hold"
    Call.STATE_DISCONNECTING -> "Ending…"
    Call.STATE_DISCONNECTED -> "Call ended"
    Call.STATE_SELECT_PHONE_ACCOUNT -> "Choose a SIM"
    else -> ""
}

object CallManager {
    private val scope = MainScope()
    private val tracked = mutableListOf<Call>()
    private val names = HashMap<String, Phonebook.Caller?>()
    private var service: CallService? = null

    private val _calls = MutableStateFlow<List<CallInfo>>(emptyList())
    val calls: StateFlow<List<CallInfo>> = _calls

    private val _audio = MutableStateFlow<CallAudioState?>(null)
    val audio: StateFlow<CallAudioState?> = _audio

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = publish()
        override fun onDetailsChanged(call: Call, details: Call.Details) = publish()
        override fun onChildrenChanged(call: Call, children: List<Call>) = publish()
        override fun onParentChanged(call: Call, parent: Call?) = publish()
        override fun onConferenceableCallsChanged(call: Call, calls: List<Call>) = publish()
    }

    fun attach(service: CallService) {
        this.service = service
    }

    fun detach(service: CallService) {
        if (this.service === service) this.service = null
        _audio.value = null
    }

    fun add(context: Context, call: Call) {
        tracked += call
        call.registerCallback(callback)
        val number = call.number()
        if (number.isNotEmpty() && !names.containsKey(number)) {
            names[number] = null
            scope.launch {
                names[number] = withContext(Dispatchers.IO) { Phonebook.lookup(context, number) }
                publish()
            }
        }
        publish()
    }

    fun remove(call: Call) {
        call.unregisterCallback(callback)
        tracked -= call
        if (tracked.isEmpty()) names.clear()
        publish()
    }

    fun onAudio(state: CallAudioState) {
        _audio.value = state
    }

    fun answer(call: Call) = call.answer(VideoProfile.STATE_AUDIO_ONLY)
    fun reject(call: Call) = call.reject(false, null)

    // Telecom sends the text itself through the default SMS app's channel; no SMS permission is needed here.
    fun rejectWithMessage(call: Call, text: String) = call.reject(true, text)
    fun hangup(call: Call) = call.disconnect()
    fun setMuted(muted: Boolean) = service?.setMuted(muted)
    fun setRoute(route: Int) = service?.setAudioRoute(route)
    fun selectAccount(call: Call, handle: PhoneAccountHandle) = call.phoneAccountSelected(handle, false)
    fun dtmfDown(call: Call, digit: Char) = call.playDtmfTone(digit)
    fun dtmfUp(call: Call) = call.stopDtmfTone()

    fun toggleHold(info: CallInfo) {
        if (info.state == Call.STATE_HOLDING) info.call.unhold() else info.call.hold()
    }

    fun merge(info: CallInfo) {
        val other = info.call.conferenceableCalls.firstOrNull()
        if (other != null) info.call.conference(other) else info.call.mergeConference()
    }

    fun swap(info: CallInfo) = info.call.swapConference()

    private fun publish() {
        _calls.value = tracked
            .filter { it.parent == null }
            .map { it.toInfo() }
            .sortedBy { order(it.state) }
    }

    private fun order(state: Int) = when (state) {
        Call.STATE_RINGING, Call.STATE_SIMULATED_RINGING -> 0
        Call.STATE_HOLDING -> 2
        Call.STATE_DISCONNECTED -> 3
        else -> 1
    }

    private fun simLabel(details: Call.Details): String? {
        val context = service ?: return null
        val handle = details.accountHandle ?: return null
        val accounts = callAccounts(context)
        if (accounts.size < 2) return null
        return accounts.firstOrNull { it.handle == handle }?.label
    }

    private fun Call.toInfo(): CallInfo {
        val d = details
        val conference = d.hasProperty(Call.Details.PROPERTY_CONFERENCE)
        val number = number()
        val accounts = d.extras?.let {
            BundleCompat.getParcelableArrayList(it, Call.AVAILABLE_PHONE_ACCOUNTS, PhoneAccountHandle::class.java)
        }.orEmpty()
        return CallInfo(
            call = this,
            state = stateCompat(),
            number = number,
            name = when {
                conference -> "Conference call"
                else -> names[number]?.name ?: d.callerDisplayName?.takeIf { it.isNotBlank() }
            },
            photo = if (conference) null else names[number]?.photo,
            sim = simLabel(d),
            connectedAt = d.connectTimeMillis,
            isConference = conference,
            canHold = d.can(Call.Details.CAPABILITY_HOLD),
            canMerge = d.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) || conferenceableCalls.isNotEmpty(),
            canSwap = d.can(Call.Details.CAPABILITY_SWAP_CONFERENCE),
            accounts = accounts,
        )
    }
}
