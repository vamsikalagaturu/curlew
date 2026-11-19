package io.github.vamsikalagaturu.curlew

import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers

// Android's own block list: only the default dialer (and SMS app) may read or write it,
// and Telecom rejects calls from these numbers before Curlew ever sees them.
object Blocking {
    fun available(context: Context): Boolean =
        runCatching { BlockedNumberContract.canCurrentUserBlockNumbers(context) }.getOrDefault(false)

    fun list(context: Context): List<String> = runCatching {
        context.contentResolver.query(
            BlockedNumbers.CONTENT_URI,
            arrayOf(BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
            null, null, null,
        )?.use { c -> buildList { while (c.moveToNext()) c.getString(0)?.let(::add) } }.orEmpty()
    }.getOrDefault(emptyList())

    fun isBlocked(context: Context, number: String): Boolean =
        runCatching { BlockedNumberContract.isBlocked(context, number) }.getOrDefault(false)

    fun block(context: Context, number: String) {
        if (number.isBlank()) return
        context.contentResolver.insert(
            BlockedNumbers.CONTENT_URI,
            ContentValues().apply { put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number) },
        )
    }

    fun unblock(context: Context, number: String) {
        BlockedNumberContract.unblock(context, number)
    }
}
