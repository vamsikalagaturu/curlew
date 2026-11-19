package io.github.vamsikalagaturu.curlew

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts

data class Field(val dataId: Long?, val value: String, val type: Int, val label: String? = null)

data class AccountOption(val type: String?, val name: String?) {
    val label: String get() = name ?: "Phone"
}

data class Draft(
    val rawContactId: Long?,
    val nameDataId: Long?,
    val name: String,
    val phones: List<Field>,
    val emails: List<Field>,
    val account: AccountOption?,
    val photo: String? = null,
)

sealed interface Loaded {
    data class Editable(val draft: Draft) : Loaded
    data class ReadOnly(val sources: List<String>) : Loaded
}

object ContactWriter {
    // Sync adapters that accept uploads are the writable accounts; WhatsApp, Signal and
    // Meet register as download-only, so their raw contacts must never be edited.
    private fun writableTypes(): Set<String> = ContentResolver.getSyncAdapterTypes()
        .filter { it.authority == ContactsContract.AUTHORITY && it.supportsUploading() }
        .map { it.accountType }
        .toSet()

    @Suppress("DEPRECATION")
    fun accounts(context: Context): List<AccountOption> {
        val writable = writableTypes()
        val found = LinkedHashSet<AccountOption>()
        if (Build.VERSION.SDK_INT >= 33) {
            ContactsContract.Settings.getDefaultAccount(context.contentResolver)
                ?.takeIf { it.type in writable }
                ?.let { found += AccountOption(it.type, it.name) }
        }
        context.contentResolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts.ACCOUNT_TYPE, RawContacts.ACCOUNT_NAME),
            "${RawContacts.DELETED} = 0", null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val type = c.getString(0) ?: continue
                if (type in writable) found += AccountOption(type, c.getString(1))
            }
        }
        found += AccountOption(null, null)
        return found.toList()
    }

    fun blank(number: String?, account: AccountOption?) = Draft(
        rawContactId = null,
        nameDataId = null,
        name = "",
        phones = listOf(Field(null, number.orEmpty(), Phone.TYPE_MOBILE)),
        emails = emptyList(),
        account = account,
    )

    data class DeleteResult(val deleted: Int, val readOnly: Int)

    fun delete(context: Context, contactIds: Collection<Long>): DeleteResult {
        val writable = writableTypes()
        val ops = ArrayList<ContentProviderOperation>()
        var deleted = 0
        var readOnly = 0
        contactIds.forEach { id ->
            val raws = mutableListOf<Long>()
            context.contentResolver.query(
                RawContacts.CONTENT_URI,
                arrayOf(RawContacts._ID, RawContacts.ACCOUNT_TYPE),
                "${RawContacts.CONTACT_ID} = ? AND ${RawContacts.DELETED} = 0", arrayOf(id.toString()), null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val type = c.getString(1)
                    if (type == null || type in writable) raws += c.getLong(0)
                }
            }
            if (raws.isEmpty()) {
                readOnly++
            } else {
                deleted++
                raws.forEach { ops += ContentProviderOperation.newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI, it)).build() }
            }
        }
        if (ops.isNotEmpty()) context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        return DeleteResult(deleted, readOnly)
    }

    fun exportVcf(context: Context, contactIds: Collection<Long>?, target: Uri): Int {
        val resolver = context.contentResolver
        val keys = mutableListOf<String>()
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.LOOKUP_KEY),
            contactIds?.let { "${ContactsContract.Contacts._ID} IN (${it.joinToString()})" },
            null, null,
        )?.use { c -> while (c.moveToNext()) c.getString(0)?.let(keys::add) }
        var written = 0
        resolver.openOutputStream(target)?.use { out ->
            keys.forEach { key ->
                val vcard = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, key)
                runCatching { resolver.openInputStream(vcard)?.use { it.copyTo(out) } }
                    .onSuccess { if (it != null) written++ }
            }
        } ?: error("Couldn't open the file for writing")
        return written
    }

    fun load(context: Context, contactId: Long): Loaded {
        val writable = writableTypes()
        val resolver = context.contentResolver
        var rawId: Long? = null
        var account: AccountOption? = null
        val sources = mutableListOf<String>()
        resolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts._ID, RawContacts.ACCOUNT_TYPE, RawContacts.ACCOUNT_NAME),
            "${RawContacts.CONTACT_ID} = ? AND ${RawContacts.DELETED} = 0", arrayOf(contactId.toString()), null,
        )?.use { c ->
            while (c.moveToNext()) {
                val type = c.getString(1)
                if (rawId == null && (type == null || type in writable)) {
                    rawId = c.getLong(0)
                    account = AccountOption(type, c.getString(2))
                }
                sources += c.getString(2) ?: type ?: "Phone"
            }
        }
        val raw = rawId ?: return Loaded.ReadOnly(sources.distinct())
        val photo = resolver.query(
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
            arrayOf(ContactsContract.Contacts.PHOTO_URI), null, null, null,
        )?.use { if (it.moveToFirst()) it.getString(0) else null }

        var nameId: Long? = null
        var name = ""
        val phones = mutableListOf<Field>()
        val emails = mutableListOf<Field>()
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3),
            "${Data.RAW_CONTACT_ID} = ?", arrayOf(raw.toString()), null,
        )?.use { c ->
            while (c.moveToNext()) {
                val value = c.getString(2).orEmpty()
                when (c.getString(1)) {
                    StructuredName.CONTENT_ITEM_TYPE -> {
                        nameId = c.getLong(0)
                        name = value
                    }
                    Phone.CONTENT_ITEM_TYPE -> phones += Field(c.getLong(0), value, c.getInt(3), c.getString(4))
                    Email.CONTENT_ITEM_TYPE -> emails += Field(c.getLong(0), value, c.getInt(3), c.getString(4))
                }
            }
        }
        return Loaded.Editable(Draft(raw, nameId, name, phones, emails, account, photo))
    }

    fun isStarred(context: Context, contactId: Long): Boolean = context.contentResolver.query(
        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
        arrayOf(ContactsContract.Contacts.STARRED), null, null, null,
    )?.use { it.moveToFirst() && it.getInt(0) == 1 } ?: false

    fun setStarred(context: Context, contactId: Long, starred: Boolean) {
        context.contentResolver.update(
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
            android.content.ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0) },
            null, null,
        )
    }

    fun setPhoto(context: Context, rawContactId: Long, jpeg: ByteArray) {
        val target = Uri.withAppendedPath(
            ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
            RawContacts.DisplayPhoto.CONTENT_DIRECTORY,
        )
        context.contentResolver.openAssetFileDescriptor(target, "rw")?.use { fd ->
            fd.createOutputStream().use { it.write(jpeg) }
        } ?: error("The contacts provider refused the photo")
    }

    fun removePhoto(context: Context, rawContactId: Long) {
        context.contentResolver.delete(
            Data.CONTENT_URI,
            "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
            arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
        )
    }

    fun save(context: Context, original: Draft, draft: Draft): Long? {
        val ops = ArrayList<ContentProviderOperation>()
        val raw = draft.rawContactId
        if (raw == null) {
            ops += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, draft.account?.type)
                .withValue(RawContacts.ACCOUNT_NAME, draft.account?.name)
                .build()
            fun insert(mime: String) = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, mime)
            if (draft.name.isNotBlank()) {
                ops += insert(StructuredName.CONTENT_ITEM_TYPE).withValue(StructuredName.DISPLAY_NAME, draft.name.trim()).build()
            }
            draft.phones.filter { it.value.isNotBlank() }.forEach {
                ops += insert(Phone.CONTENT_ITEM_TYPE).withValue(Phone.NUMBER, it.value.trim()).withValue(Phone.TYPE, it.type).build()
            }
            draft.emails.filter { it.value.isNotBlank() }.forEach {
                ops += insert(Email.CONTENT_ITEM_TYPE).withValue(Email.ADDRESS, it.value.trim()).withValue(Email.TYPE, it.type).build()
            }
        } else {
            fun insert(mime: String) = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValue(Data.RAW_CONTACT_ID, raw)
                .withValue(Data.MIMETYPE, mime)
            val nameId = draft.nameDataId
            if (nameId != null && draft.name != original.name) {
                // Nulling the parsed parts makes the provider re-derive them from the new display name.
                ops += ContentProviderOperation.newUpdate(ContentUris.withAppendedId(Data.CONTENT_URI, nameId))
                    .withValue(StructuredName.DISPLAY_NAME, draft.name.trim())
                    .withValue(StructuredName.GIVEN_NAME, null)
                    .withValue(StructuredName.MIDDLE_NAME, null)
                    .withValue(StructuredName.FAMILY_NAME, null)
                    .withValue(StructuredName.PREFIX, null)
                    .withValue(StructuredName.SUFFIX, null)
                    .build()
            } else if (nameId == null && draft.name.isNotBlank()) {
                ops += insert(StructuredName.CONTENT_ITEM_TYPE).withValue(StructuredName.DISPLAY_NAME, draft.name.trim()).build()
            }
            sync(ops, original.phones, draft.phones, ::insert, Phone.CONTENT_ITEM_TYPE, Phone.NUMBER, Phone.TYPE)
            sync(ops, original.emails, draft.emails, ::insert, Email.CONTENT_ITEM_TYPE, Email.ADDRESS, Email.TYPE)
        }
        if (ops.isEmpty()) return raw
        val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        return raw ?: results.firstOrNull()?.uri?.let(ContentUris::parseId)
    }

    private fun sync(
        ops: MutableList<ContentProviderOperation>,
        before: List<Field>,
        after: List<Field>,
        insert: (String) -> ContentProviderOperation.Builder,
        mime: String,
        valueColumn: String,
        typeColumn: String,
    ) {
        val kept = after.mapNotNull { f -> f.dataId?.takeIf { f.value.isNotBlank() } }.toSet()
        before.filter { it.dataId != null && it.dataId !in kept }.forEach {
            ops += ContentProviderOperation.newDelete(ContentUris.withAppendedId(Data.CONTENT_URI, it.dataId!!)).build()
        }
        val old = before.associateBy { it.dataId }
        after.filter { it.value.isNotBlank() }.forEach { f ->
            val id = f.dataId
            if (id == null) {
                ops += insert(mime).withValue(valueColumn, f.value.trim()).withValue(typeColumn, f.type).build()
            } else {
                val prev = old[id] ?: return@forEach
                if (prev.value == f.value && prev.type == f.type) return@forEach
                val op = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(Data.CONTENT_URI, id))
                    .withValue(valueColumn, f.value.trim())
                if (prev.type != f.type) op.withValue(typeColumn, f.type)
                ops += op.build()
            }
        }
    }
}
