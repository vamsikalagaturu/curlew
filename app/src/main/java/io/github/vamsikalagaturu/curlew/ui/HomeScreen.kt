package io.github.vamsikalagaturu.curlew.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.CallLog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import io.github.vamsikalagaturu.curlew.Blocking
import io.github.vamsikalagaturu.curlew.copyNumber
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import io.github.vamsikalagaturu.curlew.callAccounts
import io.github.vamsikalagaturu.curlew.dial
import io.github.vamsikalagaturu.curlew.handleTypedCode
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CoroutineScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.coroutineScope
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.vamsikalagaturu.curlew.Prefs
import io.github.vamsikalagaturu.curlew.UpdateStatus
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.rememberCoroutineScope
import io.github.vamsikalagaturu.curlew.ContactWriter
import io.github.vamsikalagaturu.curlew.Phonebook
import io.github.vamsikalagaturu.curlew.shareText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vamsikalagaturu.curlew.Contact
import io.github.vamsikalagaturu.curlew.RecentCall
import io.github.vamsikalagaturu.curlew.RecentGroup
import io.github.vamsikalagaturu.curlew.formatNumber
import io.github.vamsikalagaturu.curlew.numberKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class Tab(val label: String, val icon: ImageVector) {
    Keypad("Keypad", Lucide.Keypad),
    Recents("Recents", Lucide.History),
    Contacts("Contacts", Lucide.BookUser),
}

@Composable
fun HomeScreen(
    number: String,
    onNumberChange: (String) -> Unit,
    tab: Tab,
    onTabChange: (Tab) -> Unit,
    contacts: List<Contact>?,
    recents: List<RecentGroup>?,
    blocked: Map<String, String>,
    isDefaultDialer: Boolean,
    onRequestDefault: () -> Unit,
    onCall: (String) -> Unit,
    onVoicemail: () -> Unit,
    onEdit: (EditorRequest) -> Unit,
    onSettings: () -> Unit,
    update: UpdateStatus,
    onUpdate: () -> Unit,
    onRecentsSeen: () -> Unit,
) {
    val pick: (String) -> Unit = {
        onNumberChange(it)
        onTabChange(Tab.Keypad)
    }
    val recentsState = rememberLazyListState()
    val contactsState = rememberLazyListState()
    val pager = rememberPagerState(initialPage = tab.ordinal) { Tab.entries.size }
    val latestTab by rememberUpdatedState(tab)
    val highlight = remember { Animatable(tab.ordinal.toFloat()) }
    val contentAlpha = remember { Animatable(1f) }
    val contentScale = remember { Animatable(1f) }
    var jumping by remember { mutableStateOf(false) }
    // A tab change from outside the pager (nav tap, intent) is a Material "fade through" rather than a slide,
    // so a jump never drags the pages in between across the screen, and the highlight goes straight to its target.
    LaunchedEffect(tab) {
        if (tab == Tab.Recents) onRecentsSeen()
        val target = tab.ordinal
        val settled = pager.currentPage == target && highlight.value == target.toFloat() &&
            contentAlpha.value == 1f && contentScale.value == 1f
        if (settled) return@LaunchedEffect
        jumping = true
        try {
            coroutineScope {
                launch { highlight.animateTo(target.toFloat(), tween(300)) }
                if (pager.currentPage != target) {
                    contentAlpha.animateTo(0f, tween(90, easing = FastOutLinearInEasing))
                    pager.scrollToPage(target)
                    contentScale.snapTo(0.92f)
                }
                launch { contentScale.animateTo(1f, tween(210, easing = LinearOutSlowInEasing)) }
                contentAlpha.animateTo(1f, tween(210, easing = LinearOutSlowInEasing))
            }
        } finally {
            jumping = false
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage + pager.currentPageOffsetFraction }.collect { if (!jumping) highlight.snapTo(it) }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            if (page != latestTab.ordinal) onTabChange(Tab.entries[page])
        }
    }
    Scaffold(
        bottomBar = { FloatingNav({ highlight.value }, onTabChange, onSettings) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            if (!isDefaultDialer) {
                Banner("Curlew isn't your phone app yet.", "Set as default", onRequestDefault)
            } else if (update is UpdateStatus.Available) {
                Banner("Curlew ${update.release.tag} is available.", "Update", onUpdate)
            }
            HorizontalPager(
                pager,
                Modifier
                    .weight(1f)
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        scaleX = contentScale.value
                        scaleY = contentScale.value
                    },
                key = { it },
            ) { page ->
                when (Tab.entries[page]) {
                    Tab.Keypad -> KeypadTab(number, contacts.orEmpty(), recents.orEmpty(), onNumberChange, onCall, onVoicemail, onEdit)
                    Tab.Recents -> RecentsTab(recents, blocked, recentsState, onCall, pick, onEdit)
                    Tab.Contacts -> ContactsTab(contacts, contactsState, onCall, onEdit)
                }
            }
        }
    }
}

@Composable
private fun FloatingNav(position: () -> Float, onTabChange: (Tab) -> Unit, onSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surfaceContainerHigh,
            shadowElevation = 10.dp,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
        ) {
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // The highlight follows the pager's live scroll position, so it moves with the finger mid-swipe.
                val at = position()
                Tab.entries.forEach { item ->
                    val closeness = (1f - abs(at - item.ordinal)).coerceIn(0f, 1f)
                    val active = closeness > 0.5f
                    val background = colors.primary.copy(alpha = closeness)
                    val foreground = lerp(colors.onSurfaceVariant, colors.onPrimary, closeness)
                    Row(
                        Modifier
                            .weight(1f + 1.4f * closeness)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(background)
                            .clickable { onTabChange(item) }
                            .semantics { contentDescription = item.label },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(item.icon, null, tint = foreground)
                        AnimatedVisibility(
                            visible = active,
                            enter = expandHorizontally() + fadeIn(),
                            exit = shrinkHorizontally() + fadeOut(),
                        ) {
                            Text(
                                item.label,
                                color = foreground,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        Surface(
            onClick = onSettings,
            shape = CircleShape,
            color = colors.surfaceContainerHigh,
            shadowElevation = 10.dp,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Lucide.Settings, "Settings", tint = colors.onSurfaceVariant) }
        }
    }
}

@Composable
private fun Banner(message: String, action: String, onAction: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

// Shows the number formatted (e.g. "+49 1512 4866038") while the cursor still maps onto the raw digits.
private object PhoneFormatting : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val shown = formatNumber(raw)
        val toShown = IntArray(raw.length + 1)
        var j = 0
        for (i in raw.indices) {
            while (j < shown.length && shown[j] != raw[i]) j++
            if (j == shown.length) return TransformedText(text, OffsetMapping.Identity)
            toShown[i] = j++
        }
        toShown[raw.length] = shown.length
        val toRaw = IntArray(shown.length + 1)
        var i = 0
        for (k in 0..shown.length) {
            while (i < raw.length && toShown[i] < k) i++
            toRaw[k] = i
        }
        return TransformedText(
            AnnotatedString(shown),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = toShown[offset.coerceIn(0, raw.length)]
                override fun transformedToOriginal(offset: Int) = toRaw[offset.coerceIn(0, shown.length)]
            },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NumberField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val size = when (value.text.length) {
        in 0..11 -> 36.sp
        in 12..14 -> 30.sp
        in 15..17 -> 26.sp
        else -> 22.sp
    }
    // The dialpad is the only input: swallowing the platform text-input session keeps the soft keyboard
    // away while the field keeps its cursor, selection handles and the system Paste/Copy toolbar.
    InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }) {
        BasicTextField(
            value = value,
            onValueChange = { new ->
                val clean = new.text.filter { it.isDigit() || it in "+*#,;" }
                onValueChange(if (clean == new.text) new else TextFieldValue(clean, TextRange(minOf(new.selection.end, clean.length))))
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.displaySmall.copy(
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                fontSize = size,
            ),
            cursorBrush = SolidColor(colors.primary),
            visualTransformation = PhoneFormatting,
            modifier = modifier.focusRequester(focus),
        )
    }
}

private fun dtmfTone(key: Char) = when (key) {
    in '0'..'9' -> ToneGenerator.TONE_DTMF_0 + (key - '0')
    '*' -> ToneGenerator.TONE_DTMF_S
    else -> ToneGenerator.TONE_DTMF_P
}

private data class Suggestion(val name: String?, val number: String, val photo: String? = null)

private fun suggest(number: String, contacts: List<Contact>, recents: List<RecentGroup>): List<Suggestion> {
    val digits = number.filter { it.isDigit() }
    if (digits.length < 2) return emptyList()
    val fromContacts = contacts.asSequence()
        .filter { it.digits.contains(digits) }
        .map { Suggestion(it.name, it.number, it.photo) }
    val fromRecents = recents.asSequence()
        .filter { it.contactId == null && it.number.filter(Char::isDigit).contains(digits) }
        .map { Suggestion(it.name, it.number) }
    return (fromContacts + fromRecents)
        .distinctBy { numberKey(it.number) }
        .sortedBy { if (it.number.filter(Char::isDigit).startsWith(digits)) 0 else 1 }
        .take(4)
        .toList()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadTab(
    number: String,
    contacts: List<Contact>,
    recents: List<RecentGroup>,
    onNumberChange: (String) -> Unit,
    onCall: (String) -> Unit,
    onVoicemail: () -> Unit,
    onEdit: (EditorRequest) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { Prefs.get(context) }
    val haptics by prefs.haptics.collectAsState()
    val tones by prefs.tones.collectAsState()
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    val toneGenerator = remember { runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 70) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { toneGenerator?.release() } }
    var field by remember { mutableStateOf(TextFieldValue(number, TextRange(number.length))) }
    LaunchedEffect(number) {
        if (field.text != number) field = TextFieldValue(number, TextRange(number.length))
    }
    val speedDial by prefs.speedDial.collectAsState()
    val accounts = remember { callAccounts(context) }
    var simMenu by remember { mutableStateOf(false) }
    fun commit(value: TextFieldValue) {
        if (context.handleTypedCode(value.text)) {
            field = TextFieldValue("")
            onNumberChange("")
            return
        }
        field = value
        if (value.text != number) onNumberChange(value.text)
    }
    fun insert(text: String) {
        val at = field.selection
        commit(TextFieldValue(field.text.replaceRange(at.min, at.max, text), TextRange(at.min + text.length)))
    }
    fun backspace() {
        val at = field.selection
        when {
            !at.collapsed -> commit(TextFieldValue(field.text.removeRange(at.min, at.max), TextRange(at.min)))
            at.start > 0 -> commit(TextFieldValue(field.text.removeRange(at.start - 1, at.start), TextRange(at.start - 1)))
        }
    }
    val suggestions = remember(number, contacts, recents) { suggest(number, contacts, recents) }
    val known = remember(number, contacts) {
        val key = numberKey(number)
        key.isNotEmpty() && contacts.any { numberKey(it.number) == key }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
        ) {
            suggestions.forEach { s ->
                ListItem(
                    modifier = Modifier.clickable { onNumberChange(s.number) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Avatar(s.name ?: s.number, size = 36.dp, photo = s.photo) },
                    headlineContent = { Text(s.name ?: formatNumber(s.number), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = if (s.name != null) {
                        { Text(formatNumber(s.number), maxLines = 1) }
                    } else {
                        null
                    },
                    trailingContent = {
                        IconButton(onClick = { onCall(s.number) }) { Icon(Lucide.Phone, "Call") }
                    },
                )
            }
            if (number.count { it.isDigit() } >= 3 && !known) {
                TextButton(onClick = { onEdit(EditorRequest(null, number)) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Lucide.UserPlus, null)
                    Text("Create new contact", Modifier.padding(start = 8.dp))
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(48.dp))
            NumberField(field, ::commit, Modifier.weight(1f))
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (number.isNotEmpty()) {
                    Icon(
                        Lucide.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .combinedClickable(
                                onClick = ::backspace,
                                onLongClick = { commit(TextFieldValue("")) },
                            )
                            .padding(12.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Dialpad(
            onKeyDown = {
                insert(it.toString())
                if (tones && audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) toneGenerator?.startTone(dtmfTone(it), 120)
            },
            haptics = haptics,
            onLongPress = {
                val at = field.selection.min
                when {
                    it == '0' && at > 0 && field.text[at - 1] == '0' ->
                        commit(TextFieldValue(field.text.replaceRange(at - 1, at, "+"), TextRange(at)))
                    it == '1' && number == "1" -> {
                        commit(TextFieldValue(""))
                        onVoicemail()
                    }
                    it in '2'..'9' && number == it.toString() -> {
                        val target = speedDial[it - '0']
                        if (target != null) {
                            commit(TextFieldValue(""))
                            onCall(target)
                        } else {
                            Toast.makeText(context, "No speed dial on $it. Set one in Settings → Speed dial", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
        )
        Spacer(Modifier.height(24.dp))
        Box {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Nord.green)
                    .combinedClickable(
                        onClick = { if (number.isNotBlank()) onCall(number) },
                        onLongClick = { if (number.isNotBlank() && accounts.size > 1) simMenu = true },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.Phone, "Call", Modifier.size(32.dp), tint = Nord.night0)
            }
            DropdownMenu(expanded = simMenu, onDismissRequest = { simMenu = false }, shape = RoundedCornerShape(16.dp)) {
                Text(
                    "Call with",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.display) },
                        leadingIcon = { Icon(Lucide.CardSim, null) },
                        onClick = {
                            simMenu = false
                            context.dial(number, account.handle)
                            commit(TextFieldValue(""))
                        },
                    )
                }
            }
        }
    }
}

private sealed interface RecentItem {
    val key: String

    data class Header(val label: String) : RecentItem {
        override val key get() = "h:$label"
    }

    data class Group(val group: RecentGroup, val expanded: Boolean) : RecentItem {
        override val key get() = "g:${group.key}"
    }

    data class Actions(val group: RecentGroup) : RecentItem {
        override val key get() = "a:${group.key}"
    }

    data class Day(val groupKey: String, val label: String) : RecentItem {
        override val key get() = "day:$groupKey:$label"
    }

    data class Detail(val call: RecentCall, val showNumber: Boolean) : RecentItem {
        override val key get() = "d:${call.id}"
    }

    data class Footer(val groupKey: String, val remaining: Int) : RecentItem {
        override val key get() = "f:$groupKey"
    }
}

private const val PAGE = 10

// Consecutive lazy items share one tinted card: only the first and last get rounded corners.
private fun Modifier.segment(active: Boolean, color: Color, top: Boolean = false, bottom: Boolean = false): Modifier {
    val base = fillMaxWidth().padding(horizontal = 8.dp)
    if (!active) return base
    val t = if (top) 20.dp else 0.dp
    val b = if (bottom) 20.dp else 0.dp
    return base.clip(RoundedCornerShape(t, t, b, b)).background(color)
}

private fun dayLabel(millis: Long, today: LocalDate): String {
    val day = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return when {
        day == today -> "Today"
        day == today.minusDays(1) -> "Yesterday"
        day.isAfter(today.minusDays(7)) -> day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        day.year == today.year -> day.format(DateTimeFormatter.ofPattern("d MMMM"))
        else -> day.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    }
}

@Composable
private fun RecentsTab(
    recents: List<RecentGroup>?,
    blocked: Map<String, String>,
    state: LazyListState,
    onCall: (String) -> Unit,
    onPick: (String) -> Unit,
    onEdit: (EditorRequest) -> Unit,
) {
    val all = recents ?: return
    if (all.isEmpty()) return EmptyState("No recent calls")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var missedOnly by rememberSaveable { mutableStateOf(false) }
    val sims = remember { callAccounts(context).takeIf { it.size > 1 }?.associate { it.key to it.label }.orEmpty() }
    val groups = remember(all, query, missedOnly) {
        val q = query.trim()
        val digits = q.filter { it.isDigit() }
        all.mapNotNull { g ->
            val calls = if (missedOnly) g.calls.filter { isMissed(it.type) } else g.calls
            if (calls.isEmpty()) null else if (calls.size == g.calls.size) g else g.copy(calls = calls)
        }.filter { g ->
            q.isEmpty() || g.label.contains(q, ignoreCase = true) ||
                (digits.isNotEmpty() && g.number.filter(Char::isDigit).contains(digits))
        }
    }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var confirm by remember { mutableStateOf(false) }
    BackHandler(selected.isNotEmpty()) { selected = emptySet() }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var shown by rememberSaveable(expanded) { mutableIntStateOf(PAGE) }
    val items = remember(groups, expanded, shown) {
        val today = LocalDate.now()
        buildList {
            var lastHeader: String? = null
            groups.forEach { g ->
                val header = dayLabel(g.latest.date, today)
                if (header != lastHeader) {
                    add(RecentItem.Header(header))
                    lastHeader = header
                }
                val open = g.key == expanded
                add(RecentItem.Group(g, open))
                if (open) {
                    add(RecentItem.Actions(g))
                    val multipleNumbers = g.calls.map { numberKey(it.number) }.distinct().size > 1
                    var lastDay: String? = null
                    g.calls.take(shown).forEach { call ->
                        val day = dayLabel(call.date, today)
                        if (day != lastDay) {
                            add(RecentItem.Day(g.key, day))
                            lastDay = day
                        }
                        add(RecentItem.Detail(call, multipleNumbers))
                    }
                    add(RecentItem.Footer(g.key, (g.calls.size - shown).coerceAtLeast(0)))
                }
            }
        }
    }
    val card = MaterialTheme.colorScheme.surfaceContainer
    val selectedColor = MaterialTheme.colorScheme.secondaryContainer
    fun toggle(key: String) {
        selected = if (key in selected) selected - key else selected + key
    }

    if (confirm) {
        val chosen = groups.filter { it.key in selected }
        val calls = chosen.sumOf { it.calls.size }
        ConfirmDialog(
            title = "Delete call history?",
            text = "$calls ${if (calls == 1) "call" else "calls"} with ${chosen.size} " +
                "${if (chosen.size == 1) "contact" else "contacts"} will be removed from this phone.",
            confirm = "Delete",
            onConfirm = {
                val ids = chosen.flatMap { g -> g.calls.map { it.id } }
                selected = emptySet()
                scope.launch {
                    withContext(Dispatchers.IO) { runCatching { Phonebook.deleteCalls(context, ids) } }.onFailure {
                        Toast.makeText(context, "Couldn't delete: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { confirm = false },
        )
    }

    Column(Modifier.fillMaxSize()) {
        RecentsSearch(query, { query = it }, missedOnly, { missedOnly = !missedOnly })
        Box(
            Modifier
                .weight(1f)
                .imePadding(),
        ) {
            if (groups.isEmpty()) EmptyState(if (missedOnly) "No missed calls" else "No matches")
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .scrollbar(state),
                state = state,
                contentPadding = PaddingValues(bottom = if (selected.isNotEmpty()) SelectionBarSpace else 0.dp),
            ) {
                items(items, key = { it.key }, contentType = { it::class }) { item ->
                    val base = Modifier.animateItem()
                    when (item) {
                        is RecentItem.Header -> Text(
                            item.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = base.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                        )
                        is RecentItem.Group -> {
                            val key = item.group.key
                            val isSelected = key in selected
                            GroupRow(
                                group = item.group,
                                selected = isSelected,
                                blocked = numberKey(item.group.number) in blocked,
                                sim = sims[item.group.latest.account],
                                onCall = onCall,
                                modifier = base.segment(
                                    active = item.expanded || isSelected,
                                    color = if (isSelected) selectedColor else card,
                                    top = true,
                                    bottom = !item.expanded,
                                ),
                                onClick = {
                                    if (selected.isNotEmpty()) toggle(key) else expanded = if (item.expanded) null else key
                                },
                                onLongClick = {
                                    expanded = null
                                    toggle(key)
                                },
                            )
                        }
                        is RecentItem.Actions -> ActionsRow(
                            item.group,
                            blocked[numberKey(item.group.number)],
                            onPick,
                            onEdit,
                            base.segment(true, card),
                        )
                        is RecentItem.Day -> Text(
                            item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = base
                                .segment(true, card)
                                .padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                        is RecentItem.Detail -> DetailRow(item.call, item.showNumber, sims[item.call.account], base.segment(true, card))
                        is RecentItem.Footer -> Box(
                            base
                                .segment(true, card, bottom = true)
                                .padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
                        ) {
                            if (item.remaining > 0) {
                                TextButton(onClick = { shown += PAGE }) {
                                    Text("Show ${minOf(PAGE, item.remaining)} more · ${item.remaining} left")
                                }
                            }
                        }
                    }
                }
            }
            SelectionBar(selected.size, onClear = { selected = emptySet() }) {
                IconButton(onClick = { confirm = true }) { Icon(Lucide.Trash, "Delete") }
            }
        }
    }
}

@Composable
private fun RecentsSearch(query: String, onQuery: (String) -> Unit, missedOnly: Boolean, onToggleMissed: () -> Unit) {
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text("Search calls") },
            leadingIcon = { Icon(Lucide.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            selected = missedOnly,
            onClick = onToggleMissed,
            label = { Text("Missed") },
            leadingIcon = { Icon(Lucide.PhoneMissed, null, Modifier.size(16.dp)) },
            shape = CircleShape,
            modifier = Modifier.height(56.dp),
        )
    }
}

@Composable
private fun ActionsRow(
    group: RecentGroup,
    blockedAs: String?,
    onPick: (String) -> Unit,
    onEdit: (EditorRequest) -> Unit,
    modifier: Modifier,
) {
    if (group.number.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(
        modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CardAction(Lucide.Keypad, "Edit number") { onPick(group.number) }
        if (group.contactId != null) {
            CardAction(Lucide.Pencil, "Contact") { onEdit(EditorRequest(group.contactId)) }
        } else {
            CardAction(Lucide.UserPlus, "Add contact") { onEdit(EditorRequest(null, group.number)) }
        }
        CardAction(Lucide.Share, "Share") {
            context.shareText(listOfNotNull(group.name, formatNumber(group.number)).joinToString("\n"))
        }
        if (blockedAs != null) {
            CardAction(Lucide.Ban, "Unblock") { blockNumbers(context, scope, listOf(blockedAs), block = false) }
        } else {
            CardAction(Lucide.Ban, "Block") { blockNumbers(context, scope, listOf(group.number)) }
        }
    }
}

@Composable
private fun CardAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
    }
}

private fun typeIcon(type: Int): Pair<ImageVector, Color?> = when (type) {
    CallLog.Calls.INCOMING_TYPE, CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> Lucide.ArrowDownLeft to null
    CallLog.Calls.OUTGOING_TYPE -> Lucide.ArrowUpRight to Nord.green
    CallLog.Calls.BLOCKED_TYPE -> Lucide.Ban to null
    else -> Lucide.PhoneMissed to Nord.red
}

private fun isMissed(type: Int) = type == CallLog.Calls.MISSED_TYPE || type == CallLog.Calls.REJECTED_TYPE

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupRow(
    group: RecentGroup,
    selected: Boolean,
    blocked: Boolean,
    sim: String?,
    onCall: (String) -> Unit,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val latest = group.latest
    val time = remember(latest.date) {
        DateUtils.getRelativeTimeSpanString(latest.date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }
    val copy = Modifier.combinedClickable(onClick = onClick, onLongClick = { context.copyNumber(group.number) })
    ListItem(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Avatar(group.label, selected = selected, photo = group.photo) },
        headlineContent = {
            Text(
                group.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isMissed(latest.type)) Nord.red else MaterialTheme.colorScheme.onSurface,
                modifier = if (group.name == null && group.number.isNotEmpty()) copy else Modifier,
            )
        },
        supportingContent = {
            Column {
                if (group.name != null && group.number.isNotEmpty()) {
                    Text(
                        formatNumber(group.number),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 1,
                        modifier = copy,
                    )
                }
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (blocked) {
                        Icon(Lucide.Ban, null, tint = Nord.red, modifier = Modifier.size(16.dp))
                        Text(" Blocked · ", color = Nord.red, maxLines = 1)
                    }
                    group.calls.take(3).forEach {
                        val (icon, tint) = typeIcon(it.type)
                        Icon(icon, null, tint = tint ?: MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    Text(if (sim != null) "  $time · $sim" else "  $time", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        trailingContent = {
            if (group.number.isNotEmpty()) {
                IconButton(onClick = { onCall(group.number) }) { Icon(Lucide.Phone, "Call ${group.label}") }
            }
        },
    )
}

private fun typeName(type: Int) = when (type) {
    CallLog.Calls.INCOMING_TYPE -> "Incoming"
    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
    CallLog.Calls.MISSED_TYPE -> "Missed"
    CallLog.Calls.REJECTED_TYPE -> "Declined"
    CallLog.Calls.BLOCKED_TYPE -> "Blocked"
    CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
    CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> "Answered elsewhere"
    else -> "Call"
}

private fun formatDuration(seconds: Long) = when {
    seconds <= 0 -> ""
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
    else -> "${seconds / 3600}h ${seconds % 3600 / 60}m"
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun DetailRow(call: RecentCall, showNumber: Boolean, sim: String?, modifier: Modifier) {
    val (icon, tint) = typeIcon(call.type)
    val time = remember(call.id) { Instant.ofEpochMilli(call.date).atZone(ZoneId.systemDefault()).format(TIME) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier.padding(start = 16.dp, end = 24.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint ?: MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                typeName(call.type),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMissed(call.type)) Nord.red else MaterialTheme.colorScheme.onSurface,
            )
            val sub = listOfNotNull(formatNumber(call.number).takeIf { showNumber }, sim).joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = muted)
        }
        Text(
            time,
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatDuration(call.duration),
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun shareableText(rows: List<Contact>): String = rows.groupBy { it.id }.values.joinToString("\n\n") { numbers ->
    (listOf(numbers.first().name) + numbers.map { "${it.label}: ${formatNumber(it.number)}" }).joinToString("\n")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactsTab(
    contacts: List<Contact>?,
    state: LazyListState,
    onCall: (String) -> Unit,
    onEdit: (EditorRequest) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var confirm by remember { mutableStateOf(false) }
    var exportIds by remember { mutableStateOf(emptySet<Long>()) }
    BackHandler(selected.isNotEmpty()) { selected = emptySet() }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/x-vcard")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ids = exportIds
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { ContactWriter.exportVcf(context, ids, uri) } }
            val message = result.fold({ "Exported $it contacts" }, { "Export failed: ${it.message}" })
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    fun export(ids: Set<Long>) {
        exportIds = ids
        exporter.launch("contacts-${ids.size}.vcf")
    }
    fun toggle(id: Long) {
        selected = if (id in selected) selected - id else selected + id
    }

    val all = contacts ?: return

    if (confirm) {
        val count = selected.size
        ConfirmDialog(
            title = "Delete ${if (count == 1) "contact" else "$count contacts"}?",
            text = "They will be removed from this phone and from the accounts they sync to, such as Google.",
            confirm = "Delete",
            onConfirm = {
                val ids = selected
                selected = emptySet()
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { ContactWriter.delete(context, ids) } }
                    val message = result.fold(
                        { r ->
                            buildString {
                                append("Deleted ${r.deleted}")
                                if (r.readOnly > 0) append(". ${r.readOnly} come only from read-only apps like WhatsApp and were kept")
                            }
                        },
                        { "Couldn't delete: ${it.message}" },
                    )
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { confirm = false },
        )
    }
    val shown = remember(all, query) {
        val q = query.trim()
        val digits = q.filter { it.isDigit() }
        if (q.isEmpty()) {
            all
        } else {
            all.filter { it.name.contains(q, ignoreCase = true) || (digits.isNotEmpty() && it.digits.contains(digits)) }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
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
        Box(Modifier.weight(1f)) {
            if (shown.isEmpty()) {
                EmptyState(if (all.isEmpty()) "No contacts" else "No matches")
            } else {
                val favourites = remember(all, query) {
                    if (query.isBlank()) all.filter { it.starred }.distinctBy { it.id } else emptyList()
                }
                ContactList(
                    shown = shown,
                    favourites = favourites,
                    selected = selected,
                    state = state,
                    onCall = onCall,
                    onOpen = { onEdit(EditorRequest(it.id)) },
                    onClick = { contact ->
                        if (selected.isNotEmpty()) toggle(contact.id) else onEdit(EditorRequest(contact.id))
                    },
                    onLongClick = { toggle(it.id) },
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = selected.isEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                FloatingActionButton(onClick = { onEdit(EditorRequest(null)) }) {
                    Icon(Lucide.UserPlus, "New contact")
                }
            }
            SelectionBar(selected.size, onClear = { selected = emptySet() }) {
                IconButton(onClick = { context.shareText(shareableText(all.filter { it.id in selected })) }) {
                    Icon(Lucide.Share, "Share")
                }
                IconButton(onClick = { export(selected) }) { Icon(Lucide.Download, "Export") }
                IconButton(onClick = {
                    val numbers = all.filter { it.id in selected }.map { it.number }
                    selected = emptySet()
                    blockNumbers(context, scope, numbers)
                }) { Icon(Lucide.Ban, "Block") }
                IconButton(onClick = { confirm = true }) { Icon(Lucide.Trash, "Delete") }
            }
        }
    }
}

private fun blockNumbers(context: Context, scope: CoroutineScope, numbers: List<String>, block: Boolean = true) {
    val unique = numbers.filter { it.isNotBlank() }.distinctBy(::numberKey)
    scope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching { unique.forEach { if (block) Blocking.block(context, it) else Blocking.unblock(context, it) } }
        }
        val message = result.fold(
            { "${if (block) "Blocked" else "Unblocked"} ${unique.size} ${if (unique.size == 1) "number" else "numbers"}" },
            { "Couldn't update the block list. Is Curlew the default phone app?" },
        )
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Favourites(favourites: List<Contact>, onCall: (String) -> Unit, onOpen: (Contact) -> Unit) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(
            "Favourites",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, bottom = 4.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp)) {
            items(favourites, key = { it.id }) { contact ->
                Column(
                    Modifier
                        .width(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .combinedClickable(onClick = { onCall(contact.number) }, onLongClick = { onOpen(contact) })
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Avatar(contact.name, size = 56.dp, photo = contact.photo)
                    Text(
                        contact.name.substringBefore(' '),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactList(
    shown: List<Contact>,
    favourites: List<Contact>,
    selected: Set<Long>,
    state: LazyListState,
    onCall: (String) -> Unit,
    onOpen: (Contact) -> Unit,
    onClick: (Contact) -> Unit,
    onLongClick: (Contact) -> Unit,
) {
    val selectedColor = MaterialTheme.colorScheme.secondaryContainer
    LazyColumn(
        Modifier
            .fillMaxSize()
            .scrollbar(state),
        state = state,
        contentPadding = PaddingValues(bottom = if (selected.isNotEmpty()) SelectionBarSpace else 0.dp),
    ) {
        if (favourites.isNotEmpty()) {
            item(key = "favourites") { Favourites(favourites, onCall, onOpen) }
        }
        items(shown, key = { "${it.id}:${it.digits}" }) { contact ->
            val isSelected = contact.id in selected
            ListItem(
                modifier = Modifier
                    .animateItem()
                    .segment(isSelected, selectedColor, top = true, bottom = true)
                    .combinedClickable(onClick = { onClick(contact) }, onLongClick = { onLongClick(contact) }),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Avatar(contact.name, selected = isSelected, photo = contact.photo) },
                headlineContent = { Text(contact.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text("${contact.label} · ${contact.number}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingContent = {
                    IconButton(onClick = { onCall(contact.number) }) { Icon(Lucide.Phone, "Call ${contact.name}") }
                },
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
