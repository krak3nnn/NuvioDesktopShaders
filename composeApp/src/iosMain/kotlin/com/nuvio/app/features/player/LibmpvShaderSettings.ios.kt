package com.nuvio.app.features.player

// libmpv shaders are not available on iOS.
internal actual fun loadLibmpvShaderSetting(key: String): String? = null

internal actual fun saveLibmpvShaderSetting(key: String, value: String?) = Unit
