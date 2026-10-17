package com.yourname.mp3player.data

import android.content.Context
import com.yourname.mp3player.domain.DedupeMode
import com.yourname.mp3player.domain.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("sonora_settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readEnum("theme", ThemeMode.SYSTEM))
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _dedupe = MutableStateFlow(readEnum("dedupe", DedupeMode.TITLE))
    val dedupeMode: StateFlow<DedupeMode> = _dedupe.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString("theme", mode.name).apply()
        _theme.value = mode
    }

    fun setDedupeMode(mode: DedupeMode) {
        prefs.edit().putString("dedupe", mode.name).apply()
        _dedupe.value = mode
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T {
        val name = prefs.getString(key, null) ?: return default
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }
}
