package com.nuvio.app.features.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Fork-only: kept out of PlayerSettingsRepository/PlayerSettingsStorage so upstream syncs stay conflict-free.
object LibmpvShaderSettings {
    private const val ENABLED_KEY = "libmpv_shaders_enabled"
    private const val ACTIVE_KEY = "libmpv_shaders_active"
    private const val SELECTED_KEY = "libmpv_shaders_selected"

    // Settings master switch: shader support on/off (shows the in-player shader menu).
    private val _enabled by lazy { MutableStateFlow(loadLibmpvShaderSetting(ENABLED_KEY)?.toBooleanStrictOrNull() ?: false) }
    val enabled: StateFlow<Boolean> get() = _enabled.asStateFlow()

    // In-player toggle, remembered for the next video.
    private val _active by lazy { MutableStateFlow(loadLibmpvShaderSetting(ACTIVE_KEY)?.toBooleanStrictOrNull() ?: true) }
    val active: StateFlow<Boolean> get() = _active.asStateFlow()

    // Id of the shader picked in the player, remembered for the next video; null = platform default.
    private val _selectedId by lazy { MutableStateFlow(loadLibmpvShaderSetting(SELECTED_KEY)) }
    val selectedId: StateFlow<String?> get() = _selectedId.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        if (_enabled.value == enabled) return
        _enabled.value = enabled
        saveLibmpvShaderSetting(ENABLED_KEY, enabled.toString())
    }

    fun setActive(active: Boolean) {
        if (_active.value == active) return
        _active.value = active
        saveLibmpvShaderSetting(ACTIVE_KEY, active.toString())
    }

    fun setSelectedId(id: String?) {
        if (_selectedId.value == id) return
        _selectedId.value = id
        saveLibmpvShaderSetting(SELECTED_KEY, id)
    }
}

internal expect fun loadLibmpvShaderSetting(key: String): String?

internal expect fun saveLibmpvShaderSetting(key: String, value: String?)
