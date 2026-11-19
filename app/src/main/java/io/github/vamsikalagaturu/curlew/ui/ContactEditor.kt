package io.github.vamsikalagaturu.curlew.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.BaseTypes
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import io.github.vamsikalagaturu.curlew.AccountOption
import io.github.vamsikalagaturu.curlew.Blocking
import io.github.vamsikalagaturu.curlew.ContactWriter
import io.github.vamsikalagaturu.curlew.Draft
import io.github.vamsikalagaturu.curlew.Field
import io.github.vamsikalagaturu.curlew.Loaded
import io.github.vamsikalagaturu.curlew.formatNumber
import io.github.vamsikalagaturu.curlew.shareText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class EditorRequest(val contactId: Long?, val number: String? = null)

private val PHONE_TYPES = listOf(Phone.TYPE_MOBILE, Phone.TYPE_HOME, Phone.TYPE_WORK, Phone.TYPE_OTHER)
private val EMAIL_TYPES = listOf(Email.TYPE_HOME, Email.TYPE_WORK, Email.TYPE_MOBILE, Email.TYPE_OTHER)

private sealed interface EditorState {
    data object Loading : EditorState
    data class ReadOnly(val sources: List<String>) : EditorState
    data class Ready(val original: Draft, val accounts: List<AccountOption>) : EditorState
}

private sealed interface PhotoEdit {
    data object Unchanged : PhotoEdit
    class Picked(val jpeg: ByteArray, val preview: ImageBitmap) : PhotoEdit
    data object Removed : PhotoEdit
}

private fun accountLabel(account: AccountOption?): String = when {
    account == null || account.type == null -> "This phone only"
    account.type == "com.google" -> "Google · ${account.name}"
    else -> account.name ?: account.type
}

@Composable
fun ContactEditorScreen(request: EditorRequest, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(request) { mutableStateOf<EditorState>(EditorState.Loading) }
    var draft by remember(request) { mutableStateOf<Draft?>(null) }
    var photo by remember(request) { mutableStateOf<PhotoEdit>(PhotoEdit.Unchanged) }
    var saving by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var blocked by remember(request) { mutableStateOf(false) }
    var starred by remember(request) { mutableStateOf(false) }

    fun toggleStar() {
        val id = request.contactId ?: return
        val star = !starred
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { ContactWriter.setStarred(context, id, star) } }
                .onSuccess { starred = star }
                .onFailure { Toast.makeText(context, "Couldn't update favourites: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    LaunchedEffect(request) {
        state = withContext(Dispatchers.IO) {
            if (request.contactId == null) {
                val accounts = ContactWriter.accounts(context)
                EditorState.Ready(ContactWriter.blank(request.number, accounts.firstOrNull()), accounts)
            } else {
                when (val loaded = ContactWriter.load(context, request.contactId)) {
                    is Loaded.Editable -> EditorState.Ready(loaded.draft, emptyList())
                    is Loaded.ReadOnly -> EditorState.ReadOnly(loaded.sources)
                }
            }
        }
        (state as? EditorState.Ready)?.let { ready ->
            draft = ready.original
            blocked = withContext(Dispatchers.IO) {
                ready.original.phones.any { it.value.isNotBlank() && Blocking.isBlocked(context, it.value) }
            }
            request.contactId?.let { id ->
                starred = withContext(Dispatchers.IO) { runCatching { ContactWriter.isStarred(context, id) }.getOrDefault(false) }
            }
        }
    }

    fun toggleBlock() {
        val numbers = (draft ?: return).phones.map { it.value.trim() }.filter { it.isNotEmpty() }
        if (numbers.isEmpty()) return
        val block = !blocked
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { numbers.forEach { if (block) Blocking.block(context, it) else Blocking.unblock(context, it) } }
            }.onSuccess {
                blocked = block
                Toast.makeText(context, if (block) "Blocked" else "Unblocked", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Couldn't update the block list. Is Curlew the default phone app?", Toast.LENGTH_LONG).show()
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { preparePhoto(context, uri) } }
                .onSuccess { (jpeg, preview) -> photo = PhotoEdit.Picked(jpeg, preview) }
                .onFailure { Toast.makeText(context, "Couldn't read that image", Toast.LENGTH_SHORT).show() }
        }
    }

    if (confirmDelete && request.contactId != null) {
        ConfirmDialog(
            title = "Delete contact?",
            text = "It will be removed from this phone and from the account it syncs to.",
            confirm = "Delete",
            onConfirm = {
                scope.launch {
                    withContext(Dispatchers.IO) { runCatching { ContactWriter.delete(context, listOf(request.contactId)) } }
                        .onSuccess { onClose() }
                        .onFailure { Toast.makeText(context, "Couldn't delete: ${it.message}", Toast.LENGTH_LONG).show() }
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }

    val ready = state as? EditorState.Ready
    val current = draft
    val changed = current != null && (current != ready?.original || photo != PhotoEdit.Unchanged)
    val canSave = current != null && !saving && changed &&
        (current.name.isNotBlank() || current.phones.any { it.value.isNotBlank() })

    fun save() {
        val original = ready?.original ?: return
        val edited = current ?: return
        val photoEdit = photo
        saving = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = ContactWriter.save(context, original, edited)
                    if (raw != null) {
                        when (photoEdit) {
                            is PhotoEdit.Picked -> ContactWriter.setPhoto(context, raw, photoEdit.jpeg)
                            PhotoEdit.Removed -> ContactWriter.removePhoto(context, raw)
                            PhotoEdit.Unchanged -> Unit
                        }
                    }
                }
            }
            saving = false
            result.onSuccess { onClose() }.onFailure {
                Toast.makeText(context, "Couldn't save: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.imePadding()) {
            when (val s = state) {
                EditorState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is EditorState.ReadOnly -> Column(Modifier.statusBarsPadding()) {
                    IconButton(onClick = onClose, modifier = Modifier.padding(4.dp)) { Icon(Lucide.ChevronLeft, "Back") }
                    Text(
                        "This contact comes only from ${s.sources.joinToString()}, which doesn't allow edits.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
                is EditorState.Ready -> if (current != null) {
                    val scroll = rememberScrollState()
                    Column(
                        Modifier
                            .fillMaxSize()
                            .scrollbar(scroll)
                            .verticalScroll(scroll),
                    ) {
                        Hero(
                            draft = current,
                            photo = photo,
                            accounts = s.accounts,
                            existing = request.contactId != null,
                            onChange = { draft = it },
                            onBack = onClose,
                            onPickPhoto = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onRemovePhoto = { photo = PhotoEdit.Removed },
                        )
                        FieldCard(
                            icon = Lucide.Phone,
                            title = "Phone",
                            empty = "No phone numbers",
                            fields = current.phones,
                            types = PHONE_TYPES,
                            typeLabel = { Phone.getTypeLabel(context.resources, it, "").toString() },
                            keyboard = KeyboardType.Phone,
                            placeholder = "Phone number",
                            onFields = { draft = current.copy(phones = it) },
                            newField = { Field(null, "", Phone.TYPE_MOBILE) },
                        )
                        FieldCard(
                            icon = Lucide.Mail,
                            title = "Email",
                            empty = "No email addresses",
                            fields = current.emails,
                            types = EMAIL_TYPES,
                            typeLabel = { Email.getTypeLabel(context.resources, it, "").toString() },
                            keyboard = KeyboardType.Email,
                            placeholder = "Email address",
                            onFields = { draft = current.copy(emails = it) },
                            newField = { Field(null, "", Email.TYPE_HOME) },
                        )
                        if (request.contactId != null) {
                            ContactActions(
                                starred = starred,
                                onStar = ::toggleStar,
                                blocked = blocked,
                                onShare = {
                                    val lines = listOf(current.name) + current.phones.filter { it.value.isNotBlank() }.map {
                                        "${Phone.getTypeLabel(context.resources, it.type, it.label.orEmpty())}: ${formatNumber(it.value)}"
                                    }
                                    context.shareText(lines.filter { it.isNotBlank() }.joinToString("\n"))
                                },
                                onBlock = ::toggleBlock,
                                onDelete = { confirmDelete = true },
                            )
                        }
                        Spacer(Modifier.height(120.dp))
                    }
                    SaveButton(enabled = canSave, saving = saving, onClick = ::save, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

@Composable
private fun Hero(
    draft: Draft,
    photo: PhotoEdit,
    accounts: List<AccountOption>,
    existing: Boolean,
    onChange: (Draft) -> Unit,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val hue = avatarHue(draft.name.ifBlank { "?" })
    var photoMenu by remember { mutableStateOf(false) }
    var accountMenu by remember { mutableStateOf(false) }
    var nameFocused by remember { mutableStateOf(false) }
    val underline by animateDpAsState(if (nameFocused) 160.dp else 56.dp, label = "underline")
    val hasPhoto = photo is PhotoEdit.Picked || (photo == PhotoEdit.Unchanged && draft.photo != null)

    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(hue.copy(alpha = 0.35f), Color.Transparent)))
            .statusBarsPadding()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Lucide.ChevronLeft, "Back") }
        }
        Box(Modifier.size(112.dp)) {
            when (photo) {
                is PhotoEdit.Picked -> Image(
                    photo.preview,
                    null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape),
                )
                PhotoEdit.Removed -> Avatar(draft.name.ifBlank { "?" }, size = 112.dp)
                PhotoEdit.Unchanged -> Avatar(draft.name.ifBlank { "?" }, size = 112.dp, photo = draft.photo)
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .border(3.dp, colors.surface, CircleShape)
                    .clickable { if (hasPhoto) photoMenu = true else onPickPhoto() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.Camera, "Photo", tint = colors.onPrimary, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = photoMenu, onDismissRequest = { photoMenu = false }, shape = RoundedCornerShape(16.dp)) {
                DropdownMenuItem(
                    text = { Text("Choose photo") },
                    leadingIcon = { Icon(Lucide.Image, null) },
                    onClick = {
                        photoMenu = false
                        onPickPhoto()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Remove photo") },
                    leadingIcon = { Icon(Lucide.Trash, null) },
                    onClick = {
                        photoMenu = false
                        onRemovePhoto()
                    },
                )
            }
        }
        BasicTextField(
            value = draft.name,
            onValueChange = { onChange(draft.copy(name = it)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (draft.name.isEmpty()) {
                        Text(
                            "Name",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 16.dp)
                .onFocusChanged { nameFocused = it.isFocused },
        )
        Box(
            Modifier
                .padding(top = 6.dp)
                .width(underline)
                .height(2.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = if (nameFocused) 1f else 0.5f)),
        )
        val pickable = !existing && accounts.size > 1
        Box(Modifier.padding(top = 14.dp)) {
            Surface(
                onClick = { if (pickable) accountMenu = true },
                enabled = pickable,
                shape = CircleShape,
                color = colors.surfaceContainer,
                contentColor = colors.onSurfaceVariant,
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Lucide.Cloud, null, Modifier.size(16.dp))
                    Text(accountLabel(draft.account), style = MaterialTheme.typography.labelLarge)
                    if (pickable) Icon(Lucide.ChevronDown, null, Modifier.size(16.dp))
                }
            }
            DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }, shape = RoundedCornerShape(16.dp)) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(accountLabel(account)) },
                        onClick = {
                            accountMenu = false
                            onChange(draft.copy(account = account))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldCard(
    icon: ImageVector,
    title: String,
    empty: String,
    fields: List<Field>,
    types: List<Int>,
    typeLabel: (Int) -> String,
    keyboard: KeyboardType,
    placeholder: String,
    onFields: (List<Field>) -> Unit,
    newField: () -> Field,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth(),
    ) {
        Column(Modifier.padding(bottom = 8.dp)) {
            Row(
                Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                )
                IconButton(onClick = { onFields(fields + newField()) }) {
                    Icon(Lucide.Plus, "Add ${title.lowercase()}", tint = colors.primary)
                }
            }
            if (fields.isEmpty()) {
                Text(
                    empty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 60.dp, bottom = 8.dp),
                )
            }
            fields.forEachIndexed { i, field ->
                FieldRow(
                    field = field,
                    options = (types + field.type).distinct().map {
                        val custom = it == BaseTypes.TYPE_CUSTOM && it == field.type && !field.label.isNullOrBlank()
                        it to if (custom) field.label!! else typeLabel(it)
                    },
                    keyboard = keyboard,
                    placeholder = placeholder,
                    onChange = { f -> onFields(fields.toMutableList().also { it[i] = f }) },
                    onRemove = { onFields(fields.toMutableList().also { it.removeAt(i) }) },
                )
            }
        }
    }
}

@Composable
private fun FieldRow(
    field: Field,
    options: List<Pair<Int, String>>,
    keyboard: KeyboardType,
    placeholder: String,
    onChange: (Field) -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(start = 14.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = field.value,
            onValueChange = { onChange(field.copy(value = it)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            decorationBox = { inner ->
                Box {
                    if (field.value.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    inner()
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 14.dp),
        )
        Box {
            Surface(onClick = { menu = true }, shape = CircleShape, color = colors.surfaceContainerHigh) {
                Row(Modifier.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(options.first { it.first == field.type }.second, style = MaterialTheme.typography.labelMedium)
                    Icon(Lucide.ChevronDown, null, Modifier.size(16.dp))
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                options.forEach { (type, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        menu = false
                        onChange(field.copy(type = type))
                    })
                }
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Lucide.X, "Remove", tint = colors.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ContactActions(
    starred: Boolean,
    onStar: () -> Unit,
    blocked: Boolean,
    onShare: () -> Unit,
    onBlock: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth(),
    ) {
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ContactAction(
                if (starred) Lucide.StarFilled else Lucide.Star,
                "Favourite",
                if (starred) Nord.yellow else MaterialTheme.colorScheme.primary,
                onStar,
            )
            ContactAction(Lucide.Share, "Share", MaterialTheme.colorScheme.primary, onShare)
            ContactAction(Lucide.Ban, if (blocked) "Unblock" else "Block", if (blocked) Nord.red else MaterialTheme.colorScheme.primary, onBlock)
            ContactAction(Lucide.Trash, "Delete", Nord.red, onDelete)
        }
    }
}

@Composable
private fun ContactAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun SaveButton(enabled: Boolean, saving: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled || saving) colors.primary else colors.surfaceContainerHighest,
        contentColor = if (enabled || saving) colors.onPrimary else colors.onSurfaceVariant,
        shadowElevation = 8.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(16.dp)
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (saving) {
                CircularProgressIndicator(Modifier.size(20.dp), color = colors.onPrimary, strokeWidth = 2.dp)
            } else {
                Icon(Lucide.Check, null)
            }
            Text("Save contact", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun preparePhoto(context: Context, uri: Uri): Pair<ByteArray, ImageBitmap> {
    val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
        val longest = maxOf(info.size.width, info.size.height)
        if (longest > 1440) {
            val scale = 1440f / longest
            decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    val side = minOf(decoded.width, decoded.height)
    val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
    val scaled = if (side > 720) square.scale(720, 720) else square
    val jpeg = ByteArrayOutputStream().use {
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, it)
        it.toByteArray()
    }
    return jpeg to scaled.asImageBitmap()
}
