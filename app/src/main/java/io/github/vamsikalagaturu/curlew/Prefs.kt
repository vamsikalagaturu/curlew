package io.github.vamsikalagaturu.curlew

import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { System, Light, Dark }

enum class UnknownCallers { Allow, Silence, Block }

class Prefs private constructor(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val systemTones = Settings.System.getInt(context.contentResolver, Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1

    private val _theme = MutableStateFlow(runCatching { ThemeMode.valueOf(sp.getString(THEME, null)!!) }.getOrDefault(ThemeMode.System))
    val theme: StateFlow<ThemeMode> = _theme

    private val _amoled = MutableStateFlow(sp.getBoolean(AMOLED, false))
    val amoled: StateFlow<Boolean> = _amoled

    private val _defaultTab = MutableStateFlow(sp.getString(DEFAULT_TAB, "Keypad")!!)
    val defaultTab: StateFlow<String> = _defaultTab

    private val _haptics = MutableStateFlow(sp.getBoolean(HAPTICS, true))
    val haptics: StateFlow<Boolean> = _haptics

    private val _tones = MutableStateFlow(sp.getBoolean(TONES, systemTones))
    val tones: StateFlow<Boolean> = _tones

    private val _autoUpdate = MutableStateFlow(sp.getBoolean(AUTO_UPDATE, true))
    val autoUpdate: StateFlow<Boolean> = _autoUpdate

    fun setAutoUpdate(value: Boolean) {
        _autoUpdate.value = value
        sp.edit { putBoolean(AUTO_UPDATE, value) }
    }

    private val _callAccount = MutableStateFlow(sp.getString(CALL_ACCOUNT, "")!!)
    val callAccount: StateFlow<String> = _callAccount

    fun setCallAccount(value: String) {
        _callAccount.value = value
        sp.edit { putString(CALL_ACCOUNT, value) }
    }

    private val _unknownCallers = MutableStateFlow(
        runCatching { UnknownCallers.valueOf(sp.getString(UNKNOWN_CALLERS, null)!!) }.getOrDefault(UnknownCallers.Allow),
    )
    val unknownCallers: StateFlow<UnknownCallers> = _unknownCallers

    fun setUnknownCallers(value: UnknownCallers) {
        _unknownCallers.value = value
        sp.edit { putString(UNKNOWN_CALLERS, value.name) }
    }

    private val _blockPrivate = MutableStateFlow(sp.getBoolean(BLOCK_PRIVATE, false))
    val blockPrivate: StateFlow<Boolean> = _blockPrivate

    fun setBlockPrivate(value: Boolean) {
        _blockPrivate.value = value
        sp.edit { putBoolean(BLOCK_PRIVATE, value) }
    }

    private val _speedDial = MutableStateFlow((2..9).mapNotNull { d -> sp.getString("$SPEED_DIAL$d", null)?.let { d to it } }.toMap())
    val speedDial: StateFlow<Map<Int, String>> = _speedDial

    fun setSpeedDial(digit: Int, number: String?) {
        _speedDial.value = if (number == null) _speedDial.value - digit else _speedDial.value + (digit to number)
        sp.edit { if (number == null) remove("$SPEED_DIAL$digit") else putString("$SPEED_DIAL$digit", number) }
    }

    fun lastUpdateCheck(): Long = sp.getLong(LAST_UPDATE_CHECK, 0)

    fun setLastUpdateCheck(value: Long) = sp.edit { putLong(LAST_UPDATE_CHECK, value) }

    fun setTheme(value: ThemeMode) {
        _theme.value = value
        sp.edit { putString(THEME, value.name) }
    }

    fun setAmoled(value: Boolean) {
        _amoled.value = value
        sp.edit { putBoolean(AMOLED, value) }
    }

    fun setDefaultTab(value: String) {
        _defaultTab.value = value
        sp.edit { putString(DEFAULT_TAB, value) }
    }

    fun setHaptics(value: Boolean) {
        _haptics.value = value
        sp.edit { putBoolean(HAPTICS, value) }
    }

    fun setTones(value: Boolean) {
        _tones.value = value
        sp.edit { putBoolean(TONES, value) }
    }

    companion object {
        private const val THEME = "theme"
        private const val AMOLED = "amoled"
        private const val DEFAULT_TAB = "default_tab"
        private const val HAPTICS = "keypad_haptics"
        private const val TONES = "keypad_tones"
        private const val AUTO_UPDATE = "auto_update"
        private const val LAST_UPDATE_CHECK = "last_update_check"
        private const val CALL_ACCOUNT = "call_account"
        private const val UNKNOWN_CALLERS = "unknown_callers"
        private const val BLOCK_PRIVATE = "block_private"
        private const val SPEED_DIAL = "speed_dial_"

        @Volatile
        private var instance: Prefs? = null

        fun get(context: Context): Prefs = instance ?: synchronized(this) {
            instance ?: Prefs(context.applicationContext).also { instance = it }
        }
    }
}
