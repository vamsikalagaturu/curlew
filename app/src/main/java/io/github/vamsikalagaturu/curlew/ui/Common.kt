package io.github.vamsikalagaturu.curlew.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.delay
import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KEYS = listOf(
    '1' to "", '2' to "ABC", '3' to "DEF",
    '4' to "GHI", '5' to "JKL", '6' to "MNO",
    '7' to "PQRS", '8' to "TUV", '9' to "WXYZ",
    '*' to "", '0' to "+", '#' to "",
)

@Composable
fun Dialpad(
    onKeyDown: (Char) -> Unit,
    modifier: Modifier = Modifier,
    onKeyUp: (Char) -> Unit = {},
    onLongPress: (Char) -> Unit = {},
    haptics: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KEYS.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { (digit, letters) -> DialKey(digit, letters, haptics, onKeyDown, onKeyUp, onLongPress) }
            }
        }
    }
}

@Composable
private fun DialKey(
    digit: Char,
    letters: String,
    haptics: Boolean,
    onKeyDown: (Char) -> Unit,
    onKeyUp: (Char) -> Unit,
    onLongPress: (Char) -> Unit,
) {
    val feedback = LocalHapticFeedback.current
    val buzz by rememberUpdatedState(haptics)
    val down by rememberUpdatedState(onKeyDown)
    val up by rememberUpdatedState(onKeyUp)
    val long by rememberUpdatedState(onLongPress)
    var pressed by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        if (pressed) colors.primary.copy(alpha = 0.35f) else colors.surfaceContainer,
        animationSpec = tween(if (pressed) 60 else 280),
        label = "key",
    )
    val scale by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "keyScale",
    )
    Box(
        Modifier
            .size(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(background)
            .pointerInput(digit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        if (buzz) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        down(digit)
                        tryAwaitRelease()
                        pressed = false
                        up(digit)
                    },
                    onLongPress = { long(digit) },
                )
            }
            .semantics {
                contentDescription = digit.toString()
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(digit.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(letters, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun scrollbarAlpha(scrolling: Boolean): Float {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(scrolling) {
        if (scrolling) {
            alpha.animateTo(1f, tween(120))
        } else {
            delay(900)
            alpha.animateTo(0f, tween(500))
        }
    }
    return alpha.value
}

private fun DrawScope.drawThumb(color: Color, alpha: Float, viewport: Float, content: Float, scrolled: Float) {
    if (alpha == 0f || content <= viewport) return
    val width = 4.dp.toPx()
    val thumb = (viewport * viewport / content).coerceAtLeast(40.dp.toPx())
    val top = (scrolled / (content - viewport)).coerceIn(0f, 1f) * (size.height - thumb)
    drawRoundRect(
        color = color.copy(alpha = color.alpha * alpha),
        topLeft = Offset(size.width - width - 3.dp.toPx(), top),
        size = Size(width, thumb),
        cornerRadius = CornerRadius(width / 2),
    )
}

// Lazy lists only know the sizes of visible rows, so content height is estimated from their average.
@Composable
fun Modifier.scrollbar(state: LazyListState): Modifier {
    val alpha = scrollbarAlpha(state.isScrollInProgress)
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    return drawWithContent {
        drawContent()
        val info = state.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return@drawWithContent
        val average = visible.sumOf { it.size }.toFloat() / visible.size
        val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
        val scrolled = state.firstVisibleItemIndex * average + state.firstVisibleItemScrollOffset
        drawThumb(color, alpha, viewport, average * info.totalItemsCount, scrolled)
    }
}

@Composable
fun Modifier.scrollbar(state: ScrollState): Modifier {
    val alpha = scrollbarAlpha(state.isScrollInProgress)
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    return drawWithContent {
        drawContent()
        val viewport = size.height
        drawThumb(color, alpha, viewport, viewport + state.maxValue, state.value.toFloat())
    }
}

fun avatarHue(label: String): Color = Nord.avatars[Math.floorMod(label.hashCode(), Nord.avatars.size)]

private object Photos {
    private val cache = LruCache<String, ImageBitmap>(256)

    fun cached(uri: String): ImageBitmap? = cache.get(uri)

    fun load(context: Context, uri: String): ImageBitmap? = cache.get(uri) ?: runCatching {
        context.contentResolver.openInputStream(uri.toUri())?.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
    }.getOrNull()?.also { cache.put(uri, it) }
}

@Composable
fun Avatar(label: String, size: Dp = 40.dp, selected: Boolean = false, photo: String? = null, bare: Boolean = false) {
    val context = LocalContext.current
    val image by produceState(photo?.let(Photos::cached), photo) {
        if (photo != null && value == null) value = withContext(Dispatchers.IO) { Photos.load(context, photo) }
    }
    val bitmap = image
    if (bitmap != null && !selected) {
        Image(
            bitmap,
            label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
        )
        return
    }
    val initial = label.firstOrNull()?.takeIf { it.isLetter() }?.uppercaseChar()
    val hue = if (initial != null) avatarHue(label) else MaterialTheme.colorScheme.onSurfaceVariant
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else hue.copy(alpha = 0.22f),
        label = "avatar",
    )
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (bare) Modifier else Modifier.background(background)),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Lucide.Check, "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(size * 0.55f))
        } else if (initial != null) {
            Text(
                initial.toString(),
                color = hue,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.42f).sp,
            )
        } else {
            Icon(
                Lucide.User, null,
                tint = hue,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

@Composable
fun CurlewSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        thumbContent = if (checked) {
            { Icon(Lucide.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
        } else {
            null
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onPrimary,
            checkedTrackColor = colors.primary,
            checkedIconColor = colors.primary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = colors.onSurfaceVariant,
            uncheckedTrackColor = colors.surfaceContainerHighest,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

val SelectionBarSpace = 96.dp

@Composable
fun BoxScope.SelectionBar(count: Int, onClear: () -> Unit, actions: @Composable RowScope.() -> Unit) {
    var shownCount by remember { mutableIntStateOf(count) }
    if (count > 0) shownCount = count
    AnimatedVisibility(
        visible = count > 0,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClear) { Icon(Lucide.X, "Clear selection") }
                Text("$shownCount selected", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                actions()
            }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(confirm, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
