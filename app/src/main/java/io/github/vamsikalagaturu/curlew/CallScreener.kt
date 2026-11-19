package io.github.vamsikalagaturu.curlew

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager

class CallScreener : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        val allow = CallResponse.Builder().build()
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(details, allow)
            return
        }
        val prefs = Prefs.get(this)
        val number = details.handle?.schemeSpecificPart.orEmpty()
        val hidden = number.isBlank() || details.handlePresentation != TelecomManager.PRESENTATION_ALLOWED
        val stranger = !hidden && prefs.unknownCallers.value != UnknownCallers.Allow &&
            Phonebook.lookup(this, number).name == null
        val response = when {
            hidden && prefs.blockPrivate.value -> reject()
            stranger && prefs.unknownCallers.value == UnknownCallers.Block -> reject()
            stranger -> CallResponse.Builder().setSilenceCall(true).build()
            else -> allow
        }
        respondToCall(details, response)
    }

    private fun reject() = CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectCall(true)
        .setSkipCallLog(false)
        .setSkipNotification(false)
        .build()
}
