package com.nuvio.app.features.player

import android.content.Context
import android.content.SharedPreferences

private const val preferencesName = "nuvio_player_shaders"

private fun shaderPreferences(): SharedPreferences? =
    PlayerSettingsStorage.applicationContext()
        ?.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

internal actual fun loadLibmpvShaderSetting(key: String): String? = shaderPreferences()?.getString(key, null)

internal actual fun saveLibmpvShaderSetting(key: String, value: String?) {
    shaderPreferences()?.edit()?.apply {
        if (value == null) remove(key) else putString(key, value)
    }?.apply()
}
