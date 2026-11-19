package io.github.vamsikalagaturu.curlew.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.vamsikalagaturu.curlew.Prefs
import io.github.vamsikalagaturu.curlew.ThemeMode

object Nord {
    val night0 = Color(0xFF2E3440)
    val night1 = Color(0xFF3B4252)
    val night2 = Color(0xFF434C5E)
    val night3 = Color(0xFF4C566A)
    val snow0 = Color(0xFFD8DEE9)
    val snow1 = Color(0xFFE5E9F0)
    val snow2 = Color(0xFFECEFF4)
    val frost1 = Color(0xFF88C0D0)
    val frost2 = Color(0xFF81A1C1)
    val frost3 = Color(0xFF5E81AC)
    val frost0 = Color(0xFF8FBCBB)
    val red = Color(0xFFBF616A)
    val orange = Color(0xFFD08770)
    val yellow = Color(0xFFEBCB8B)
    val green = Color(0xFFA3BE8C)
    val purple = Color(0xFFB48EAD)

    val avatars = listOf(frost0, frost1, frost2, frost3, red, orange, yellow, green, purple)
}

private val Dark = darkColorScheme(
    primary = Nord.frost1,
    onPrimary = Nord.night0,
    primaryContainer = Nord.night2,
    onPrimaryContainer = Nord.frost1,
    secondary = Nord.frost2,
    onSecondary = Nord.night0,
    secondaryContainer = Nord.night2,
    onSecondaryContainer = Nord.snow2,
    background = Nord.night0,
    onBackground = Nord.snow2,
    surface = Nord.night0,
    onSurface = Nord.snow2,
    surfaceVariant = Nord.night1,
    onSurfaceVariant = Nord.snow0,
    surfaceContainerLowest = Nord.night0,
    surfaceContainerLow = Nord.night1,
    surfaceContainer = Nord.night1,
    surfaceContainerHigh = Nord.night2,
    surfaceContainerHighest = Nord.night3,
    outline = Nord.night3,
    outlineVariant = Nord.night2,
    error = Nord.red,
    onError = Nord.night0,
)

private val Light = lightColorScheme(
    primary = Nord.frost3,
    onPrimary = Nord.snow2,
    primaryContainer = Nord.snow1,
    onPrimaryContainer = Nord.frost3,
    secondary = Nord.frost2,
    onSecondary = Nord.night0,
    secondaryContainer = Nord.snow0,
    onSecondaryContainer = Nord.night0,
    background = Nord.snow2,
    onBackground = Nord.night0,
    surface = Nord.snow2,
    onSurface = Nord.night0,
    surfaceVariant = Nord.snow1,
    onSurfaceVariant = Nord.night3,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Nord.snow1,
    surfaceContainer = Nord.snow1,
    surfaceContainerHigh = Nord.snow0,
    surfaceContainerHighest = Nord.snow0,
    outline = Nord.night3,
    outlineVariant = Nord.snow0,
    error = Nord.red,
    onError = Nord.snow2,
)

private val Black = Dark.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0C0E12),
    surfaceContainer = Color(0xFF15181E),
    surfaceContainerHigh = Color(0xFF1F232B),
    surfaceContainerHighest = Color(0xFF2A2F39),
    surfaceVariant = Color(0xFF15181E),
)

@Composable
fun CurlewTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs.get(context) }
    val mode by prefs.theme.collectAsState()
    val amoled by prefs.amoled.collectAsState()
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    LaunchedEffect(dark) {
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        (context as? ComponentActivity)?.enableEdgeToEdge(style, style)
    }
    val scheme = when {
        !dark -> Light
        amoled -> Black
        else -> Dark
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
