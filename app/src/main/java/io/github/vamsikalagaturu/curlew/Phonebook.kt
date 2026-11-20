package io.github.vamsikalagaturu.curlew

import android.Manifest
import android.annotation.SuppressLint
import android.telephony.TelephonyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.PhoneLookup
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import android.widget.Toast

data class Contact(
    val id: Long,
    val name: String,
    val number: String,
    val label: String,
    val photo: String? = null,
    val starred: Boolean = false,
) {
    val digits: String = number.filter { it.isDigit() }
}

data class RecentCall(
    val id: Long,
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val duration: Long,
    val account: String? = null,
)

data class RecentGroup(
    val key: String,
    val name: String?,
    val contactId: Long?,
    val calls: List<RecentCall>,
    val photo: String? = null,
) {
    val latest: RecentCall get() = calls.first()
    val number: String get() = latest.number
    val label: String get() = name ?: number.takeIf { it.isNotEmpty() }?.let(::formatNumber) ?: "Private number"
}

// Matching on the last 10 digits treats "+49 151 …" and "0151 …" as the same line.
fun numberKey(number: String): String = number.filter { it.isDigit() }.takeLast(10)

object Phonebook {
    data class Caller(val name: String?, val photo: String?)

    fun lookup(context: Context, number: String): Caller = try {
        context.contentResolver.query(
            Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
            arrayOf(PhoneLookup.DISPLAY_NAME, PhoneLookup.PHOTO_THUMBNAIL_URI), null, null, null,
        )?.use { if (it.moveToFirst()) Caller(it.getString(0), it.getString(1)) else null } ?: Caller(null, null)
    } catch (_: SecurityException) {
        Caller(null, null)
    }

    fun contacts(context: Context): List<Contact> = try {
        context.contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(
                Phone.CONTACT_ID, Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER, Phone.TYPE, Phone.LABEL,
                Phone.PHOTO_THUMBNAIL_URI, Phone.STARRED,
            ),
            null, null, "${Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE",
        )?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val number = c.getString(2)?.trim()?.takeIf { it.any(Char::isDigit) } ?: continue
                    val label = Phone.getTypeLabel(context.resources, c.getInt(3), c.getString(4)).toString()
                    add(Contact(c.getLong(0), c.getString(1) ?: number, number, label, c.getString(5), c.getInt(6) == 1))
                }
            }.distinctBy { it.id to numberKey(it.number) }
        }.orEmpty()
    } catch (_: SecurityException) {
        emptyList()
    }

    fun recents(context: Context, limit: Int = 2000): List<RecentCall> = try {
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION,
                CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, CallLog.Calls.PHONE_ACCOUNT_ID,
            ),
            null, null, "${CallLog.Calls.DATE} DESC",
        )?.use { c ->
            buildList {
                while (size < limit && c.moveToNext()) {
                    val component = c.getString(6)
                    val accountId = c.getString(7)
                    add(
                        RecentCall(
                            c.getLong(0), c.getString(1).orEmpty(), c.getString(2)?.takeIf { it.isNotBlank() },
                            c.getInt(3), c.getLong(4), c.getLong(5),
                            if (component != null && accountId != null) "$component|$accountId" else null,
                        ),
                    )
                }
            }
        }.orEmpty()
    } catch (_: SecurityException) {
        emptyList()
    }

    fun deleteCalls(context: Context, ids: Collection<Long>): Int = ids.chunked(500).sumOf { chunk ->
        context.contentResolver.delete(CallLog.Calls.CONTENT_URI, "${CallLog.Calls._ID} IN (${chunk.joinToString()})", null)
    }

    fun group(calls: List<RecentCall>, contacts: List<Contact>): List<RecentGroup> {
        val byNumber = HashMap<String, Contact>()
        contacts.forEach { byNumber.putIfAbsent(numberKey(it.number), it) }
        val groups = LinkedHashMap<String, MutableList<RecentCall>>()
        val owners = HashMap<String, Contact?>()
        calls.forEach { call ->
            val contact = byNumber[numberKey(call.number)]
            val key = when {
                contact != null -> "c${contact.id}"
                call.number.isEmpty() -> "private"
                else -> "n${numberKey(call.number)}"
            }
            groups.getOrPut(key) { mutableListOf() } += call
            owners.putIfAbsent(key, contact)
        }
        return groups.map { (key, list) ->
            val contact = owners[key]
            RecentGroup(key, contact?.name ?: list.firstNotNullOfOrNull { it.name }, contact?.id, list, contact?.photo)
        }
    }
}

fun Context.placeCall(number: String, account: PhoneAccountHandle? = null) =
    placeCall(Uri.fromParts("tel", number, null), account)

fun Context.placeCall(uri: Uri, account: PhoneAccountHandle? = null) {
    if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(this, "Allow the Phone permission to place calls", Toast.LENGTH_SHORT).show()
        return
    }
    val extras = Bundle()
    val wanted = Prefs.get(this).callAccount.value
    (account ?: callAccounts(this).firstOrNull { it.key == wanted }?.handle)?.let {
        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
    }
    getSystemService(TelecomManager::class.java).placeCall(uri, extras)
}

private val SECRET_CODE = Regex("""^\*#\*#(\d+)#\*#\*$""")

// Codes that act as soon as they are typed, the way stock dialers handle them. Returns true when consumed.
fun Context.handleTypedCode(number: String): Boolean {
    SECRET_CODE.matchEntire(number)?.let { match ->
        runCatching { getSystemService(TelephonyManager::class.java).sendDialerSpecialCode(match.groupValues[1]) }
            .onFailure { Toast.makeText(this, "Only the default phone app can send this code", Toast.LENGTH_SHORT).show() }
        return true
    }
    if (number == "*#06#") {
        // Apps cannot read the IMEI on Android 10+, not even the default dialer; the system status page can.
        Toast.makeText(this, "Android shows the IMEI under About phone → Status", Toast.LENGTH_LONG).show()
        runCatching { startActivity(Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        return true
    }
    return false
}

// Carrier codes such as *123# go to Telephony's MMI handling (USSD dialogs) instead of being dialled.
@SuppressLint("MissingPermission")
fun Context.dial(number: String, account: PhoneAccountHandle? = null) {
    val mmi = number.length > 1 && (number.startsWith('*') || number.startsWith('#')) && number.endsWith('#')
    val handled = mmi && runCatching {
        val telecom = getSystemService(TelecomManager::class.java)
        if (account != null) telecom.handleMmi(number, account) else telecom.handleMmi(number)
    }.getOrDefault(false)
    if (!handled) placeCall(number, account)
}

data class CallAccount(val key: String, val handle: PhoneAccountHandle, val label: String, val number: String? = null) {
    val display: String get() = listOfNotNull(label, number?.let(::formatNumber)).joinToString(" · ")
}

fun callAccounts(context: Context): List<CallAccount> = try {
    val telecom = context.getSystemService(TelecomManager::class.java)
    telecom.callCapablePhoneAccounts.map { handle ->
        val account = telecom.getPhoneAccount(handle)
        val label = account?.label?.toString()?.takeIf { it.isNotBlank() } ?: handle.id
        val number = simNumber(context, handle) ?: account?.address?.schemeSpecificPart
        CallAccount("${handle.componentName.flattenToString()}|${handle.id}", handle, label, number?.takeIf { it.isNotBlank() })
    }
} catch (_: SecurityException) {
    emptyList()
}

// Carriers often leave the number off the SIM, so this is null for many SIMs even with READ_PHONE_NUMBERS granted.
@SuppressLint("MissingPermission", "HardwareIds")
private fun simNumber(context: Context, handle: PhoneAccountHandle): String? {
    if (Build.VERSION.SDK_INT < 30) return null
    if (context.checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) return null
    return runCatching {
        val subId = context.getSystemService(TelephonyManager::class.java).getSubscriptionId(handle)
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return null
        val subscriptions = context.getSystemService(SubscriptionManager::class.java)
        if (Build.VERSION.SDK_INT >= 33) {
            subscriptions.getPhoneNumber(subId)
        } else {
            @Suppress("DEPRECATION")
            subscriptions.getActiveSubscriptionInfo(subId)?.number
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

fun Context.copyNumber(number: String) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Phone number", number))
    // Android 13+ confirms clipboard writes itself; older versions show nothing.
    if (Build.VERSION.SDK_INT < 33) Toast.makeText(this, "Number copied", Toast.LENGTH_SHORT).show()
}

fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null))
}

fun formatNumber(number: String): String =
    PhoneNumberUtils.formatNumber(number, java.util.Locale.getDefault().country)?.takeIf { it.isNotBlank() } ?: number
