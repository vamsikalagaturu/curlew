package io.github.vamsikalagaturu.curlew

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.provider.ContactsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PhoneData(private val context: Context, scope: CoroutineScope) {
    private val _contacts = MutableStateFlow<List<Contact>?>(null)
    val contacts: StateFlow<List<Contact>?> = _contacts

    private val _recents = MutableStateFlow<List<RecentGroup>?>(null)
    val recents: StateFlow<List<RecentGroup>?> = _recents

    private val _blocked = MutableStateFlow<Map<String, String>>(emptyMap())
    val blocked: StateFlow<Map<String, String>> = _blocked

    private val reloads = Channel<Unit>(Channel.CONFLATED)
    private var observing = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = reload()
    }

    init {
        scope.launch(Dispatchers.IO) {
            for (request in reloads) {
                val contacts = Phonebook.contacts(context)
                val calls = Phonebook.recents(context)
                _contacts.value = contacts
                _recents.value = Phonebook.group(calls, contacts)
                _blocked.value = Blocking.list(context).associateBy(::numberKey)
                // Sync adapters (WhatsApp, Signal) fire bursts of change notifications; this
                // window folds a burst into a single reload.
                delay(500)
            }
        }
    }

    fun reload() {
        reloads.trySend(Unit)
    }

    fun start() {
        if (!observing) {
            observing = runCatching {
                context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
                context.contentResolver.registerContentObserver(ContactsContract.AUTHORITY_URI, true, observer)
                context.contentResolver.registerContentObserver(BlockedNumberContract.AUTHORITY_URI, true, observer)
            }.isSuccess
        }
        reload()
    }

    fun stop() {
        if (observing) context.contentResolver.unregisterContentObserver(observer)
        observing = false
    }
}
