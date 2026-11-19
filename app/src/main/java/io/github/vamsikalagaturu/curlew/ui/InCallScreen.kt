@file:Suppress("DEPRECATION")

package io.github.vamsikalagaturu.curlew.ui

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vamsikalagaturu.curlew.CallInfo
import io.github.vamsikalagaturu.curlew.CallManager
import io.github.vamsikalagaturu.curlew.stateLabel
import kotlinx.coroutines.delay

@Composable
fun InCallScreen(onAddCall: () -> Unit, onNoCalls: () -> Unit) {
    val calls by CallManager.calls.collectAsStateWithLifecycle()
    val audio by CallManager.audio.collectAsStateWithLifecycle()
    LaunchedEffect(calls.isEmpty()) { if (calls.isEmpty()) onNoCalls() }
    val primary = calls.firstOrNull() ?: return
    val others = calls.drop(1)
    var keypad by rememberSaveable { mutableStateOf(false) }
    var replying by remember { mutableStateOf(false) }
    var dtmf by rememberSaveable(primary.call.hashCode()) { mutableStateOf("") }

    val colors = MaterialTheme.colorScheme
    val waiting = primary.state == Call.STATE_RINGING || primary.state == Call.STATE_DIALING || primary.state == Call.STATE_CONNECTING
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.22f), colors.surface, colors.surface)))
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            others.forEach {
                OtherCall(it)
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(32.dp))
            Box(contentAlignment = Alignment.Center) {
                if (waiting) PulseRing(colors.primary)
                Avatar(primary.label, size = 96.dp, photo = primary.photo)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                primary.label,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (primary.name != null && primary.number.isNotEmpty()) {
                Text(primary.number, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (keypad && dtmf.isNotEmpty()) dtmf else statusText(primary),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            primary.sim?.let {
                Text("via $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (replying) {
                QuickReplies(
                    onSend = { text ->
                        replying = false
                        CallManager.rejectWithMessage(primary.call, text)
                    },
                    onDismiss = { replying = false },
                )
            }
            Spacer(Modifier.weight(1f))

            when {
                primary.state == Call.STATE_SELECT_PHONE_ACCOUNT -> {
                    AccountPicker(primary)
                    Spacer(Modifier.height(24.dp))
                    RoundAction(Lucide.PhoneOff, "Cancel", Nord.red) { CallManager.hangup(primary.call) }
                }
                primary.state == Call.STATE_RINGING -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    RoundAction(Lucide.PhoneOff, "Decline", Nord.red) { CallManager.reject(primary.call) }
                    if (primary.number.isNotEmpty()) {
                        RoundAction(Lucide.MessageSquare, "Message", MaterialTheme.colorScheme.primary) { replying = true }
                    }
                    RoundAction(Lucide.Phone, "Answer", Nord.green) { CallManager.answer(primary.call) }
                }
                else -> {
                    if (keypad) {
                        Dialpad(
                            onKeyDown = {
                                dtmf += it
                                CallManager.dtmfDown(primary.call, it)
                            },
                            onKeyUp = { CallManager.dtmfUp(primary.call) },
                        )
                        TextButton(onClick = { keypad = false }) { Text("Hide keypad") }
                    } else {
                        Controls(primary, audio, onKeypad = { keypad = true }, onAddCall = onAddCall)
                    }
                    Spacer(Modifier.height(24.dp))
                    RoundAction(Lucide.PhoneOff, null, Nord.red) { CallManager.hangup(primary.call) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private val QUICK_REPLIES = listOf(
    "Can't talk now. I'll call you back.",
    "I'll call you later.",
    "I'm in a meeting. Text me?",
)

@Composable
private fun QuickReplies(onSend: (String) -> Unit, onDismiss: () -> Unit) {
    var custom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reply with a message") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                QUICK_REPLIES.forEach { reply ->
                    Surface(
                        onClick = { onSend(reply) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(reply, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    placeholder = { Text("Write your own…") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = custom.isNotBlank(), onClick = { onSend(custom.trim()) }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PulseRing(color: Color) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val progress by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ring",
    )
    Box(
        Modifier
            .size(96.dp)
            .graphicsLayer {
                val scale = 1f + progress * 0.6f
                scaleX = scale
                scaleY = scale
                alpha = 1f - progress
            }
            .border(3.dp, color, CircleShape),
    )
}

@Composable
private fun statusText(info: CallInfo): String {
    if (info.state != Call.STATE_ACTIVE || info.connectedAt <= 0) return stateLabel(info.state)
    val now by produceState(System.currentTimeMillis(), info.connectedAt) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val seconds = ((now - info.connectedAt) / 1000).coerceAtLeast(0)
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun Controls(info: CallInfo, audio: CallAudioState?, onKeypad: () -> Unit, onAddCall: () -> Unit) {
    val muted = audio?.isMuted == true
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ControlButton(Lucide.MicOff, "Mute", checked = muted) { CallManager.setMuted(!muted) }
            ControlButton(Lucide.Keypad, "Keypad", onClick = onKeypad)
            AudioRouteButton(audio)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val held = info.state == Call.STATE_HOLDING
            ControlButton(Lucide.Pause, "Hold", checked = held, enabled = info.canHold || held) {
                CallManager.toggleHold(info)
            }
            ControlButton(Lucide.UserPlus, "Add call", onClick = onAddCall)
            if (info.canSwap) {
                ControlButton(Lucide.ArrowLeftRight, "Swap") { CallManager.swap(info) }
            } else {
                ControlButton(Lucide.Merge, "Merge", enabled = info.canMerge) { CallManager.merge(info) }
            }
        }
    }
}

@Composable
private fun AudioRouteButton(audio: CallAudioState?) {
    val mask = audio?.supportedRouteMask ?: 0
    val route = audio?.route ?: CallAudioState.ROUTE_EARPIECE
    val bluetooth = mask and CallAudioState.ROUTE_BLUETOOTH != 0
    var menu by remember { mutableStateOf(false) }
    Box {
        if (bluetooth) {
            ControlButton(routeIcon(route), routeName(route), checked = route != CallAudioState.ROUTE_EARPIECE) { menu = true }
        } else {
            val speaker = route == CallAudioState.ROUTE_SPEAKER
            ControlButton(Lucide.Volume2, "Speaker", checked = speaker) {
                val base = if (mask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
                    CallAudioState.ROUTE_WIRED_HEADSET
                } else {
                    CallAudioState.ROUTE_EARPIECE
                }
                CallManager.setRoute(if (speaker) base else CallAudioState.ROUTE_SPEAKER)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            ROUTES.filter { mask and it != 0 }.forEach { r ->
                DropdownMenuItem(
                    text = { Text(routeName(r)) },
                    leadingIcon = { Icon(routeIcon(r), null) },
                    trailingIcon = if (r == route) {
                        { Icon(Lucide.Check, null) }
                    } else {
                        null
                    },
                    onClick = {
                        CallManager.setRoute(r)
                        menu = false
                    },
                )
            }
        }
    }
}

private val ROUTES = listOf(
    CallAudioState.ROUTE_BLUETOOTH,
    CallAudioState.ROUTE_SPEAKER,
    CallAudioState.ROUTE_WIRED_HEADSET,
    CallAudioState.ROUTE_EARPIECE,
)

private fun routeName(route: Int) = when (route) {
    CallAudioState.ROUTE_BLUETOOTH -> "Bluetooth"
    CallAudioState.ROUTE_SPEAKER -> "Speaker"
    CallAudioState.ROUTE_WIRED_HEADSET -> "Headset"
    else -> "Phone"
}

private fun routeIcon(route: Int): ImageVector = when (route) {
    CallAudioState.ROUTE_BLUETOOTH -> Lucide.Bluetooth
    CallAudioState.ROUTE_SPEAKER -> Lucide.Volume2
    CallAudioState.ROUTE_WIRED_HEADSET -> Lucide.Headphones
    else -> Lucide.PhoneCall
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    checked: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.width(88.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = if (checked) colors.primary else colors.surfaceContainerHigh,
            contentColor = when {
                !enabled -> colors.onSurface.copy(alpha = 0.38f)
                checked -> colors.onPrimary
                else -> colors.onSurface
            },
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, label) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f),
        )
    }
}

@Composable
private fun RoundAction(icon: ImageVector, label: String?, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(onClick = onClick, shape = CircleShape, color = color, contentColor = Nord.night0, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, label ?: "End call", Modifier.size(32.dp)) }
        }
        if (label != null) {
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun OtherCall(info: CallInfo) {
    Surface(
        onClick = { if (info.state == Call.STATE_HOLDING) info.call.unhold() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(info.label, size = 32.dp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(info.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stateLabel(info.state), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (info.state == Call.STATE_HOLDING) {
                Text("Switch", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AccountPicker(info: CallInfo) {
    val context = LocalContext.current
    val telecom = remember { context.getSystemService(TelecomManager::class.java) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Call with", style = MaterialTheme.typography.titleMedium)
        info.accounts.forEach { handle ->
            val label = remember(handle) {
                runCatching { telecom.getPhoneAccount(handle)?.label?.toString() }.getOrNull() ?: handle.id
            }
            OutlinedButton(onClick = { CallManager.selectAccount(info.call, handle) }, modifier = Modifier.fillMaxWidth()) {
                Text(label)
            }
        }
    }
}
