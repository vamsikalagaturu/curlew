@file:Suppress("DEPRECATION")

package io.github.vamsikalagaturu.curlew.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vamsikalagaturu.curlew.CallInfo
import io.github.vamsikalagaturu.curlew.CallManager
import io.github.vamsikalagaturu.curlew.callAccounts
import io.github.vamsikalagaturu.curlew.stateLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private class LastCall {
    var info: CallInfo? = null
}

private data class Reveal(val color: Color, val origin: Offset)

private enum class Phase { PickSim, Ringing, Live }

private val OUTGOING = setOf(Call.STATE_DIALING, Call.STATE_CONNECTING)

@Composable
fun InCallScreen(onAddCall: () -> Unit, onNoCalls: () -> Unit) {
    val context = LocalContext.current
    val calls by CallManager.calls.collectAsStateWithLifecycle()
    val audio by CallManager.audio.collectAsStateWithLifecycle()
    // Telecom drops a call from the list the moment it ends; the last one stays on screen as "Call ended" briefly.
    val last = remember { LastCall() }
    val ended = calls.isEmpty()
    LaunchedEffect(ended) {
        if (ended) {
            if (last.info != null) delay(1000)
            onNoCalls()
        }
    }
    val header = remember { Animatable(0f) }
    val footer = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { header.animateTo(1f, tween(450, easing = LinearOutSlowInEasing)) }
        delay(90)
        footer.animateTo(1f, tween(450, easing = LinearOutSlowInEasing))
    }
    val avatarPop = remember { Animatable(1f) }
    var reveal by remember { mutableStateOf<Reveal?>(null) }
    val revealGrow = remember { Animatable(0f) }
    val revealFade = remember { Animatable(0f) }
    LaunchedEffect(reveal) {
        if (reveal == null) return@LaunchedEffect
        revealGrow.snapTo(0f)
        revealFade.snapTo(1f)
        revealGrow.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        revealFade.animateTo(0f, tween(450))
        reveal = null
    }
    val primary = calls.firstOrNull()?.also { last.info = it } ?: last.info ?: return
    val others = calls.drop(1)
    var keypad by rememberSaveable { mutableStateOf(false) }
    var replying by remember { mutableStateOf(false) }
    var dtmf by rememberSaveable(primary.call.hashCode()) { mutableStateOf("") }

    val haptics = LocalHapticFeedback.current
    var previous by remember(primary.call) { mutableIntStateOf(primary.state) }
    LaunchedEffect(primary.call, primary.state) {
        val from = previous
        previous = primary.state
        if (primary.state == Call.STATE_ACTIVE && (from in OUTGOING || from == Call.STATE_RINGING)) {
            if (from in OUTGOING) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            avatarPop.animateTo(1.15f, tween(140, easing = FastOutSlowInEasing))
            avatarPop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    LaunchedEffect(ended) {
        if (ended) avatarPop.animateTo(0.88f, spring(dampingRatio = 0.6f))
    }

    val colors = MaterialTheme.colorScheme
    val waiting = !ended && (primary.state == Call.STATE_RINGING || primary.state in OUTGOING)
    val tint by animateColorAsState(
        when {
            ended -> Nord.red
            primary.state == Call.STATE_ACTIVE -> Nord.green
            primary.state == Call.STATE_HOLDING -> Nord.yellow
            else -> colors.primary
        },
        tween(700),
        label = "tint",
    )
    val ambient = rememberInfiniteTransition(label = "ambient")
    val breathe by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val status = when {
        ended -> stateLabel(Call.STATE_DISCONNECTED)
        primary.state == Call.STATE_ACTIVE && primary.connectedAt > 0 -> null
        else -> stateLabel(primary.state)
    }
    val statusColor by animateColorAsState(if (ended) Nord.red else colors.primary, label = "statusColor")
    val dim by animateFloatAsState(if (ended) 0.35f else 1f, tween(400), label = "dim")
    val phase = when (primary.state) {
        Call.STATE_SELECT_PHONE_ACCOUNT -> Phase.PickSim
        Call.STATE_RINGING -> Phase.Ringing
        else -> Phase.Live
    }

    Surface(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                reveal?.let { r ->
                    drawCircle(
                        r.color.copy(alpha = 0.4f * revealFade.value),
                        radius = revealGrow.value * size.maxDimension * 1.25f,
                        center = r.origin,
                    )
                }
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val glow = if (waiting) 0.20f + 0.12f * breathe else 0.16f
                    drawRect(
                        Brush.radialGradient(
                            listOf(tint.copy(alpha = glow), Color.Transparent),
                            center = Offset(size.width / 2f, size.height * 0.2f),
                            radius = size.width * (0.85f + 0.15f * breathe),
                        ),
                    )
                }
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
            ) {
                others.forEach {
                    OtherCall(it)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
            Column(Modifier.rise(header), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    PulseRings(tint, visible = waiting)
                    Box(
                        Modifier.graphicsLayer {
                            val s = avatarPop.value * (if (waiting) 1f + 0.05f * breathe else 1f)
                            scaleX = s
                            scaleY = s
                            alpha = if (ended) 0.7f else 1f
                        },
                    ) {
                        Avatar(primary.label, size = 96.dp, photo = primary.photo)
                    }
                }
                Spacer(Modifier.height(16.dp))
                AnimatedContent(primary.label, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "name") { name ->
                    Text(
                        name,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
                if (primary.name != null && primary.number.isNotEmpty()) {
                    Text(primary.number, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                val statusStyle = MaterialTheme.typography.titleMedium
                if (keypad && dtmf.isNotEmpty() && !ended) {
                    Text(dtmf, style = statusStyle, color = colors.primary)
                } else {
                    AnimatedContent(
                        status,
                        transitionSpec = {
                            (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                        },
                        label = "status",
                    ) { s ->
                        if (s == null) {
                            RollingText(elapsed(primary.connectedAt), statusStyle, statusColor)
                        } else {
                            Text(s, style = statusStyle, color = statusColor)
                        }
                    }
                }
                primary.sim?.let {
                    Text("via $it", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            if (replying && !ended) {
                QuickReplies(
                    onSend = { text ->
                        replying = false
                        CallManager.rejectWithMessage(primary.call, text)
                    },
                    onDismiss = { replying = false },
                )
            }
            Spacer(Modifier.weight(1f))

            AnimatedContent(
                phase,
                transitionSpec = {
                    (fadeIn(tween(300, delayMillis = 150)) + slideInVertically(tween(350, delayMillis = 150)) { it / 4 }) togetherWith
                        fadeOut(tween(150)) using SizeTransform(clip = false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .rise(footer)
                    .graphicsLayer { alpha = dim }
                    .then(if (ended) Modifier.blockTouches() else Modifier),
                label = "phase",
            ) { shown ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (shown) {
                        Phase.PickSim -> {
                            AccountPicker(primary)
                            Spacer(Modifier.height(24.dp))
                            RoundAction(Lucide.PhoneOff, "Cancel", Nord.red) { CallManager.hangup(primary.call) }
                        }
                        Phase.Ringing -> key(primary.call) {
                            val covered = rememberCovered()
                            SwipeToAnswer(
                                enabled = !covered,
                                onReveal = { reveal = it },
                                onAnswer = { CallManager.answer(primary.call) },
                                onDecline = { CallManager.reject(primary.call) },
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { CallManager.silence(context, primary.call) },
                                    enabled = !covered && !primary.silenced,
                                ) {
                                    Icon(Lucide.BellOff, null, Modifier.size(18.dp))
                                    AnimatedContent(primary.silenced, label = "silence") { silenced ->
                                        Text(if (silenced) "Silenced" else "Silence", Modifier.padding(start = 8.dp))
                                    }
                                }
                                if (primary.number.isNotEmpty()) {
                                    TextButton(onClick = { replying = true }, enabled = !covered) {
                                        Icon(Lucide.MessageSquare, null, Modifier.size(18.dp))
                                        Text("Message", Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        }
                        Phase.Live -> {
                            AnimatedContent(
                                keypad,
                                transitionSpec = {
                                    (fadeIn(tween(220, delayMillis = 80)) + scaleIn(tween(220, delayMillis = 80), initialScale = 0.9f)) togetherWith
                                        (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.95f)) using SizeTransform(clip = false)
                                },
                                label = "keypad",
                            ) { showing ->
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (showing) {
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
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            RoundAction(Lucide.PhoneOff, null, Nord.red) { CallManager.hangup(primary.call) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun Modifier.rise(progress: Animatable<Float, *>) = graphicsLayer {
    alpha = progress.value
    translationY = (1f - progress.value) * 32.dp.toPx()
}

private fun Modifier.blockTouches() = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
    }
}

// Pocket guard: the answer handle ignores touches while the proximity sensor is covered. "Near" is below
// 5 cm or the sensor's range, the threshold AOSP's DisplayPowerController uses; binary sensors report 0 or max.
@Composable
private fun rememberCovered(): Boolean {
    val context = LocalContext.current
    var covered by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val sensors = context.getSystemService(SensorManager::class.java)
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val distance = event.values[0]
                covered = distance >= 0f && distance < minOf(event.sensor.maximumRange, 5f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensors?.unregisterListener(listener) }
    }
    return covered
}

@Composable
private fun SwipeToAnswer(enabled: Boolean, onReveal: (Reveal) -> Unit, onAnswer: () -> Unit, onDecline: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val travel = 112.dp
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    val reach = with(LocalDensity.current) { travel.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }
    var done by remember { mutableStateOf(false) }
    var center by remember { mutableStateOf(Offset.Zero) }
    val interaction = remember { MutableInteractionSource() }
    val dragging by interaction.collectIsDraggedAsState()
    val lift by animateFloatAsState(if (dragging) 1.12f else 1f, spring(dampingRatio = 0.5f), label = "lift")
    LaunchedEffect(enabled) { if (!enabled && !done) offset = 0f }
    val idle = rememberInfiniteTransition(label = "hint")
    val bob by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val halo by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)),
        label = "halo",
    )
    val flow by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "flow",
    )
    val drag = rememberDraggableState { delta ->
        val next = (offset + delta).coerceIn(-reach, reach)
        if ((abs(offset) >= threshold) != (abs(next) >= threshold)) {
            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
        offset = next
    }
    // Negative is up (answer), positive is down (decline).
    val progress = (offset / threshold).coerceIn(-1f, 1f)
    val handle = when {
        progress < 0f -> lerp(colors.surfaceContainerHighest, Nord.green, -progress)
        else -> lerp(colors.surfaceContainerHighest, Nord.red, progress)
    }
    val iconTint = if (abs(progress) > 0.5f) Nord.night0 else colors.onSurface
    val resting = offset == 0f && enabled && !done

    Box(
        Modifier
            .fillMaxWidth()
            .height(travel * 2 + 72.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        contentAlignment = Alignment.Center,
    ) {
        SwipeHint(
            "Swipe up to answer", Nord.green, up = true, flow = { flow },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = 1f - progress.coerceAtLeast(0f) },
        )
        SwipeHint(
            "Swipe down to decline", Nord.red, up = false, flow = { flow },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = 1f + progress.coerceAtMost(0f) },
        )
        Box(
            Modifier
                .size(72.dp)
                .graphicsLayer {
                    val s = 1f + halo * 0.6f
                    scaleX = s
                    scaleY = s
                    alpha = if (resting) (1f - halo) * 0.5f else 0f
                }
                .background(Nord.green, CircleShape),
        )
        Surface(
            shape = CircleShape,
            color = handle,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(72.dp)
                .offset {
                    val hint = if (resting) -bob * 10.dp.toPx() else 0f
                    IntOffset(0, (offset + hint).roundToInt())
                }
                .graphicsLayer {
                    scaleX = lift
                    scaleY = lift
                }
                .onGloballyPositioned { center = it.boundsInRoot().center }
                .draggable(
                    drag,
                    Orientation.Vertical,
                    enabled = enabled && !done,
                    interactionSource = interaction,
                    onDragStopped = {
                        val answer = offset <= -threshold
                        val decline = offset >= threshold
                        if (answer || decline) {
                            done = true
                            haptics.performHapticFeedback(if (answer) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)
                            onReveal(Reveal(if (answer) Nord.green else Nord.red, center))
                            animate(offset, if (answer) -reach else reach, animationSpec = tween(120)) { v, _ -> offset = v }
                            if (answer) onAnswer() else onDecline()
                        } else {
                            animate(offset, 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { v, _ -> offset = v }
                        }
                    },
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Lucide.Phone,
                    "Answer or decline",
                    tint = iconTint,
                    modifier = Modifier
                        .size(32.dp)
                        .rotate(progress.coerceAtLeast(0f) * 135f),
                )
            }
        }
    }
}

@Composable
private fun SwipeHint(text: String, color: Color, up: Boolean, flow: () -> Float, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val chevrons = @Composable {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                repeat(3) { i ->
                    // The bright chevron travels toward the hint's direction: upward to answer, downward to decline.
                    val step = if (up) 2 - i else i
                    Icon(
                        Lucide.ChevronDown,
                        null,
                        tint = color,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (up) 180f else 0f)
                            .graphicsLayer {
                                val d = (flow() * 3f - step + 3f) % 3f
                                alpha = 0.2f + 0.8f * (if (d < 1f) sin(d * PI.toFloat()) else 0f)
                            },
                    )
                }
            }
        }
        if (up) chevrons()
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
        if (!up) chevrons()
    }
}

@Composable
private fun RollingText(text: String, style: TextStyle, color: Color) {
    val digits = style.copy(fontFeatureSettings = "tnum")
    Row {
        text.forEachIndexed { i, c ->
            // Keyed from the right so the seconds keep their slot when the minutes gain a digit.
            key(text.length - i) {
                AnimatedContent(
                    c,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) using
                            SizeTransform(clip = true)
                    },
                    label = "digit",
                ) { ch -> Text(ch.toString(), style = digits, color = color) }
            }
        }
    }
}

@Composable
fun IncomingCard(
    info: CallInfo?,
    visible: Boolean,
    onOpen: () -> Unit,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onSilence: () -> Unit,
    onDrag: (Float) -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) { state.targetState = visible }
    AnimatedVisibility(
        state,
        enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn(),
        exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(220)),
    ) {
        if (info != null) CardContent(info, onOpen, onAnswer, onDecline, onSilence, onDrag)
    }
}

@Composable
private fun CardContent(
    info: CallInfo,
    onOpen: () -> Unit,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onSilence: () -> Unit,
    onDrag: (Float) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val motion = rememberInfiniteTransition(label = "card")
    val wiggle by motion.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1600
                0f at 0
                -14f at 80
                14f at 160
                -10f at 240
                10f at 320
                0f at 400
            },
        ),
        label = "wiggle",
    )
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerHigh,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dy ->
                    change.consume()
                    onDrag(dy)
                }
            },
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                PulseRings(Nord.green, visible = true, size = 48.dp)
                Avatar(info.label, size = 48.dp, photo = info.photo)
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(info.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(if (info.silenced) "Silenced" else stateLabel(info.state), info.sim).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedVisibility(
                !info.silenced,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Row {
                    CardAction(Lucide.BellOff, "Silence", colors.surfaceContainerHighest, colors.onSurface, onSilence)
                    Spacer(Modifier.width(8.dp))
                }
            }
            CardAction(Lucide.PhoneOff, "Decline", Nord.red, Nord.night0, onDecline)
            Spacer(Modifier.width(12.dp))
            CardAction(Lucide.Phone, "Answer", Nord.green, Nord.night0, onAnswer, Modifier.graphicsLayer { rotationZ = wiggle })
        }
    }
}

@Composable
private fun CardAction(
    icon: ImageVector,
    description: String,
    color: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.85f else 1f, spring(dampingRatio = 0.5f), label = "press")
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color,
        contentColor = content,
        interactionSource = interaction,
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            },
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, description, Modifier.size(24.dp)) }
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
private fun PulseRings(color: Color, visible: Boolean, size: Dp = 96.dp) {
    AnimatedVisibility(visible, enter = fadeIn(tween(300)), exit = fadeOut(tween(500))) {
        val pulse = rememberInfiniteTransition(label = "pulse")
        val progress by pulse.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
            label = "rings",
        )
        Box(contentAlignment = Alignment.Center) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .size(size)
                        .graphicsLayer {
                            val phase = (progress + i / 3f) % 1f
                            val scale = 1f + LinearOutSlowInEasing.transform(phase) * 0.75f
                            scaleX = scale
                            scaleY = scale
                            alpha = (1f - phase) * 0.6f
                        }
                        .border(2.dp, color, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun elapsed(connectedAt: Long): String {
    val now by produceState(System.currentTimeMillis(), connectedAt) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val seconds = ((now - connectedAt) / 1000).coerceAtLeast(0)
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
            ControlButton(Lucide.MicOff, "Mute", index = 0, checked = muted) { CallManager.setMuted(!muted) }
            ControlButton(Lucide.Keypad, "Keypad", index = 1, onClick = onKeypad)
            AudioRouteButton(audio, index = 2)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val held = info.state == Call.STATE_HOLDING
            ControlButton(Lucide.Pause, "Hold", index = 3, checked = held, enabled = info.canHold || held) {
                CallManager.toggleHold(info)
            }
            ControlButton(Lucide.UserPlus, "Add call", index = 4, onClick = onAddCall)
            if (info.canSwap) {
                ControlButton(Lucide.ArrowLeftRight, "Swap", index = 5) { CallManager.swap(info) }
            } else {
                ControlButton(Lucide.Merge, "Merge", index = 5, enabled = info.canMerge) { CallManager.merge(info) }
            }
        }
    }
}

@Composable
private fun AudioRouteButton(audio: CallAudioState?, index: Int) {
    val mask = audio?.supportedRouteMask ?: 0
    val route = audio?.route ?: CallAudioState.ROUTE_EARPIECE
    val bluetooth = mask and CallAudioState.ROUTE_BLUETOOTH != 0
    var menu by remember { mutableStateOf(false) }
    Box {
        if (bluetooth) {
            ControlButton(routeIcon(route), routeName(route), index, checked = route != CallAudioState.ROUTE_EARPIECE) { menu = true }
        } else {
            val speaker = route == CallAudioState.ROUTE_SPEAKER
            ControlButton(Lucide.Volume2, "Speaker", index, checked = speaker) {
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
    index: Int,
    checked: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 40L)
        appear.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.5f), label = "press")
    val container by animateColorAsState(if (checked) colors.primary else colors.surfaceContainerHigh, label = "container")
    val content by animateColorAsState(
        when {
            !enabled -> colors.onSurface.copy(alpha = 0.38f)
            checked -> colors.onPrimary
            else -> colors.onSurface
        },
        label = "content",
    )
    val bounce = remember { Animatable(1f) }
    var wasChecked by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) {
        if (checked != wasChecked) {
            wasChecked = checked
            bounce.snapTo(0.7f)
            bounce.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
        }
    }
    Column(
        Modifier
            .width(88.dp)
            .graphicsLayer {
                val a = appear.value
                alpha = a.coerceIn(0f, 1f)
                val s = 0.6f + 0.4f * a
                scaleX = s
                scaleY = s
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = container,
            contentColor = content,
            interactionSource = interaction,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    label,
                    Modifier.graphicsLayer {
                        scaleX = bounce.value
                        scaleY = bounce.value
                    },
                )
            }
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.5f), label = "press")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = color,
            contentColor = Nord.night0,
            interactionSource = interaction,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                },
        ) {
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
    val known = remember { callAccounts(context).associateBy { it.handle } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Call with", style = MaterialTheme.typography.titleMedium)
        info.accounts.forEach { handle ->
            val label = known[handle]?.display ?: handle.id
            OutlinedButton(onClick = { CallManager.selectAccount(info.call, handle) }, modifier = Modifier.fillMaxWidth()) {
                Text(label)
            }
        }
    }
}
