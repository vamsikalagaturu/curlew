package io.github.vamsikalagaturu.curlew.ui

import android.content.Intent
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.vamsikalagaturu.curlew.Prefs
import io.github.vamsikalagaturu.curlew.Blocking
import io.github.vamsikalagaturu.curlew.Contact
import io.github.vamsikalagaturu.curlew.Phonebook
import io.github.vamsikalagaturu.curlew.UnknownCallers
import io.github.vamsikalagaturu.curlew.numberKey
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import io.github.vamsikalagaturu.curlew.ContactWriter
import io.github.vamsikalagaturu.curlew.R
import io.github.vamsikalagaturu.curlew.callAccounts
import io.github.vamsikalagaturu.curlew.formatNumber
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.vamsikalagaturu.curlew.ThemeMode
import io.github.vamsikalagaturu.curlew.UpdateStatus
import io.github.vamsikalagaturu.curlew.Updater
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri

private enum class Page { Blocked, SpeedDial }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedDialScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs.get(context) }
    val slots by prefs.speedDial.collectAsState()
    val contacts by produceState<List<Contact>?>(null) {
        value = withContext(Dispatchers.IO) { Phonebook.contacts(context) }
    }
    var picking by remember { mutableStateOf<Int?>(null) }
    val byNumber = remember(contacts) { contacts.orEmpty().associateBy { numberKey(it.number) } }

    picking?.let { digit ->
        BackHandler { picking = null }
        ContactPickerScreen(digit, contacts, onPick = {
            prefs.setSpeedDial(digit, it.number)
            picking = null
        }, onClose = { picking = null })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speed dial") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Lucide.ChevronLeft, "Back") } },
            )
        },
    ) { padding ->
        val scroll = rememberScrollState()
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .scrollbar(scroll)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Long-press a key on the keypad to call its contact.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    (2..9).forEach { digit ->
                        val number = slots[digit]
                        val contact = number?.let { byNumber[numberKey(it)] }
                        ListItem(
                            modifier = Modifier.clickable { picking = digit },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(digit.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            headlineContent = { Text(contact?.name ?: number?.let(::formatNumber) ?: "Not set") },
                            supportingContent = if (contact != null) {
                                { Text(formatNumber(contact.number)) }
                            } else {
                                null
                            },
                            trailingContent = if (number != null) {
                                { IconButton(onClick = { prefs.setSpeedDial(digit, null) }) { Icon(Lucide.X, "Remove") } }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactPickerScreen(digit: Int, contacts: List<Contact>?, onPick: (Contact) -> Unit, onClose: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val all = contacts.orEmpty()
    val shown = remember(all, query) {
        val q = query.trim()
        val digits = q.filter(Char::isDigit)
        if (q.isEmpty()) {
            all
        } else {
            all.filter { it.name.contains(q, ignoreCase = true) || (digits.isNotEmpty() && it.digits.contains(digits)) }
        }
    }
    val favourites = remember(all, query) { if (query.isBlank()) all.filter { it.starred } else emptyList() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Speed dial $digit")
                        Text(
                            "Choose who long-pressing $digit calls",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Lucide.ChevronLeft, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search contacts") },
                leadingIcon = { Icon(Lucide.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            when {
                contacts == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matches", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    val list = rememberLazyListState()
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .scrollbar(list),
                        state = list,
                    ) {
                        if (favourites.isNotEmpty()) {
                            item(key = "h:fav") { PickerHeader("Favourites") }
                            items(favourites, key = { "f:${it.id}:${it.digits}" }) { PickerRow(it, onPick) }
                            item(key = "h:all") { PickerHeader("All contacts") }
                        }
                        items(shown, key = { "${it.id}:${it.digits}" }) { PickerRow(it, onPick) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun PickerRow(contact: Contact, onPick: (Contact) -> Unit) {
    ListItem(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPick(contact) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Avatar(contact.name, photo = contact.photo) },
        headlineContent = { Text(contact.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("${contact.label} · ${formatNumber(contact.number)}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockedScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var input by rememberSaveable { mutableStateOf("") }
    val numbers by produceState<List<String>?>(null, refresh) {
        value = withContext(Dispatchers.IO) { Blocking.list(context) }
    }
    fun change(action: () -> Unit) {
        scope.launch {
            withContext(Dispatchers.IO) { runCatching(action) }.onFailure {
                Toast.makeText(context, "Couldn't update the block list: ${it.message}", Toast.LENGTH_LONG).show()
            }
            refresh++
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked numbers") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Lucide.ChevronLeft, "Back") } },
            )
        },
    ) { padding ->
        val scroll = rememberScrollState()
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .scrollbar(scroll)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!Blocking.available(context)) {
                Text(
                    "Make Curlew your default phone app to manage blocked numbers.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Add a number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    enabled = input.isNotBlank(),
                    onClick = {
                        val number = input.trim()
                        input = ""
                        change { Blocking.block(context, number) }
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("Block") }
            }
            val list = numbers
            when {
                list == null -> Unit
                list.isEmpty() -> Text("Nobody is blocked.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        list.forEach { number ->
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = { SettingIcon(Lucide.Ban) },
                                headlineContent = { Text(formatNumber(number)) },
                                trailingContent = {
                                    TextButton(onClick = { change { Blocking.unblock(context, number) } }) { Text("Unblock") }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs.get(context) }
    val theme by prefs.theme.collectAsState()
    val amoled by prefs.amoled.collectAsState()
    val defaultTab by prefs.defaultTab.collectAsState()
    val haptics by prefs.haptics.collectAsState()
    val tones by prefs.tones.collectAsState()
    val autoUpdate by prefs.autoUpdate.collectAsState()
    val update by Updater.status.collectAsState()

    val callAccount by prefs.callAccount.collectAsState()
    val speedDial by prefs.speedDial.collectAsState()
    val unknownCallers by prefs.unknownCallers.collectAsState()
    val blockPrivate by prefs.blockPrivate.collectAsState()
    val accounts = remember { callAccounts(context) }
    var page by rememberSaveable { mutableStateOf<Page?>(null) }
    val scope = rememberCoroutineScope()
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/x-vcard")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { ContactWriter.exportVcf(context, null, uri) } }
            Toast.makeText(context, result.fold({ "Exported $it contacts" }, { "Export failed: ${it.message}" }), Toast.LENGTH_LONG).show()
        }
    }

    fun open(intent: Intent) {
        runCatching { context.startActivity(intent) }.onFailure {
            Toast.makeText(context, "This phone doesn't offer that screen", Toast.LENGTH_SHORT).show()
        }
    }

    if (page == Page.Blocked) {
        BackHandler { page = null }
        BlockedScreen(onClose = { page = null })
        return
    }
    if (page == Page.SpeedDial) {
        BackHandler { page = null }
        SpeedDialScreen(onClose = { page = null })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Lucide.ChevronLeft, "Back") } },
            )
        },
    ) { padding ->
        val scroll = rememberScrollState()
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .scrollbar(scroll)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Group("Appearance") {
                ChoiceRow(Lucide.Contrast, "Theme") {
                    Choice(ThemeMode.entries.map { it.name }, theme.name) { prefs.setTheme(ThemeMode.valueOf(it)) }
                }
                Toggle(Lucide.Moon, "Pure black", "Black background in dark theme, for OLED screens", amoled, prefs::setAmoled)
            }
            Group("Start") {
                ChoiceRow(Lucide.AppWindow, "Open on") {
                    Choice(Tab.entries.map { it.name }, defaultTab) { prefs.setDefaultTab(it) }
                }
            }
            Group("Keypad") {
                Toggle(Lucide.Vibrate, "Vibrate on key press", null, haptics, prefs::setHaptics)
                Toggle(Lucide.Keypad, "Key tones", "Play a tone while dialling; muted when the phone is silent", tones, prefs::setTones)
            }
            Group("Phone") {
                Link(Lucide.Phone, "Default phone app", "Choose which app handles calls") {
                    open(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                }
                Link(Lucide.Music, "Ringtone and sounds", "System sound settings") {
                    open(Intent(Settings.ACTION_SOUND_SETTINGS))
                }
                if (accounts.size > 1) {
                    ChoiceRow(Lucide.CardSim, "Call with") {
                        val keys = listOf("") + accounts.map { it.key }
                        val labels = listOf("Phone default") + accounts.map { it.label }
                        RadioList(labels, keys.indexOf(callAccount).coerceAtLeast(0)) { prefs.setCallAccount(keys[it]) }
                    }
                }
                Link(Lucide.PhoneForwarded, "Carrier call settings", "Forwarding, call waiting, caller ID") {
                    open(Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS))
                }
                Link(Lucide.Keypad, "Speed dial", "${speedDial.size} of 8 keys set") { page = Page.SpeedDial }
                Link(Lucide.Ban, "Blocked numbers", "Numbers that can't call or text you") { page = Page.Blocked }
            }
            Group("Calls from strangers") {
                ChoiceRow(Lucide.UserX, "Numbers not in your contacts") {
                    Choice(UnknownCallers.entries.map { it.name }, unknownCallers.name) { prefs.setUnknownCallers(UnknownCallers.valueOf(it)) }
                }
                Toggle(Lucide.BellOff, "Block private numbers", "Calls that hide their caller ID", blockPrivate, prefs::setBlockPrivate)
            }
            Group("Contacts") {
                Link(Lucide.Download, "Export all contacts", "Save every contact as a .vcf file") {
                    exporter.launch("contacts.vcf")
                }
            }
            Group("Updates") {
                Toggle(Lucide.RefreshCw, "Check automatically", "Once a day, from GitHub releases", autoUpdate, prefs::setAutoUpdate)
                val (subtitle, action) = when (val s = update) {
                    UpdateStatus.Idle -> "Tap to check now" to null
                    UpdateStatus.Checking -> "Checking…" to null
                    UpdateStatus.UpToDate -> "You're on the latest version" to null
                    UpdateStatus.NoReleases -> "No releases published yet" to null
                    is UpdateStatus.Available -> "${s.release.tag} is available" to "Install"
                    is UpdateStatus.Downloading -> "Downloading… ${(s.progress * 100).toInt()}%" to null
                    UpdateStatus.Installing -> "Installing…" to null
                    is UpdateStatus.Failed -> s.message to "Retry"
                }
                ListItem(
                    modifier = Modifier.clickable {
                        val s = update
                        if (s is UpdateStatus.Available) Updater.install(context, s.release) else Updater.check(context)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { SettingIcon(Lucide.Download) },
                    headlineContent = { Text("Check for updates") },
                    supportingContent = { Text(subtitle) },
                    trailingContent = when (val s = update) {
                        UpdateStatus.Checking, UpdateStatus.Installing -> {
                            { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp) }
                        }
                        is UpdateStatus.Downloading -> {
                            { CircularProgressIndicator(progress = { s.progress }, modifier = Modifier.size(24.dp), strokeWidth = 3.dp) }
                        }
                        else -> action?.let { label -> { FilledTonalButton(onClick = {
                            if (s is UpdateStatus.Available) Updater.install(context, s.release) else Updater.check(context)
                        }) { Text(label) } } }
                    },
                )
            }
            Group("About") {
                AboutCard(Updater.currentVersion(context))
                Link(Lucide.Info, "App info", "Permissions, storage and notifications") {
                    open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                }
                Link(Lucide.Code, "Source and releases", "github.com/${Updater.REPO}") {
                    open(Intent(Intent.ACTION_VIEW, Updater.PAGE.toUri()))
                }
            }
        }
    }
}

@Composable
private fun AboutCard(version: String) {
    val context = LocalContext.current
    val code = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode }.getOrNull()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Nord.night0),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(96.dp))
        }
        Column(Modifier.padding(start = 16.dp)) {
            Text("Curlew", style = MaterialTheme.typography.titleLarge)
            Text(
                "Version $version${code?.let { " ($it)" }.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                context.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ChoiceRow(icon: ImageVector, title: String, control: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingIcon(icon)
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
        }
        Box(Modifier.padding(top = 12.dp)) { control() }
    }
}

@Composable
private fun Toggle(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { SettingIcon(icon) },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { CurlewSwitch(checked, onChange) },
    )
}

@Composable
private fun Link(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { SettingIcon(icon) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
    )
}

@Composable
private fun RadioList(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Column {
        options.forEachIndexed { i, option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPick(i) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = i == selected, onClick = { onPick(i) })
                Text(option, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(options: List<String>, selected: String, onPick: (String) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onPick(option) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(option) }
        }
    }
}
