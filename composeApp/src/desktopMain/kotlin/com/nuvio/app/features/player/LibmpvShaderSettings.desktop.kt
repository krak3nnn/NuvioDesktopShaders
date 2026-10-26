package com.nuvio.app.features.player

import com.nuvio.app.core.storage.DesktopStorage

private val shaderStore by lazy { DesktopStorage.store("nuvio_player_shaders") }

internal actual fun loadLibmpvShaderSetting(key: String): String? = shaderStore.getString(key)

internal actual fun saveLibmpvShaderSetting(key: String, value: String?) {
    shaderStore.putString(key, value)
}
